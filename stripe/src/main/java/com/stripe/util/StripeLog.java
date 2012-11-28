package com.stripe.util;

import android.util.Log;

public class StripeLog {

    // Turn off logging by default
    private static Integer logLevel = null;
    private static final String tag = "com.stripe.Stripe";

    public static void setLogLevel(Integer newLogLevel) {
        logLevel = newLogLevel;
    }

    private static boolean shouldLog(int level) {
        return logLevel != null && logLevel <= level;
    }

    public static void e(String format, Object... args) {
        if (shouldLog(Log.ERROR)) {
            Log.e(tag, String.format(format, args));
        }
    }

    public static void e(Exception e, String format, Object... args) {
        if (shouldLog(Log.ERROR)) {
            Log.e(tag, String.format(format, args), e);
        }
    }

    public static void e(Exception e) {
        if (shouldLog(Log.ERROR)) {
            Log.e(tag, e.getMessage(), e);
        }
    }

    public static void d(String format, Object... args) {
        if (shouldLog(Log.DEBUG)) {
            Log.d(tag, String.format(format, args));
        }
    }
}
