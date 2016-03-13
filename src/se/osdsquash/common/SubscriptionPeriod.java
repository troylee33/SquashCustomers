package se.osdsquash.common;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A subscription period, lasting X months with a start and end day
 */
public class SubscriptionPeriod {

    private static final String DAY_FORMAT = "yyyy-MM-dd";

    private Calendar startDay;
    private Calendar endDay;

    /**
     * Creates a new period, starting on the NEXT even period from "now".
     * 
     * <p>
     * Example: If the current date is 2016-10-22, the period's start date will be 2017-01-01.
     * </p>
     */
    public SubscriptionPeriod() {
        this.initCalendars();
    }

    private void initCalendars() {

        Calendar thisPeriodPassedCal = Calendar.getInstance();
        thisPeriodPassedCal.set(Calendar.MILLISECOND, 0);
        thisPeriodPassedCal.set(Calendar.SECOND, 0);
        thisPeriodPassedCal.set(Calendar.MINUTE, 0);
        thisPeriodPassedCal.set(Calendar.HOUR_OF_DAY, 0);
        thisPeriodPassedCal.set(Calendar.DATE, 1);
        thisPeriodPassedCal.set(Calendar.MONTH, SquashProperties.NR_OF_MONTHS);

        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.MILLISECOND, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.HOUR_OF_DAY, 0);

        // If the second subscription period of this year is still to come, use that period.
        // Otherwise, use the next year's first period.
        if (startCal.before(thisPeriodPassedCal)) {
            this.startDay = thisPeriodPassedCal;
        } else {
            thisPeriodPassedCal.add(Calendar.MONTH, SquashProperties.NR_OF_MONTHS);
            this.startDay = thisPeriodPassedCal;
        }

        // Set the end date, always relative to the start date
        Calendar endDay = (Calendar) this.startDay.clone();
        endDay.add(Calendar.MONTH, SquashProperties.NR_OF_MONTHS);
        endDay.add(Calendar.DATE, -1); // Exclude the next month's first day
        this.endDay = endDay;
    }

    public Calendar getStartDay() {
        return this.startDay;
    }

    public Calendar getEndDay() {
        return this.endDay;
    }

    public String getStartDayString() {
        return new SimpleDateFormat(DAY_FORMAT).format(this.startDay.getTime());
    }

    public String getEndDayString() {
        return new SimpleDateFormat(DAY_FORMAT).format(this.endDay.getTime());
    }

    // Returns the period as one string
    public String getPeriodString() {
        SimpleDateFormat format = new SimpleDateFormat(DAY_FORMAT);
        return format.format(this.startDay.getTime())
            + " till "
            + format.format(this.endDay.getTime());
    }
}
