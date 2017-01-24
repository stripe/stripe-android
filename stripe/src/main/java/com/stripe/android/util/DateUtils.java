package com.stripe.android.util;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.model.Card;
import com.stripe.android.time.Clock;

import java.util.Calendar;
import java.util.Locale;

public class DateUtils {

    static final int MAX_VALID_YEAR = 9980;

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

    /**
     * Checks to see if the string input represents a valid month.
     *
     * @param monthString user input representing the month
     * @return {@code true} if the string is a number between "01" and "12" inclusive,
     * {@code false} otherwise
     */
    public static boolean isValidMonth(@Nullable String monthString) {
        if (monthString == null) {
            return false;
        }

        try {
            int monthInt = Integer.parseInt(monthString);
            return monthInt > 0 && monthInt <= 12;
        } catch (NumberFormatException numEx) {
            return false;
        }
    }

    /**
     * Separates raw string input of the format MMYY into a "month" group and a "year" group.
     * Either or both of these may be incomplete. This method does not check to see if the input
     * is valid.
     *
     * @param expiryInput up to four characters of user input
     * @return a length-2 array containing the first two characters in the 0 index, and the last
     * two characters in the 1 index. "123" gets split into {"12" , "3"}, and "1" becomes {"1", ""}.
     */
    @Size(2)
    @NonNull
    public static String[] separateDateStringParts(@NonNull @Size(max = 4) String expiryInput) {
        String[] parts = new String[2];
        if (expiryInput.length() >= 2) {
            parts[0] = expiryInput.substring(0, 2);
            parts[1] = expiryInput.substring(2);
        } else {
            parts[0] = expiryInput;
            parts[1] = "";
        }
        return parts;
    }

    /**
     * Checks whether or not the input month and year has yet expired.
     *
     * @param expiryMonth An integer representing a month. Only values 1-12 are valid,
     *                    but this is called by user input, so we have to check outside that range.
     * @param expiryYear An integer representing the full year (2017, not 17). Only positive values
     *                   are valid, but this is called by user input, so we have to check outside
     *                   for otherwise nonsensical dates. This code cannot validate years greater
     *                   than {@link #MAX_VALID_YEAR 9980} because of how we parse years in
     *                   {@link #convertTwoDigitYearToFour(int, Calendar)}.
     * @return {@code true} if the current month and year is the same as or later than the input
     * month and year, {@code false} otherwise. Note that some cards expire on the first of the
     * month, but we don't validate that here.
     */
    public static boolean isExpiryDataValid(int expiryMonth, int expiryYear) {
        return isExpiryDataValid(expiryMonth, expiryYear, Calendar.getInstance());
    }

    @VisibleForTesting
    static boolean isExpiryDataValid(int expiryMonth, int expiryYear, @NonNull Calendar calendar) {
        if (expiryMonth < 1 || expiryMonth > 12) {
            return false;
        }

        if (expiryYear < 0 || expiryYear > MAX_VALID_YEAR) {
            return false;
        }

        int currentYear = calendar.get(Calendar.YEAR);
        if (expiryYear < currentYear) {
            return false;
        } else if (expiryYear > currentYear) {
            return true;
        } else { // the card expires this year
            int readableMonth = calendar.get(Calendar.MONTH) + 1;
            return expiryMonth >= readableMonth;
        }
    }

    /**
     * Converts a two-digit input year to a four-digit year. As the current calendar year
     * approaches a century, we assume small values to mean the next century. For instance, if
     * the current year is 2090, and the input value is "18", the user probably means 2118,
     * not 2018. However, in 2017, the input "18" probably means 2018. This code should be
     * updated before the year 9981.
     *
     * @param inputYear a two-digit integer, between 0 and 99, inclusive
     * @return a four-digit year
     */
    @IntRange(from = 1000, to = 9999)
    public static int convertTwoDigitYearToFour(@IntRange(from = 0, to = 99) int inputYear) {
        return convertTwoDigitYearToFour(inputYear, Calendar.getInstance());
    }

    @VisibleForTesting
    @IntRange(from = 1000, to = 9999)
    static int convertTwoDigitYearToFour(
            @IntRange(from = 0, to = 99) int inputYear,
            @NonNull Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        // Intentional integer division
        int centuryBase = year / 100;
        if (year % 100 > 80 && inputYear < 20) {
            centuryBase++;
        } else if (year % 100 < 20 && inputYear > 80) {
            centuryBase--;
        }
        return centuryBase * 100 + inputYear;
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
