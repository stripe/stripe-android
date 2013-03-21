package com.stripe.android.util;

import com.stripe.android.time.Clock;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static boolean hasYearPassedOrInvalid(String year) {
        try {
            int yearVal = parseYear(year);
            Calendar now = Clock.getCalendarInstance();
            return yearVal < now.get(Calendar.YEAR);
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean hasMonthPassedOrInvalid(String year, String month) {
        try {
            Calendar now = Clock.getCalendarInstance();
            // Expires at end of specified month, Calendar month starts at 0
            return hasYearPassedOrInvalid(year) || parseYear(year) == now.get(Calendar.YEAR) && Integer.parseInt(month) < (now.get(Calendar.MONTH) + 1);
        } catch (Exception e) {
            return true;
        }
    }

    private static int parseYear(String year) throws ParseException {
        int yearVal = Integer.parseInt(year);
        if (yearVal < 100 && yearVal >= 0) {
            SimpleDateFormat twoDigitFormat = new SimpleDateFormat("yy");
            SimpleDateFormat fourDigitFormat = new SimpleDateFormat("yyyy");
            Date date = twoDigitFormat.parse(year);
            yearVal = Integer.parseInt(fourDigitFormat.format(date));
        }
        return yearVal;
    }
}
