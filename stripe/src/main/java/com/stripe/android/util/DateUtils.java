package com.stripe.android.util;

import com.stripe.android.time.Clock;

import java.util.Calendar;
import java.util.Locale;

public class DateUtils {
    public static boolean hasYearPassed(int year) {
        int normalized = normalizeYear(year);
        Calendar now = Clock.getCalendarInstance();
        return normalized < now.get(Calendar.YEAR);
    }

    public static boolean hasMonthPassed(int year, int month) {
        Calendar now = Clock.getCalendarInstance();
        // Expires at end of specified month, Calendar month starts at 0
        return hasYearPassed(year) || normalizeYear(year) == now.get(Calendar.YEAR) && month < (now.get(Calendar.MONTH) + 1);
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
