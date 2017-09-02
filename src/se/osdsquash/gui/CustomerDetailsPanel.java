package se.osdsquash.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import se.osdsquash.common.SquashUtil;
import se.osdsquash.gui.MainGUI.TextFormatLevel;
import se.osdsquash.xml.XmlRepository;
import se.osdsquash.xml.jaxb.CustomerInfoType;
import se.osdsquash.xml.jaxb.CustomerType;
import se.osdsquash.xml.jaxb.InvoiceType;
import se.osdsquash.xml.jaxb.InvoicesType;
import se.osdsquash.xml.jaxb.SubscriptionType;
import se.osdsquash.xml.jaxb.SubscriptionsType;

/**
 * A Panel holding customer details
 */
public class CustomerDetailsPanel extends JPanel {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = -5061672333810391510L;

    private XmlRepository xmlRepository;

    // The current customer shown in the panel:
    private UUID customerUUID;

    // Original CustomerNr, before an edit is made
    private String customerNr;

    // All components in the Panel:
    private JLabel kundNrLabel;
    private JTextField kundNrTextField;

    private JCheckBox foretagCheckbox;
    private JLabel emptyFillerLabel;

    private JLabel fornamnLabel;
    private JTextField fornamnTextField;
    private JLabel efternamnLabel;
    private JTextField efternamnTextField;
    private JLabel gatuAdressLabel;
    private JTextField gatuAdressTextField;
    private JLabel postNrLabel;
    private JTextField postNrTextField;
    private JLabel ortLabel;
    private JTextField ortTextField;
    private JLabel telefonLabel;
    private JTextField telefonTextField;
    private JLabel eMailLabel;
    private JTextField eMailTextField;

    // A special subscription price will override 
    // standard price in the invoice, for this customer:
    private JLabel specialPriceLabel;
    private JTextField specialPriceTextField;

    private List<JComponent> inputsWithVerifiers;

    // Tables holding the subscriptions and invoices
    private SubscriptionsTable subscriptionsTable;
    private InvoicesTable invoicesTable;
    private JLabel customerNotesLabel;
    private JTextArea customerNotesText;

    private JButton saveButton;

    // True if there are unsaved changes made to the customer
    private final DirtyMarker dirtyMarker;

    /**
     * Constructor initializing an empty and disabled customer details panel
     */
    protected CustomerDetailsPanel(
        SubscriptionsTable subscriptionsTable,
        InvoicesTable invoicesTable,
        JLabel customerNotesLabel,
        JTextArea customerNotesText) {

        super();
        this.dirtyMarker = new DirtyMarker();
        this.inputsWithVerifiers = new ArrayList<>();

        this.xmlRepository = XmlRepository.getInstance();
        this.subscriptionsTable = subscriptionsTable;
        this.invoicesTable = invoicesTable;
        this.customerNotesLabel = customerNotesLabel;
        this.customerNotesText = customerNotesText;

        this.initPanel();
    }

    private void initPanel() {

        this.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.setMinimumSize(new Dimension(580, 440));
        this.setMaximumSize(new Dimension(580, 440));

        SpringLayout springLayout = new SpringLayout();
        this.setLayout(springLayout);

        // Prepare all input components, two of them per row.
        this.kundNrLabel = new JLabel("Kundnr: ");
        this.add(this.kundNrLabel);
        this.kundNrTextField = new CustomerTextField("");
        this.add(this.kundNrTextField);

        this.foretagCheckbox = new JCheckBox("Företagskund", false);
        this.add(this.foretagCheckbox);
        this.emptyFillerLabel = new JLabel(" ");
        this.add(this.emptyFillerLabel);

        this.fornamnLabel = new JLabel("Förnamn: ");
        this.add(this.fornamnLabel);
        this.fornamnTextField = new CustomerTextField("");
        this.add(this.fornamnTextField);

        this.efternamnLabel = new JLabel("Efternamn: ");
        this.add(this.efternamnLabel);
        this.efternamnTextField = new CustomerTextField("");
        this.add(this.efternamnTextField);

        this.gatuAdressLabel = new JLabel("Gatuadress: ");
        this.add(this.gatuAdressLabel);
        this.gatuAdressTextField = new CustomerTextField("");
        this.add(this.gatuAdressTextField);

        this.postNrLabel = new JLabel("Postnr: ");
        this.add(this.postNrLabel);
        this.postNrTextField = new CustomerTextField("");
        this.add(this.postNrTextField);

        this.ortLabel = new JLabel("Ort: ");
        this.add(this.ortLabel);
        this.ortTextField = new CustomerTextField("");
        this.add(this.ortTextField);

        this.telefonLabel = new JLabel("Telefon: ");
        this.add(this.telefonLabel);
        this.telefonTextField = new CustomerTextField("");
        this.add(this.telefonTextField);

        this.eMailLabel = new JLabel("E-mail: ");
        this.add(this.eMailLabel);
        this.eMailTextField = new CustomerTextField("");
        this.add(this.eMailTextField);

        this.specialPriceLabel = new JLabel("Eget ab.pris: ");
        this.add(this.specialPriceLabel);
        this.specialPriceTextField = new CustomerTextField("");
        this.add(this.specialPriceTextField);

        // Empty "separator" row:
        this.add(new JLabel(" "));
        this.add(new JLabel(" "));

        // The validate and save button:
        this.saveButton = new JButton("Spara");
        this.saveButton.setPreferredSize(new Dimension(70, 22));
        this.add(this.saveButton);

        this.saveButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {

                // Re-evaluate all fields before save, which also error-marks the fields.
                // Don't save if we find error(s).
                boolean validationErrors = false;
                for (JComponent inputComponent : CustomerDetailsPanel.this.inputsWithVerifiers) {
                    if (!inputComponent.getInputVerifier().shouldYieldFocus(inputComponent)) {
                        validationErrors = true;
                    }
                }
                if (validationErrors) {
                    JOptionPane.showMessageDialog(
                        CustomerDetailsPanel.this,
                        "Kunden sparades inte eftersom det finns fel:"
                            + "\n"
                            + "Uppgifter saknas eller är felaktigt angivna.",
                        "Ogiltiga uppgifter",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validate subscriptions, making sure they are not already taken
                List<SubscriptionType> subscriptions = CustomerDetailsPanel.this.subscriptionsTable
                    .getSubscriptions();
                if (subscriptions != null && (!subscriptions.isEmpty())) {

                    String subscriptionError = ValidatorHelper.validateSubscriptions(
                        Integer.valueOf(CustomerDetailsPanel.this.kundNrTextField.getText()),
                        subscriptions,
                        CustomerDetailsPanel.this.xmlRepository.getAllCustomers());
                    if (subscriptionError != null) {
                        JOptionPane.showMessageDialog(
                            CustomerDetailsPanel.this,
                            "Kunden sparades inte eftersom det finns fel:"
                                + "\n"
                                + subscriptionError,
                            "Ogiltiga uppgifter",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // If no UUID, this is a new customer, otherwise it should exist
                CustomerType customer;
                boolean newCustomer;

                boolean specialPriceNotice = false;
                Integer specialPrice;
                if (CustomerDetailsPanel.this.specialPriceTextField.getText() != null
                    && CustomerDetailsPanel.this.specialPriceTextField.getText().length() > 0) {
                    specialPrice = Integer
                        .parseInt(CustomerDetailsPanel.this.specialPriceTextField.getText());
                } else {
                    specialPrice = null;
                }

                if (CustomerDetailsPanel.this.customerUUID == null) {
                    newCustomer = true;
                    customer = CustomerDetailsPanel.this.xmlRepository.getNewCustomer();
                    CustomerDetailsPanel.this.customerUUID = UUID.randomUUID();
                    customer.getCustomerInfo().setCustomerUUID(
                        CustomerDetailsPanel.this.customerUUID.toString());

                    // Give a notice about special price
                    if (specialPrice != null) {
                        specialPriceNotice = true;
                    }

                } else {
                    newCustomer = false;
                    customer = CustomerDetailsPanel.this.xmlRepository
                        .getCustomer(CustomerDetailsPanel.this.customerUUID);

                    // If no special price before, but there is now - give a notice about it
                    if (customer.getCustomerInfo().getSubscriptionPrice() != null
                        && specialPrice != null) {
                        specialPriceNotice = true;
                    }
                }

                // Read all input fields and set values to the customer object
                CustomerInfoType customerInfo = customer.getCustomerInfo();
                customerInfo.setCity(CustomerDetailsPanel.this.ortTextField.getText());
                customerInfo.setCompany(CustomerDetailsPanel.this.foretagCheckbox.isSelected());
                customerInfo.setCustomerNumber(
                    Integer.valueOf(CustomerDetailsPanel.this.kundNrTextField.getText()));
                customerInfo.setEmail(CustomerDetailsPanel.this.eMailTextField.getText());
                customerInfo.setFirstname(CustomerDetailsPanel.this.fornamnTextField.getText());
                customerInfo.setLastname(CustomerDetailsPanel.this.efternamnTextField.getText());
                customerInfo.setPostalCode(CustomerDetailsPanel.this.postNrTextField.getText());
                customerInfo.setStreet(CustomerDetailsPanel.this.gatuAdressTextField.getText());
                customerInfo.setSubscriptionPrice(specialPrice);
                customerInfo.setTelephone(CustomerDetailsPanel.this.telefonTextField.getText());

                customerInfo.setNotes(CustomerDetailsPanel.this.customerNotesText.getText());

                // Simply take what's in the subscriptions table and set that
                List<SubscriptionType> subscriptionsFromTable = CustomerDetailsPanel.this.subscriptionsTable
                    .getSubscriptions();
                CustomerDetailsPanel.this.xmlRepository
                    .setSubscriptionsToCustomer(customer, subscriptionsFromTable);

                // Simply take what's in the invoices table and set that
                List<InvoiceType> invoicesFromTable = CustomerDetailsPanel.this.invoicesTable
                    .getInvoices();
                CustomerDetailsPanel.this.xmlRepository
                    .setInvoicesToCustomer(customer, invoicesFromTable);

                // Save the customer object
                CustomerDetailsPanel.this.xmlRepository.saveCustomer(customer);
                String saveMessage;

                // We must add the new customer to the customer list...
                if (newCustomer) {
                    MainGUI.getInstance().addCustomerToList(customer);
                    saveMessage = "Ny kund sparad";
                } else {
                    // ...or just repaint the list
                    MainGUI.getInstance().repaintCustomerList();
                    saveMessage = "Kunduppgifter sparade";
                }

                // Update email buttons enable/disable for the whole program
                MainGUI.getInstance().toggleEmailFunction();

                MainGUI.getInstance().printInfoText(saveMessage, TextFormatLevel.Info, true);

                if (specialPriceNotice) {
                    MainGUI.getInstance().printInfoText(
                        "NOTERA att eget ab.pris är satt!",
                        TextFormatLevel.Notice,
                        true);
                }

                CustomerDetailsPanel.this.dirtyMarker.setClean();
            }
        });

        this.add(new JLabel(" "));

        // By default, all fields are empty and everything is disabled
        this.toggleFields(true, false);

        // NOTE: The above components much be even with the nr of rows and columns here:
        SpringLayoutUtil.makeGrid(
            this,
            12, // Rows
            2, // Cols
            10, // X location
            10, // Y location
            4, // X padding
            2); // Y padding

        this.add(new JLabel(" "));

        // Add dirty listeners for all input components, so we know when something has been modified
        this.registerDirtyListeners();

        // Start with customer not dirty
        this.dirtyMarker.setClean();
    }

    private void registerDirtyListeners() {

        // Add data change listeners for all our different input components
        for (Component component : super.getComponents()) {
            if (component instanceof JTextField) {
                ((JTextField) component)
                    .getDocument()
                    .addDocumentListener(CustomerDetailsPanel.this.dirtyMarker);
            } else if (component instanceof JCheckBox) {
                ((JCheckBox) component).addChangeListener(CustomerDetailsPanel.this.dirtyMarker);
            } else if (component instanceof JTextArea) {
                ((JTextArea) component)
                    .getDocument()
                    .addDocumentListener(CustomerDetailsPanel.this.dirtyMarker);
            }
        }

        // Common notes text area
        this.customerNotesText
            .getDocument()
            .addDocumentListener(CustomerDetailsPanel.this.dirtyMarker);

        // The subscriptions table
        this.subscriptionsTable.getSubscriptionsTableModel().addTableModelListener(
            CustomerDetailsPanel.this.dirtyMarker);

        // The invoices table
        this.invoicesTable
            .getInvoicesTableModel()
            .addTableModelListener(CustomerDetailsPanel.this.dirtyMarker);
    }

    /**
     * Clears customer data from the panel, if any
     */
    protected void clearCustomer() {

        this.customerUUID = null;
        this.customerNr = null;

        // Clear all inputs and disable them
        this.toggleFields(true, false);

        // Make customer not dirty after everything is set
        this.dirtyMarker.setClean();
    }

    /**
     * Sets given customer data to the panel
     * @param customerType An existing customer
     */
    protected void setCustomer(CustomerType customerType) {

        CustomerInfoType customerInfo = customerType.getCustomerInfo();
        this.customerUUID = UUID.fromString(customerInfo.getCustomerUUID());
        this.customerNr = String.valueOf(customerInfo.getCustomerNumber());

        // Enable all inputs
        this.toggleFields(false, true);

        // Set all input values
        this.kundNrTextField.setText(String.valueOf(customerInfo.getCustomerNumber()));

        this.foretagCheckbox.setSelected(customerInfo.isCompany());

        this.fornamnTextField.setText(customerInfo.getFirstname());
        this.efternamnTextField.setText(customerInfo.getLastname());
        this.gatuAdressTextField.setText(customerInfo.getStreet());
        this.postNrTextField.setText(customerInfo.getPostalCode());
        this.ortTextField.setText(customerInfo.getCity());
        this.telefonTextField.setText(customerInfo.getTelephone());
        this.eMailTextField.setText(customerInfo.getEmail());

        if (customerInfo.getSubscriptionPrice() != null) {
            this.specialPriceTextField.setText(String.valueOf(customerInfo.getSubscriptionPrice()));
        }

        this.customerNotesText.setText(customerInfo.getNotes());
        this.kundNrTextField.requestFocus();

        // Set all subscriptions
        this.subscriptionsTable.clearSubscriptions();

        SubscriptionsType subscriptionsType = customerType.getSubscriptions();
        if (subscriptionsType != null) {
            for (SubscriptionType subscriptionType : subscriptionsType.getSubscription()) {
                this.subscriptionsTable.addSubscription(subscriptionType);
            }
        }

        // Set all invoices
        InvoicesType invoicesType = customerType.getInvoices();
        if (invoicesType != null) {
            this.invoicesTable.setInvoices(customerInfo, invoicesType.getInvoice());
        }

        // Sets all input field validators...
        this.initValidators();

        // ...and re-evaluate them to get the correct status
        for (JComponent component : this.inputsWithVerifiers) {
            component.getInputVerifier().shouldYieldFocus(component);
        }

        // Repaint now that things are populated and done:
        this.subscriptionsTable.repaint();
        this.invoicesTable.repaint();

        // Make customer not dirty after everything is set
        this.dirtyMarker.setClean();
    }

    /**
     * Clears the panel and prepares for a new customer to be entered
     * @param customerNr A new, unique customer number to use as default nr
     */
    protected void prepareNewCustomer(int customerNr) {

        // Clear all inputs and enable them
        this.toggleFields(true, true);

        this.customerUUID = null;
        this.customerNr = String.valueOf(customerNr);
        this.kundNrTextField.setText(String.valueOf(customerNr));

        // Sets all input field validators...
        this.initValidators();

        // ...and clear old validation error markers.
        // We must give the user a chance to enter everything before we error-mark.
        for (JComponent component : this.inputsWithVerifiers) {
            component.setBackground(Color.WHITE);
        }

        this.kundNrTextField.requestFocus();

        // Make customer not dirty after everything is set
        this.dirtyMarker.setClean();
    }

    /**
     * Returns true if there is a customer set and it's details has
     * been modified and NOT saved
     * 
     * @return True if there are unsaved changes to the customer
     */
    protected boolean isCustomerDirty() {
        return this.dirtyMarker.isDirty();
    }

    /**
     * Clear dirty marker and sets it as NON dirty
     */
    protected void clearCustomerDirty() {
        this.dirtyMarker.setClean();
    }

    // Optional clearing of all input fields and enable or disable them
    private void toggleFields(boolean clearValues, boolean enableFields) {

        for (Component component : super.getComponents()) {
            if (component instanceof JLabel) {
                component.setEnabled(enableFields);
            } else if (component instanceof JButton) {
                component.setEnabled(enableFields);
            } else if (component instanceof JTextField) {
                if (clearValues) {
                    ((JTextField) component).setText("");
                }
                ((JTextField) component).setEnabled(enableFields);
            } else if (component instanceof JCheckBox) {
                if (clearValues) {
                    ((JCheckBox) component).setSelected(false);
                }
                ((JCheckBox) component).setEnabled(enableFields);
            } else if (component instanceof JTextArea) {
                if (clearValues) {
                    ((JTextArea) component).setText("");
                }
                ((JTextArea) component).setEnabled(enableFields);
            }
        }

        // These components are not "added" to this panel, we have references to them:
        if (clearValues) {
            this.subscriptionsTable.clearSubscriptions();
            this.invoicesTable.clearInvoices();
            this.customerNotesText.setText("");
        }
        this.subscriptionsTable.setEnabled(enableFields);
        this.invoicesTable.setEnabled(enableFields);
        this.customerNotesLabel.setEnabled(enableFields);
        this.customerNotesText.setEnabled(enableFields);

        // Repaint the tables to make sure they are updated
        this.subscriptionsTable.repaint();
        this.invoicesTable.repaint();
    }

    // Resets/creates all input field validators
    private void initValidators() {

        // Validation errors are written to the main panel
        MainGUI mainGui = MainGUI.getInstance();

        // Collect the fields having verifiers
        this.inputsWithVerifiers.clear();

        InputVerifier kundNrVerifier = new ValidatorHelper.CustomerNrJTextFieldVerifier(
            this.customerNr,
            mainGui.getValidationErrorTextLabel());
        this.kundNrTextField.setInputVerifier(kundNrVerifier);
        this.inputsWithVerifiers.add(this.kundNrTextField);

        InputVerifier fornamnVerifier = new ValidatorHelper.ValueMandatoryJTextFieldVerifier(
            "Obligatoriska uppgifter",
            mainGui.getValidationErrorTextLabel());
        this.fornamnTextField.setInputVerifier(fornamnVerifier);
        this.inputsWithVerifiers.add(this.fornamnTextField);

        InputVerifier specialPriceVerifier = new ValidatorHelper.OptionalIntegerJTextFieldVerifier(
            "Eget ab.pris",
            mainGui.getValidationErrorTextLabel());
        this.specialPriceTextField.setInputVerifier(specialPriceVerifier);
        this.inputsWithVerifiers.add(this.specialPriceTextField);

        // --- Don't require post adress: ---

        /*InputVerifier streetVerifier = new ValidatorHelper.ValueMandatoryJTextFieldVerifier(
            "Obligatoriska uppgifter",
            mainGui.getValidationErrorTextLabel());
        this.gatuAdressTextField.setInputVerifier(streetVerifier);
        this.inputsWithVerifiers.add(this.gatuAdressTextField);
        
        InputVerifier postNrVerifier = new ValidatorHelper.ValueMandatoryJTextFieldVerifier(
            "Obligatoriska uppgifter",
            mainGui.getValidationErrorTextLabel());
        this.postNrTextField.setInputVerifier(postNrVerifier);
        this.inputsWithVerifiers.add(this.postNrTextField);
        
        InputVerifier cityVerifier = new ValidatorHelper.ValueMandatoryJTextFieldVerifier(
            "Obligatoriska uppgifter",
            mainGui.getValidationErrorTextLabel());
        this.ortTextField.setInputVerifier(cityVerifier);
        this.inputsWithVerifiers.add(this.ortTextField);*/
    }

    // Enables/disables mailing functions, if customer have an e-mail or not.
    // Returns true if e-mail is enabled, otherwise false.
    protected boolean toggleEmailFunction() {
        this.invoicesTable.refreshInvoiceTable();
        return SquashUtil.isSet(this.eMailTextField.getText());
    }

    // Default sizes text field
    private static final class CustomerTextField extends JTextField {

        /**
         * Serial UID
         */
        private static final long serialVersionUID = -4963309556324245707L;

        private static final int defaultNrOfColumns = 20;

        protected CustomerTextField(String textValue) {
            super(textValue, defaultNrOfColumns);
        }
    }
}
