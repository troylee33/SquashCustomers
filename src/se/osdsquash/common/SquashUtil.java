package se.osdsquash.common;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import se.osdsquash.xml.jaxb.WeekdayType;

/**
 * Common utility functions
 */
public abstract class SquashUtil {

    // The time format (HH:mm) for track start times, e.g. when a track is booked.
    private static final String START_TIME_DATE_FORMAT = "HH:mm";

    /**
     * Converts an XML weekday type to a Swedish string
     * @param weekdayType XML type
     * @return A presentation string for the weekday
     */
    public static String weekdayTypeToString(WeekdayType weekdayType) {

        if (weekdayType == null) {
            // Should not happen...
            throw new RuntimeException("weekdayType is null");
        }

        switch (weekdayType) {
            case MONDAY : {
                return "Måndag";
            }
            case TUESDAY : {
                return "Tisdag";
            }
            case WEDNESDAY : {
                return "Onsdag";
            }
            case THURSDAY : {
                return "Torsdag";
            }
            case FRIDAY : {
                return "Fredag";
            }
            case SATURDAY : {
                return "Lördag";
            }
            case SUNDAY : {
                return "Söndag";
            }
            default :
                // Ignore...
        }

        // Should not happen...
        throw new RuntimeException("Unknown WeekdayType value: " + weekdayType.toString());
    }

    /**
     * Converts a Swedish weekday presentation string to XML weekday type
     * @param weekdayString String type
     * @return A presentation string for the weekday
     */
    public static WeekdayType weekdayStringToType(String weekdayString) {

        if (weekdayString == null) {
            // Should not happen...
            throw new RuntimeException("weekdayString is null");
        }

        switch (weekdayString) {
            case "Måndag" : {
                return WeekdayType.MONDAY;
            }
            case "Tisdag" : {
                return WeekdayType.TUESDAY;
            }
            case "Onsdag" : {
                return WeekdayType.WEDNESDAY;
            }
            case "Torsdag" : {
                return WeekdayType.THURSDAY;
            }
            case "Fredag" : {
                return WeekdayType.FRIDAY;
            }
            case "Lördag" : {
                return WeekdayType.SATURDAY;
            }
            case "Söndag" : {
                return WeekdayType.SUNDAY;
            }
            default :
                // Ignore...
        }

        // Should not happen...
        throw new RuntimeException("Unknown weekday string: " + weekdayString);
    }

    /**
     * Returns the track time as a String (HH:mm) for an XML Calendar
     * @param xmlCalendar An XML calendar
     * @return The track time as a string/human time value
     */
    public static String getTrackTimeFromCalendar(XMLGregorianCalendar xmlCalendar) {

        if (xmlCalendar == null) {
            return null;
        }

        final SimpleDateFormat trackTimeFormat = new SimpleDateFormat(START_TIME_DATE_FORMAT);
        return trackTimeFormat.format(xmlCalendar.toGregorianCalendar().getTime());
    }

    /**
     * Returns the track time as an XMLCalendar, given a HH:mm string format
     * @param trackTime The track time as a string/human time value
     * @return An XML calendar
     */
    public static XMLGregorianCalendar getCalendarFromTrackTime(String trackTime) {

        if (trackTime == null) {
            return null;
        }

        try {
            final SimpleDateFormat trackTimeFormat = new SimpleDateFormat(START_TIME_DATE_FORMAT);

            GregorianCalendar gregorianCal = new GregorianCalendar();
            gregorianCal.setTime(trackTimeFormat.parse(trackTime));
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCal);

        } catch (ParseException | DatatypeConfigurationException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Returns all existing tracks
     * @return All valid track numbers to choose from
     */
    public static List<Integer> getAllTracks() {

        List<Integer> trackNrs = new ArrayList<>();
        int trackNr = SquashProperties.FIRST_TRACK_NR;
        while (trackNr <= SquashProperties.LAST_TRACK_NR) {
            trackNrs.add(Integer.valueOf(trackNr));
            ++trackNr;
        }
        return trackNrs;
    }

    /**
     * Returns all possible/valid track start times to choose from
     * @return All valid start times in HH:mm format
     */
    public static List<String> getAllStartTimes() {

        final SimpleDateFormat trackTimeFormat = new SimpleDateFormat(START_TIME_DATE_FORMAT);

        // This is the number of bookable tracks
        final int nrOfHours = SquashProperties.LAST_TRACK_HOUR
            - SquashProperties.FIRST_TRACK_HOUR
            + 1;
        final List<String> startTimes = new ArrayList<>(nrOfHours);

        int nrOfSteps = 1;
        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.set(Calendar.MILLISECOND, 0);
        timeCalendar.set(Calendar.SECOND, 0);
        timeCalendar.set(Calendar.MINUTE, 0);
        timeCalendar.set(Calendar.HOUR_OF_DAY, SquashProperties.FIRST_TRACK_HOUR);

        startTimes.add(trackTimeFormat.format(timeCalendar.getTime()));

        // ...and step through all hours
        while (nrOfSteps < nrOfHours) {
            timeCalendar.add(Calendar.HOUR_OF_DAY, 1);
            startTimes.add(trackTimeFormat.format(timeCalendar.getTime()));
            ++nrOfSteps;
        }

        return startTimes;
    }

    /**
     * Returns true if string is not null and contains non whitespace characters
     * @return True if string has a value
     */
    public static boolean isSet(String str) {
        return str != null && str.trim().length() > 0;
    }

    /**
     * Strips the filename path from a given file path
     * @return The filename only, or null if given path was null/empty
     */
    public static String getFilenameFromPath(String fullPath) {

        if (!isSet(fullPath)) {
            return null;
        }

        Path fullPathObject = Paths.get(fullPath);
        Path fileName = fullPathObject.getFileName();
        if (fileName != null) {
            return fileName.toString();
        }
        return null;
    }
}
