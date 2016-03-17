package se.osdsquash.test;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import se.osdsquash.excel.ExcelHandler;
import se.osdsquash.xml.XmlRepository;
import se.osdsquash.xml.jaxb.CustomerInfoType;
import se.osdsquash.xml.jaxb.CustomerType;
import se.osdsquash.xml.jaxb.ObjectFactory;
import se.osdsquash.xml.jaxb.SubscriptionType;
import se.osdsquash.xml.jaxb.SubscriptionsType;
import se.osdsquash.xml.jaxb.WeekdayType;

/**
 * Test to create Excel invoice files
 */
public class ExcelCreatorTest {

    // A simple test method
    public static void main(String[] args) {

        // Simulate a customer
        ObjectFactory xmlObjectFactory = new ObjectFactory();
        CustomerType customerType = xmlObjectFactory.createCustomerType();

        CustomerInfoType customerInfoType = xmlObjectFactory.createCustomerInfoType();
        customerInfoType.setCustomerNumber(369369);
        customerInfoType.setCity("Kundstaden");
        customerInfoType.setCompany(false);
        customerInfoType.setCustomerUUID(UUID.randomUUID().toString());
        customerInfoType.setEmail("kundens.epostadress@ganskalangtext.se");
        customerInfoType.setFirstname("Johannes");
        customerInfoType.setLastname("Kundefternamnsson");
        customerInfoType.setPostalCode("932 54");
        customerInfoType.setStreet("Kundgatan 335, 3 trappor");
        customerInfoType.setTelephone("070-1234 3433");

        customerType.setCustomerInfo(customerInfoType);

        // Add a subscription
        SubscriptionType subscriptionType = xmlObjectFactory.createSubscriptionType();
        try {
            subscriptionType.setStartTime(
                DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (DatatypeConfigurationException exception) {
            throw new RuntimeException(exception);
        }
        subscriptionType.setTrackNumber(1);
        subscriptionType.setWeekday(WeekdayType.TUESDAY);

        SubscriptionsType subscriptionsType = xmlObjectFactory.createSubscriptionsType();
        subscriptionsType.getSubscription().add(subscriptionType);
        customerType.setSubscriptions(subscriptionsType);

        String filename = new ExcelHandler(XmlRepository.getInstance())
            .createInvoiceFile(customerType, 30)
            .getRelativeFilePath();
        System.out.println("Excel file created : " + filename);

        // Open the file
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.open(new File(filename));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
