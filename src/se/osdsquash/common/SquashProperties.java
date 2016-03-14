package se.osdsquash.common;

import java.io.InputStream;
import java.util.Properties;

/**
 * Common property values for the program.
 * 
 * <p>
 * The properties are loaded from a property file. Make changes in
 * the properties file if you need to re-configure the program!
 * </p>
 */
public abstract class SquashProperties {

    // Read properties from file - overriding default values in this class:
    private static final String PROPERTIES_FILE = "se/osdsquash/common/squash.properties";
    static {
        try {
            Properties properties = new Properties();
            InputStream propertiesStream = SquashProperties.class
                .getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE);

            properties.load(propertiesStream);
            propertiesStream.close();

            // TODO OLLE: Improve error handling and default values if property is null...

            CLUB_NAME = properties.getProperty("club.name");
            CLUB_ORG_NR = properties.getProperty("club.orgnr");
            INVOICE_NAME = properties.getProperty("invoice.name");
            INVOICE_STREET = properties.getProperty("invoice.street");
            INVOICE_CITY = properties.getProperty("invoice.city");
            INVOICE_PHONE = properties.getProperty("invoice.phonenr");
            INVOICE_EMAIL = properties.getProperty("invoice.email");
            INVOICE_DAYS_DUE = Integer.parseInt(properties.getProperty("invoice.days.due"));
            TRACK_PRICE_PERSON = Integer
                .parseInt(properties.getProperty("subscription.person.price"));
            TRACK_PRICE_COMPANY = Integer
                .parseInt(properties.getProperty("subscription.company.price"));
            NR_OF_MONTHS = Integer.parseInt(properties.getProperty("subscription.period.months"));
            FIRST_TRACK_HOUR = Integer.parseInt(properties.getProperty("track.first.nr"));
            LAST_TRACK_HOUR = Integer.parseInt(properties.getProperty("track.last.nr"));
            FIRST_TRACK_NR = Integer.parseInt(properties.getProperty("booking.first.hour"));
            LAST_TRACK_NR = Integer.parseInt(properties.getProperty("booking.last.hour"));

        } catch (Exception exception) {
            throw new RuntimeException(
                "Fel: Kunde ej läsa property-fil "
                    + PROPERTIES_FILE
                    + ". Felmeddelande: "
                    + exception.getMessage());
        }
    }

    /**
     * The name of the squash club
     */
    public static String CLUB_NAME = "Östersunds Squashförening";

    /**
     * The club's organization number
     */
    public static String CLUB_ORG_NR = "802416-6251";

    /**
     * The first and last name of the person to be the sender of an invoice
     */
    public static String INVOICE_NAME = "Olle Bergström";

    /**
     * The street address to the person to be the sender of an invoice
     */
    public static String INVOICE_STREET = "Hinderstigen 3";

    /**
     * The postal code and city for the person to be the sender of an invoice
     */
    public static String INVOICE_CITY = "831 32 Östersund";

    /**
     * The phone nr to the person to be the sender of an invoice
     */
    public static String INVOICE_PHONE = "0730-798339";

    /**
     * The e-mail to the person to be the sender of an invoice
     */
    public static String INVOICE_EMAIL = "ollesiphone@gmail.com";

    /**
     * Number of days until invoice payment due, from the day it was sent
     */
    public static int INVOICE_DAYS_DUE = 30;

    /**
     * The track subscription price in SEK for a person, 6 months
     */
    public static int TRACK_PRICE_PERSON = 1400;

    /**
     * The track subscription price in SEK for a company, 6 months
     */
    public static int TRACK_PRICE_COMPANY = 5000;

    /**
     * Period length for a subscription, in number of months
     */
    public static int NR_OF_MONTHS = 6;

    /**
     * First bookable track, hour of the day
     */
    public static int FIRST_TRACK_HOUR = 6;

    /**
     * Last bookable track, hour of the day
     */
    public static int LAST_TRACK_HOUR = 21;

    /**
     * First bookable track number
     */
    public static Integer FIRST_TRACK_NR = Integer.valueOf(1);

    /**
     * Last bookable track number
     */
    public static Integer LAST_TRACK_NR = Integer.valueOf(2);
}
