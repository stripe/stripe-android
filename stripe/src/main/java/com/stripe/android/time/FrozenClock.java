package com.stripe.android.time;

import java.util.Calendar;

public class FrozenClock extends Clock {
    public static void freeze(Calendar freeze) {
        getInstance().calendarInstance = freeze;
    }

    public static void unfreeze() {
        getInstance().calendarInstance = null;
    }
}