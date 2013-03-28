package com.stripe.android.time;

import java.util.Calendar;

public class FrozenClock extends Clock {
    public static void freeze(Calendar freeze) {
        getInstance().mCalendarInstance = freeze;
    }

    public static void unfreeze() {
        getInstance().mCalendarInstance = null;
    }
}