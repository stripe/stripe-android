package com.stripe.android.util;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.stripe.android.time.Clock;

public class CardExpiry {
    private boolean mPatternMismatch = false;
    private String mMonth = "";
    private String mYear = "";

    private Pattern mDatePattern = Pattern
            .compile("^([0-9][0-9]?)?/?([0-9][0-9]?[0-9]?[0-9]?)?$");

    public CardExpiry() {
    }

    public CardExpiry(String str) {
        updateFromString(str);
    }

    @Override
    public String toString() {
        if (mYear.length() == 0) {
            return mMonth;
        }
        return mMonth + "/" + mYear;
    }

    public String toStringWithTrail() {
        if (mYear.length() == 0 && mMonth.length() == 2) {
            return mMonth + "/";
        }
        return toString();
    }

    public boolean isValid() {
        return isValidLength() && isValidDate();
    }

    private boolean isValidLength() {
        return (mMonth.length() == 2) && (mYear.length() == 2 || mYear.length() == 4);
    }

    private boolean isValidDate() {
        if (getMonth() < 1 || getMonth() > 12) {
            return false;
        }

        Date now = Clock.getCalendarInstance().getTime();
        return now.before(getExpiryDate());
    }

    public boolean isPartiallyValid() {
        if (mPatternMismatch) {
            return false;
        }
        if (isValidLength()) {
            return isValidDate();
        } else {
            return getMonth() <= 12 && mYear.length() <= 4;
        }
    }

    public int getMonth() {
        try {
            return Integer.parseInt(mMonth);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getYear() {
        try {
            int year = Integer.parseInt(mYear);
            return DateUtils.normalizeYear(year);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Date getExpiryDate() {
        Calendar cal = Calendar.getInstance();
        // Card expires at the end of the month, so we should add one to the current month
        // But Calendar is zero-based, so we need to subtract one from getMonth()
        cal.set(getYear(), getMonth(), 1);
        return cal.getTime();
    }

    public void updateFromString(String str) {
        Matcher m = mDatePattern.matcher(str);
        mPatternMismatch = !m.find();
        if (mPatternMismatch) {
            mMonth = "";
            mYear = "";
            return;
        }
        String monthStr = m.group(1);
        if (monthStr == null) {
            mMonth = "";
        } else {
            mMonth = monthStr;
            if (mMonth.length() == 1) {
                if (!(mMonth.equals("0") || mMonth.equals("1"))) {
                    mMonth = "0" + mMonth;
                }
            }
        }
        String yearStr = m.group(2);
        if (yearStr == null) {
            mYear = "";
        } else {
            mYear = yearStr;
        }
    }
}