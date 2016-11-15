package com.stripe.android.util;

import com.stripe.android.time.Clock;

import java.util.Calendar;
import java.util.Locale;

public class DateUtils {

    /**
     * Determines whether or not the input year has already passed.
     *
     * @param year the input year, as a two or four-digit integer
     * @return {@code true} if the year has passed, {@code false} otherwise.
     */
    public static boolean hasYearPassed(int year) {
        int normalized = normalizeYear(year);
        Calendar now = Clock.getCalendarInstance();
        return normalized < now.get(Calendar.YEAR);
    }

    /**
     * Determines whether the input year-month pair has passed.
     *
     * @param year the input year, as a two or four-digit integer
     * @param month the input month
     * @return {@code true} if the input time has passed, {@code false} otherwise.
     */
    public static boolean hasMonthPassed(int year, int month) {
        if (hasYearPassed(year)) {
            return true;
        }

        Calendar now = Clock.getCalendarInstance();
        // Expires at end of specified month, Calendar month starts at 0
        return normalizeYear(year) == now.get(Calendar.YEAR)
                && month < (now.get(Calendar.MONTH) + 1);
    }

    // Convert two-digit year to full year if necessary
    private static int normalizeYear(int year)  {
        if (year < 100 && year >= 0) {
            Calendar now = Clock.getCalendarInstance();
            String currentYear = String.valueOf(now.get(Calendar.YEAR));
            String prefix = currentYear.substring(0, currentYear.length() - 2);
            year = Integer.parseInt(String.format(Locale.US, "%s%02d", prefix, year));
        }
        return year;
    }
}
