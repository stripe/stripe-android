package com.stripe.android.util;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.stripe.android.time.Clock;

public class CardExpiry {
    private boolean patternMismatch = false;
    private String month = "";
    private String year = "";

    private Pattern datePattern = Pattern
            .compile("^([0-9][0-9]?)?/?([0-9][0-9]?[0-9]?[0-9]?)?$");

    public CardExpiry() {
    }

    public CardExpiry(String str) {
        updateFromString(str);
    }

    @Override
    public String toString() {
        if (this.year.length() == 0) {
            return this.month;
        }
        return this.month + "/" + this.year;
    }

    public String toStringWithTrail() {
        if (this.year.length() == 0 && this.month.length() == 2) {
            return this.month + "/";
        }
        return toString();
    }

    public boolean isValid() {
        return isValidLength() && isValidDate();
    }

    private boolean isValidLength() {
        return (this.month.length() == 2) && (this.year.length() == 2 || this.year.length() == 4);
    }

    private boolean isValidDate() {
        if (getMonth() < 1 || getMonth() > 12) {
            return false;
        }

        Date now = Clock.getCalendarInstance().getTime();
        return now.before(getExpiryDate());
    }

    public boolean isPartiallyValid() {
        if (patternMismatch) {
            return false;
        }
        if (isValidLength()) {
            return isValidDate();
        } else {
            return getMonth() <= 12 && this.year.length() <= 4;
        }
    }

    public int getMonth() {
        try {
            return Integer.parseInt(this.month);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getYear() {
        try {
            int year = Integer.parseInt(this.year);
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
        Matcher m = datePattern.matcher(str);
        patternMismatch = !m.find();
        if (patternMismatch) {
            this.month = "";
            this.year = "";
            return;
        }
        String monthStr = m.group(1);
        if (monthStr == null) {
            this.month = "";
        } else {
            this.month = monthStr;
            if (this.month.length() == 1) {
                if (!(this.month.equals("0") || this.month.equals("1"))) {
                    this.month = "0" + this.month;
                }
            }
        }
        String yearStr = m.group(2);
        if (yearStr == null) {
            this.year = "";
        } else {
            this.year = yearStr;
        }
    }
}