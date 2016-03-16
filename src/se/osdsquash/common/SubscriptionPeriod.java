package se.osdsquash.common;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A subscription period, lasting X months with start and end days
 */
public class SubscriptionPeriod {

    private static final String DAY_FORMAT = "yyyy-MM-dd";

    private Calendar startDay;
    private Calendar endDay;

    /**
     * Creates a new period, starting either on the NEXT even period from "now",
     * or returns the current/ongoing period.
     * <p>
     * Example: If the current date is 2016-10-22, the NEXT period's 
     *          start date will be 2017-01-01.
     * </p>
     * 
     * @param nextPeriod true if to create the next, upcoming period. False to create current.
     * @return A subscription period
     */
    public SubscriptionPeriod(boolean nextPeriod) {
        this.initCalendars(nextPeriod);
    }

    private void initCalendars(boolean nextPeriod) {

        Calendar thisPeriodPassedCal = SquashUtil.getTimeZeroedCalendar();
        thisPeriodPassedCal.set(Calendar.DATE, 1);
        thisPeriodPassedCal.set(Calendar.MONTH, SquashProperties.NR_OF_MONTHS);

        Calendar startCal = SquashUtil.getTimeZeroedCalendar();

        // If the second subscription period of this year is still to come,
        // use that as next period. Otherwise, use the next year's first period.
        if (nextPeriod) {
            if (startCal.before(thisPeriodPassedCal)) {
                this.startDay = thisPeriodPassedCal;
            } else {
                thisPeriodPassedCal.add(Calendar.MONTH, SquashProperties.NR_OF_MONTHS);
                this.startDay = thisPeriodPassedCal;
            }
        } else {
            if (startCal.get(Calendar.MONTH) < SquashProperties.NR_OF_MONTHS) {
                startCal.set(Calendar.MONTH, 0);
                startCal.set(Calendar.DATE, 1);
                this.startDay = startCal;
            } else {
                startCal.set(Calendar.MONTH, SquashProperties.NR_OF_MONTHS);
                startCal.set(Calendar.DATE, 1);
                this.startDay = startCal;
            }
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
