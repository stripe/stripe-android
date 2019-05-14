package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
        return value != null && isDigitsOnly(value);
    }

    /**
     * Returns whether the given CharSequence contains only digits.
     */
    private static boolean isDigitsOnly(@NonNull CharSequence str) {
        final int len = str.length();
        for (int cp, i = 0; i < len; i += Character.charCount(cp)) {
            cp = Character.codePointAt(str, i);
            if (!Character.isDigit(cp)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether the input year-month pair has passed.
     *
     * @param year the input year, as a two or four-digit integer
     * @param month the input month
     * @param now the current time
     * @return {@code true} if the input time has passed the specified current time,
     *  {@code false} otherwise.
     */
    static boolean hasMonthPassed(int year, int month, @NonNull Calendar now) {
        if (hasYearPassed(year, now)) {
            return true;
        }

        // Expires at end of specified month, Calendar month starts at 0
        return normalizeYear(year, now) == now.get(Calendar.YEAR)
                && month < (now.get(Calendar.MONTH) + 1);
    }

    /**
     * Determines whether or not the input year has already passed.
     *
     * @param year the input year, as a two or four-digit integer
     * @param now, the current time
     * @return {@code true} if the input year has passed the year of the specified current time
     *  {@code false} otherwise.
     */
    static boolean hasYearPassed(int year, @NonNull Calendar now) {
        int normalized = normalizeYear(year, now);
        return normalized < now.get(Calendar.YEAR);
    }

    static int normalizeYear(int year, @NonNull Calendar now)  {
        if (year < 100 && year >= 0) {
            String currentYear = String.valueOf(now.get(Calendar.YEAR));
            String prefix = currentYear.substring(0, currentYear.length() - 2);
            year = Integer.parseInt(String.format(Locale.US, "%s%02d", prefix, year));
        }
        return year;
    }
}
