package com.stripe.android.util;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.StringDef;

import com.stripe.android.BuildConfig;
import com.stripe.android.net.StripeApiHandler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Util class to create logging items, which are fed as {@link java.util.Map Map} objects in
 * query parameters to our server.
 */
public class LoggingUtils {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            CARD_WIDGET_TOKEN
    })
    public @interface LoggingToken { }
    public static final String CARD_WIDGET_TOKEN = "CardInputView";
    public static final Set<String> VALID_LOGGING_TOKENS = new HashSet<>();
    static {
        VALID_LOGGING_TOKENS.add(CARD_WIDGET_TOKEN);
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            EVENT_TOKEN_CREATION
    })
    public @interface LoggingEventName { }
    public static final String EVENT_TOKEN_CREATION = "token_creation";
    public static final String FIELD_PRODUCT_USAGE = "product_usage";

    private static final String ANALYTICS_PREFIX = "analytics";
    private static final String ANALYTICS_NAME = "stripe_android";
    private static final String ANALYTICS_VERSION = "1.0";
    private static final String FIELD_ANALYTICS_UA = "analytics_ua";
    private static final String FIELD_EVENT = "event";
    private static final String FIELD_BINDINGS_VERSION = "bindings_version";
    private static final String FIELD_DEVICE_TYPE = "device_type";
    private static final String FIELD_OS_VERSION = "os_version";
    private static final String FIELD_PUBLISHABLE_KEY = "publishable_key";
    private static final String FIELD_JAVA_BINDINGS_VERSION = "java_bindings";

    @NonNull
    public static Map<String, Object> getTokenCreationParams(
            @NonNull List<String> productUsageTokens,
            @NonNull String publishableApiKey) {
        Map<String, Object> paramsObject = new HashMap<>();
        paramsObject.put(FIELD_ANALYTICS_UA, getAnalyticsUa());
        paramsObject.put(FIELD_EVENT, getEventName(EVENT_TOKEN_CREATION));
        paramsObject.put(FIELD_PUBLISHABLE_KEY, publishableApiKey);
        paramsObject.put(FIELD_OS_VERSION, Build.VERSION.SDK_INT);
        paramsObject.put(FIELD_DEVICE_TYPE, getDeviceLoggingString());
        paramsObject.put(FIELD_BINDINGS_VERSION, BuildConfig.VERSION_NAME);
        paramsObject.put(FIELD_JAVA_BINDINGS_VERSION, StripeApiHandler.VERSION);
        paramsObject.put(FIELD_PRODUCT_USAGE, productUsageTokens);
        return paramsObject;
    }

    @NonNull
    static String getDeviceLoggingString() {
        StringBuilder builder = new StringBuilder();
        builder.append(Build.MANUFACTURER).append('_')
                .append(Build.BRAND).append('_')
                .append(Build.MODEL);
        return builder.toString();
    }

    @NonNull
    static String getAnalyticsUa() {
        return ANALYTICS_PREFIX + "." + ANALYTICS_NAME + "-" + ANALYTICS_VERSION;
    }

    @NonNull
    static String getEventName(@NonNull @LoggingEventName String eventName) {
        return ANALYTICS_NAME + '.' + eventName;
    }
}
