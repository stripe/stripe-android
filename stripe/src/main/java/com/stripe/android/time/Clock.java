package com.stripe.android.time;

import java.util.Calendar;

public class Clock {
    private static Clock mInstance;
    protected Calendar mCalendarInstance;

    protected static Clock getInstance() {
        if (mInstance == null) {
            mInstance = new Clock();
        }
        return mInstance;
    }

    private Calendar _calendarInstance() {
        return (mCalendarInstance != null) ?
                (Calendar) mCalendarInstance.clone() : Calendar.getInstance();
    }

    public static Calendar getCalendarInstance() {
        return getInstance()._calendarInstance();
    }
}
