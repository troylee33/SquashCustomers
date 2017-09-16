package se.osdsquash.xml;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import se.osdsquash.common.SquashProperties;
import se.osdsquash.common.SquashRuntimeInfo;
import se.osdsquash.common.SquashUtil;
import se.osdsquash.excel.ExcelHandler;
import se.osdsquash.logger.SquashLogger;
import se.osdsquash.xml.jaxb.CustomerInfoType;
import se.osdsquash.xml.jaxb.CustomerType;
import se.osdsquash.xml.jaxb.CustomersType;
import se.osdsquash.xml.jaxb.InvoiceType;
import se.osdsquash.xml.jaxb.InvoicesType;
import se.osdsquash.xml.jaxb.ObjectFactory;
import se.osdsquash.xml.jaxb.SubscriptionType;
import se.osdsquash.xml.jaxb.SubscriptionsType;

/**
 * Handles reading/writing of the XML repository, singleton class.
 * 
 * <p>
 * All writes are thread safe!
 * </p>
 */
public class XmlRepository {

    private static final SquashLogger logger = SquashLogger.getInstance();

    // We start at 3000, to avoid duplicates with the club's historical data
    private static final int DEFAULT_START_NR = 3000;

    private static final String XSD_SCHEMA_PATH = "se/osdsquash/xml/Customers.xsd";

    private static final String DATA_DIR_PATH;
    private static final String BACKUPS_DIR_PATH;
    private static final String XML_STORAGE_FILE_PATH;

    /**
     * Path to the invoices directory
     */
    public static final String INVOICES_DIR_PATH;

    private static final String FILE_DATE_FORMAT = "yyyyMMdd";

    // This is the "in memory" XML data object:
    private JAXBElement<CustomersType> customersJaxbXml;

    // Reference to the actual XML file. Null if it doesn't exist, e.g. no customers:
    private File xmlFile;

    // Load static JAXB and Schema instances:
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static final JAXBContext JAXB_CONTEXT;
    private static final Schema SCHEMA;
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(CustomersType.class);
            SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            SCHEMA = schemaFactory
                .newSchema(XmlRepository.class.getClassLoader().getResource(XSD_SCHEMA_PATH));

            DATA_DIR_PATH = SquashRuntimeInfo.getDataDirPath();
            INVOICES_DIR_PATH = DATA_DIR_PATH + "/invoices";
            BACKUPS_DIR_PATH = DATA_DIR_PATH + "/backups";
            XML_STORAGE_FILE_PATH = DATA_DIR_PATH + "/CustomerDatabase.xml";

        } catch (SAXException | JAXBException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static final XmlRepository INSTANCE = new XmlRepository();

    /**
     * Returns the one and only thread-safe instance of the repository
     * @return The instance
     */
    public static XmlRepository getInstance() {
        return INSTANCE;
    }

    // Private constructor...
    private XmlRepository() {
        this.init();
    }

    private void init() {

        logger.log("Initializing the customer database...", false);

        // Always make sure we have all data folders:
        File dataDir = new File(DATA_DIR_PATH);
        if (!dataDir.exists()) {
            if (!dataDir.mkdir()) {
                throw new RuntimeException(
                    "FEL när data-katalogen skulle skapas, kontrollera att det gör att skriva till lagringsytan!");
            }
        }

        File invoicesDir = new File(INVOICES_DIR_PATH);
        if (!invoicesDir.exists()) {
            if (!invoicesDir.mkdir()) {
                throw new RuntimeException(
                    "FEL när invoices-katalogen skulle skapas, kontrollera att det gör att skriva till lagringsytan!");
            }
        }

        File backupsDir = new File(BACKUPS_DIR_PATH);
        if (!backupsDir.exists()) {
            if (!backupsDir.mkdir()) {
                throw new RuntimeException(
                    "FEL när backups-katalogen skulle skapas, kontrollera att det går att skriva till lagringsytan!");
            }
        }

        // Check for very old backup files and delete them
        final long thresholdMillis = this.getBackupThresholdMillis();
        File[] tooOldFiles = backupsDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.lastModified() < thresholdMillis;
            }
        });

        if (tooOldFiles != null) {
            for (File file : tooOldFiles) {
                try {
                    if (!file.delete()) {
                        logger.log(
                            "Notis: Kunde ej radera gammal backup-fil: " + file.getPath(),
                            true);
                    }
                } catch (Exception ex) {
                    logger.log("Notis: Kunde ej radera gammal backup-fil: " + file.getPath(), true);
                }

            }
        }

        // Load and parse the XML file - if it exists
        FileInputStream xmlFileStream = null;
        try {

            this.xmlFile = new File(XML_STORAGE_FILE_PATH);
            if (!this.xmlFile.isFile()) {
                this.xmlFile = null;
                logger.log(
                    "XML-databasens fil existerade inte: "
                        + XML_STORAGE_FILE_PATH
                        + ". Inga kunder finns - startar med ny databas.",
                    false);
            }

            // Parse the XML if it exists
            if (this.xmlFile != null) {

                // Create a backup of the XML file before we start.
                // We re-use one file per day, or we could get too many.
                try {
                    String backupFilePath = BACKUPS_DIR_PATH
                        + "/CustomerDbBackup_"
                        + new SimpleDateFormat(FILE_DATE_FORMAT).format(new Date())
                        + ".xml";
                    Files.copy(
                        this.xmlFile.toPath(),
                        new File(backupFilePath).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception exception) {
                    logger.log(
                        "Varning: Fel uppstod vid skapande a backup-fil för XML-databasen. Felmeddelande: "
                            + exception.getMessage(),
                        true);
                }

                xmlFileStream = new FileInputStream(XML_STORAGE_FILE_PATH);
                Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();

                // This enables schema validation
                unmarshaller.setSchema(SCHEMA);

                this.customersJaxbXml = unmarshaller
                    .unmarshal(new StreamSource(xmlFileStream), CustomersType.class);

            } else {
                // If no existing customers, make sure there is at least the
                // XML root element to avoid null-problems. Also init the nr series.
                if (this.customersJaxbXml == null) {
                    CustomersType customersRootElement = OBJECT_FACTORY.createCustomersType();
                    customersRootElement.setCurrentCustomerNr(DEFAULT_START_NR);
                    customersRootElement.setCurrentInvoiceNr(DEFAULT_START_NR);
                    this.customersJaxbXml = OBJECT_FACTORY.createCustomers(customersRootElement);
                }
            }

        } catch (Exception exception) {
            throw new RuntimeException(
                "FEL när XML-databasen skulle laddas. Felmeddelande: " + exception.getMessage(),
                exception);
        } finally {
            this.closeResource(xmlFileStream);
        }

        logger.log("Customer database initialize finished", false);
    }

    /**
     * Returns all the Customers (in a copied list)
     * @return The customers, empty list if none
     */
    public List<CustomerType> getAllCustomers() {

        List<CustomerType> customerItems = new ArrayList<>();
        for (CustomerType customerType : this.customersJaxbXml.getValue().getCustomer()) {
            customerItems.add(customerType);
        }
        return customerItems;
    }

    /**
     * Deletes a customer and persists the change
     * 
     * @param customerUUID Customer UUID
     * @throws IllegalArgumentException If customer not found
     */
    public synchronized void deleteCustomer(UUID customerUUID) {

        // Look for the customer
        Iterator<CustomerType> customersIterator = this.customersJaxbXml
            .getValue()
            .getCustomer()
            .iterator();

        while (customersIterator.hasNext()) {
            CustomerType customer = customersIterator.next();
            if (customer.getCustomerInfo().getCustomerUUID().equals(customerUUID.toString())) {

                // This deletes from underlying XML list
                customersIterator.remove();
                this.saveRepository();
                return;
            }
        }

        throw new IllegalArgumentException("Hittade inte kund med ID " + customerUUID.toString());
    }

    /**
     * Stores a given customer and persists the change
     * @param customer The customer to store, can be new or existing
     */
    public synchronized void saveCustomer(CustomerType customer) {

        // Add to customer list if new customer
        if (this
            .getCustomer(UUID.fromString(customer.getCustomerInfo().getCustomerUUID())) == null) {
            this.customersJaxbXml.getValue().getCustomer().add(customer);
        }

        // Save everything
        this.saveRepository();
    }

    /**
     * Returns the customer object given a customer UUID, null if it doesn't exist
     * @param customerUUID Customer UUID
     * @return The customer, if null if not present
     */
    public CustomerType getCustomer(UUID customerUUID) {

        // Look for the customer
        for (CustomerType customer : this.getAllCustomers()) {
            if (customer.getCustomerInfo().getCustomerUUID().equals(customerUUID.toString())) {
                return customer;
            }
        }

        return null;
    }

    /**
     * Returns the next avaliable customer nr.
     * The number is "consumed" when/if the repository is saved.
     * 
     * @return A new, unique customer nr
     */
    public synchronized int getNewCustomerNr() {

        // Increment, set and return a new nr.
        // This is never null, since we always initialize the repository.
        CustomersType customersType = this.customersJaxbXml.getValue();
        Integer customerNr = customersType.getCurrentCustomerNr();
        customerNr++;

        // This new customer nr will be saved whenever the repository is saved
        customersType.setCurrentCustomerNr(customerNr);

        return customerNr;
    }

    /**
     * Returns a new customer (and customer info) object.
     * NOTE: This customer object is not saved, just a new object in memory!
     * 
     * @return A new, unsaved customer
     */
    public CustomerType getNewCustomer() {
        CustomerType customerType = OBJECT_FACTORY.createCustomerType();
        customerType.setCustomerInfo(OBJECT_FACTORY.createCustomerInfoType());
        return customerType;
    }

    /**
     * Returns a new subscription object.
     * NOTE: This object is not saved, just a new object in memory!
     * 
     * @return A new, unsaved subscription
     */
    public SubscriptionType getNewSubscription() {
        return OBJECT_FACTORY.createSubscriptionType();
    }

    /**
     * Returns a new invoice object.
     * NOTE: This object is not saved, just a new object in memory!
     * 
     * @return A new, unsaved invoice
     */
    public InvoiceType getNewInvoice() {
        return OBJECT_FACTORY.createInvoiceType();
    }

    /**
     * Sets given subscriptions to the customer, replacing existing ones.
     * NOTE: The customer object is not persisted, just saved in memory!
     * 
     * @param customer Customer to set subscriptions for
     * @param subscriptions The subscriptions to set
     */
    public synchronized void setSubscriptionsToCustomer(
        CustomerType customer,
        List<SubscriptionType> subscriptions) {

        if (customer.getSubscriptions() == null) {
            customer.setSubscriptions(OBJECT_FACTORY.createSubscriptionsType());
        }
        SubscriptionsType subscriptionsType = customer.getSubscriptions();
        subscriptionsType.getSubscription().clear();
        subscriptionsType.getSubscription().addAll(subscriptions);
    }

    /**
     * Sets given invoices to the customer, replacing existing ones.
     * NOTE: The customer object is not persisted, just saved in memory!
     * 
     * @param customer Customer to set invoices for
     * @param invoices The invoices to set
     */
    public synchronized void setInvoicesToCustomer(
        CustomerType customer,
        List<InvoiceType> invoices) {

        if (customer.getInvoices() == null) {
            customer.setInvoices(OBJECT_FACTORY.createInvoicesType());
        }
        InvoicesType invoicesType = customer.getInvoices();
        invoicesType.getInvoice().clear();
        invoicesType.getInvoice().addAll(invoices);
    }

    /**
     * Deletes an invoice from a customer and persists the change.
     * If the actual invoice file is found, that is also deleted.
     * 
     * @param invoiceNr The number of the invoice to delete
     * @throws IllegalArgumentException if invoice is not found
     */
    public synchronized void deleteInvoice(int invoiceNr) {

        // This is never null, since we always initialize the repository.
        CustomersType customersType = this.customersJaxbXml.getValue();

        // Loop customer and their invoices
        for (CustomerType customer : customersType.getCustomer()) {
            if (customer.getInvoices() != null) {
                Iterator<InvoiceType> invoiceIterator = customer
                    .getInvoices()
                    .getInvoice()
                    .iterator();
                while (invoiceIterator.hasNext()) {
                    InvoiceType invoice = invoiceIterator.next();
                    if (invoiceNr == invoice.getInvoiceNumber()) {

                        // This deletes from underlying XML list
                        invoiceIterator.remove();
                        this.saveRepository();

                        // Try to delete file
                        File invoiceFile = new File(invoice.getRelativeFilePath());
                        if (invoiceFile.isFile()) {
                            try {
                                invoiceFile.delete();
                            } catch (Exception ex) {
                                // Just log file deletion failure...
                                logger.log(
                                    "Varning: Fel uppstod vid radering av fakturafil: "
                                        + invoiceFile.getAbsolutePath()
                                        + ". Felmeddelande: "
                                        + ex.getMessage(),
                                    true);
                            }
                        }

                        return;
                    }
                }
            }
        }

        throw new IllegalArgumentException("Hittade inte faktura med nr " + invoiceNr);
    }

    /**
     * Adds given invoice to the customer.
     * NOTE: The customer object is not persisted, just saved in memory!
     * 
     * @param customer Customer to add an invoice for
     * @param invoice The invoice to add
     */
    public synchronized void addInvoiceToCustomer(CustomerType customer, InvoiceType invoice) {

        if (customer.getInvoices() == null) {
            customer.setInvoices(OBJECT_FACTORY.createInvoicesType());
        }
        InvoicesType invoicesType = customer.getInvoices();
        invoicesType.getInvoice().add(invoice);
    }

    /**
     * Returns the next avaliable invoice nr and saves it in the repository.
     * The number is "consumed" as soon as this method has been called.
     * 
     * @return A new, unique invoice nr
     */
    public synchronized int getNewInvoiceNr() {

        // Increment, set and return a new nr.
        // This is never null, since we always initialize the repository.
        CustomersType customersType = this.customersJaxbXml.getValue();
        Integer invoiceNr = customersType.getCurrentInvoiceNr();
        invoiceNr++;

        // There could be a scenario where a new invoice is created with this nr, but the repository
        // is never saved with the new nr. That would lead to duplicate numbers, so we save right away.
        customersType.setCurrentInvoiceNr(invoiceNr);
        this.saveRepository();

        return invoiceNr;
    }

    /**
     * Returns the current/last used customer nr.
     * 
     * @return The latest customer nr used
     */
    public synchronized int getCurrentCustomerNr() {

        // This is never null, since we always initialize the repository.
        CustomersType customersType = this.customersJaxbXml.getValue();
        return customersType.getCurrentCustomerNr().intValue();
    }

    /**
     * Finds a customer by given invoice nr, if any
     * 
     * @param invoiceNr Invoice number
     * @return The customer belonging to given invoice, null if not found
     */
    public CustomerType getCustomerByInvoiceNr(int invoiceNr) {

        // This is never null, since we always initialize the repository.
        CustomersType customersType = this.customersJaxbXml.getValue();

        for (CustomerType customer : customersType.getCustomer()) {
            if (customer.getInvoices() != null) {
                for (InvoiceType invoice : customer.getInvoices().getInvoice()) {
                    if (invoiceNr == invoice.getInvoiceNumber()) {
                        return customer;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Generates a single invoice file for a customer for a given period
     * and saves it at the same time. The result is always one single invoice.
     * 
     * @param nextPeriod True if to use next subscription period, or false for the current one
     * @param customerUUID Customer's ID
     * @return The invoice creation result
     */
    public synchronized InvoiceResults generateAndStoreSingleInvoice(
        boolean nextPeriod,
        UUID customerUUID) {

        List<String> invoiceFilenameSingleton = new ArrayList<>(1);
        List<String> customersWithoutSubscriptionSingleton = new ArrayList<>(1);

        ExcelHandler excelHandler = new ExcelHandler(this);
        CustomerType customer = this.getCustomer(customerUUID);
        InvoiceType invoice = excelHandler
            .createInvoiceFile(customer, SquashProperties.INVOICE_DAYS_DUE, nextPeriod);
        invoiceFilenameSingleton.add(invoice.getRelativeFilePath());

        // Period is null if there is no subscription
        if (invoice.getPeriodStartDate() == null) {
            customersWithoutSubscriptionSingleton.add("Kunden har inga abonnemang");
        }

        this.saveRepository();

        return new InvoiceResults(invoiceFilenameSingleton, customersWithoutSubscriptionSingleton);
    }

    /**
     * Generates invoice files for all customers for a given period
     * and saves everything at the same time.
     * 
     * @param nextPeriod True if to use next subscription period, or false for the current one
     * @return The invoice creation result
     */
    public synchronized InvoiceResults generateAndStoreInvoices(boolean nextPeriod) {

        List<String> invoiceFilenames = new ArrayList<>();
        List<String> customersWithoutSubscriptions = new ArrayList<>();

        ExcelHandler excelHandler = new ExcelHandler(this);
        for (CustomerType customer : this.getAllCustomers()) {
            InvoiceType invoice = excelHandler
                .createInvoiceFile(customer, SquashProperties.INVOICE_DAYS_DUE, nextPeriod);
            invoiceFilenames.add(invoice.getRelativeFilePath());

            // Period is null if there is no subscription
            if (invoice.getPeriodStartDate() == null) {

                CustomerInfoType customerInfo = customer.getCustomerInfo();
                String customerLabel = String.valueOf(customerInfo.getCustomerNumber())
                    + " ("
                    + (!SquashUtil.isSet(customerInfo.getFirstname())
                        ? ""
                        : customerInfo.getFirstname())
                    + (!SquashUtil.isSet(customerInfo.getLastname())
                        ? ""
                        : " " + customerInfo.getLastname())
                    + ")";
                customersWithoutSubscriptions.add(customerLabel);
            }
        }

        this.saveRepository();

        return new InvoiceResults(invoiceFilenames, customersWithoutSubscriptions);
    }

    /**
     * Saves the current state of the whole repository to XML file
     */
    public synchronized void saveRepository() {

        if (this.customersJaxbXml.getValue().getCustomer().isEmpty()) {
            // If no customers, but there is an existing file - simply delete it.
            if (this.xmlFile != null) {
                if (this.xmlFile.exists()) {
                    this.xmlFile.delete();
                }
            }
        } else {

            // Marshall the XML object to file
            FileOutputStream fileOutput = null;
            try {
                Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8");

                // This enables schema validation
                marshaller.setSchema(SCHEMA);

                fileOutput = new FileOutputStream(XML_STORAGE_FILE_PATH, false);
                marshaller.marshal(this.customersJaxbXml, fileOutput);

                // Indicate that there is a file now
                if (this.xmlFile == null) {
                    this.xmlFile = new File(XML_STORAGE_FILE_PATH);
                }

            } catch (Exception exception) {
                throw new RuntimeException(
                    "Fel då XML-databasen skulle sparas. Felmeddelande: " + exception.getMessage(),
                    exception);
            } finally {
                this.closeResource(fileOutput);
            }
        }
    }

    private void closeResource(Closeable closable) {

        if (closable != null) {
            try {
                closable.close();
            } catch (Exception exception) {
                // Ignore this...
            }
        }
    }

    // How old backup files we keep
    private long getBackupThresholdMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, -1);
        return cal.getTimeInMillis();
    }
}
