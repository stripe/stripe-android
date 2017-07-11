package com.stripe.android.model;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.stripe.android.time.Clock;

import java.util.Calendar;
import java.util.Locale;

/**
 * Utilities function class for the models package.
 */
class ModelUtils {

    /**
     * Check to see whether the input string is a whole, positive number.
     *
     * @param value the input string to test
     * @return {@code true} if the input value consists entirely of integers
     */
    static boolean isWholePositiveNumber(@Nullable String value) {
        return value != null && TextUtils.isDigitsOnly(value);
    }

    /**
     * Determines whether the input year-month pair has passed.
     *
     * @param year the input year, as a two or four-digit integer
     * @param month the input month
     * @return {@code true} if the input time has passed, {@code false} otherwise.
     */
    @SuppressWarnings("WrongConstant")
    static boolean hasMonthPassed(int year, int month) {
        if (hasYearPassed(year)) {
            return true;
        }

        Calendar now = Clock.getCalendarInstance();
        // Expires at end of specified month, Calendar month starts at 0
        return normalizeYear(year) == now.get(Calendar.YEAR)
                && month < (now.get(Calendar.MONTH) + 1);
    }

    /**
     * Determines whether or not the input year has already passed.
     *
     * @param year the input year, as a two or four-digit integer
     * @return {@code true} if the year has passed, {@code false} otherwise.
     */
    static boolean hasYearPassed(int year) {
        int normalized = normalizeYear(year);
        Calendar now = Clock.getCalendarInstance();
        return normalized < now.get(Calendar.YEAR);
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
