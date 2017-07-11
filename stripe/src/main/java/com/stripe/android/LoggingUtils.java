package com.stripe.android;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.stripe.android.model.Source;
import com.stripe.android.model.Token;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Util class to create logging items, which are fed as {@link java.util.Map Map} objects in
 * query parameters to our server.
 */
class LoggingUtils {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            EVENT_TOKEN_CREATION,
            EVENT_SOURCE_CREATION
    })
    @interface LoggingEventName { }
    static final String EVENT_TOKEN_CREATION = "token_creation";
    static final String EVENT_SOURCE_CREATION = "source_creation";
    static final String FIELD_PRODUCT_USAGE = "product_usage";

    static final String FIELD_ANALYTICS_UA = "analytics_ua";
    static final String FIELD_BINDINGS_VERSION = "bindings_version";
    static final String FIELD_DEVICE_TYPE = "device_type";
    static final String FIELD_EVENT = "event";
    static final String FIELD_OS_NAME = "os_name";
    static final String FIELD_OS_RELEASE = "os_release";
    static final String FIELD_OS_VERSION = "os_version";
    static final String FIELD_PUBLISHABLE_KEY = "publishable_key";
    static final String FIELD_SOURCE_TYPE = "source_type";
    static final String FIELD_TOKEN_TYPE = "token_type";
    static final Set<String> VALID_PARAM_FIELDS = new HashSet<>();
    static {
        VALID_PARAM_FIELDS.add(FIELD_ANALYTICS_UA);
        VALID_PARAM_FIELDS.add(FIELD_BINDINGS_VERSION);
        VALID_PARAM_FIELDS.add(FIELD_DEVICE_TYPE);
        VALID_PARAM_FIELDS.add(FIELD_EVENT);
        VALID_PARAM_FIELDS.add(FIELD_OS_VERSION);
        VALID_PARAM_FIELDS.add(FIELD_OS_NAME);
        VALID_PARAM_FIELDS.add(FIELD_OS_RELEASE);
        VALID_PARAM_FIELDS.add(FIELD_PRODUCT_USAGE);
        VALID_PARAM_FIELDS.add(FIELD_PUBLISHABLE_KEY);
        VALID_PARAM_FIELDS.add(FIELD_SOURCE_TYPE);
        VALID_PARAM_FIELDS.add(FIELD_TOKEN_TYPE);
    }

    private static final String ANALYTICS_PREFIX = "analytics";
    private static final String ANALYTICS_NAME = "stripe_android";
    private static final String ANALYTICS_VERSION = "1.0";

    @NonNull
    static Map<String, Object> getTokenCreationParams(
            @NonNull List<String> productUsageTokens,
            @NonNull String publishableApiKey,
            @Nullable String tokenType) {
        return getEventLoggingParams(
                productUsageTokens,
                null,
                tokenType,
                publishableApiKey,
                EVENT_TOKEN_CREATION);
    }

    @NonNull
    static Map<String, Object> getSourceCreationParams(
            @NonNull String publishableApiKey,
            @NonNull @Source.SourceType String sourceType) {
        return getEventLoggingParams(
                null,
                sourceType,
                null,
                publishableApiKey,
                EVENT_SOURCE_CREATION);
    }

    @NonNull
    static Map<String, Object> getEventLoggingParams(
            @Nullable List<String> productUsageTokens,
            @Nullable @Source.SourceType String sourceType,
            @Nullable @Token.TokenType String tokenType,
            @NonNull String publishableApiKey,
            @NonNull @LoggingEventName String eventName) {
        Map<String, Object> paramsObject = new HashMap<>();
        paramsObject.put(FIELD_ANALYTICS_UA, getAnalyticsUa());
        paramsObject.put(FIELD_EVENT, getEventParamName(eventName));
        paramsObject.put(FIELD_PUBLISHABLE_KEY, publishableApiKey);
        paramsObject.put(FIELD_OS_NAME, Build.VERSION.CODENAME);
        paramsObject.put(FIELD_OS_RELEASE, Build.VERSION.RELEASE);
        paramsObject.put(FIELD_OS_VERSION, Build.VERSION.SDK_INT);
        paramsObject.put(FIELD_DEVICE_TYPE, getDeviceLoggingString());
        paramsObject.put(FIELD_BINDINGS_VERSION, BuildConfig.VERSION_NAME);
        if (productUsageTokens != null) {
            paramsObject.put(FIELD_PRODUCT_USAGE, productUsageTokens);
        }

        if (sourceType != null) {
            paramsObject.put(FIELD_SOURCE_TYPE, sourceType);
        }

        if (tokenType != null) {
            paramsObject.put(FIELD_TOKEN_TYPE, tokenType);
        } else if (sourceType == null) {
            // This is not a source event, so to match iOS we log a token without type
            // as type "unknown"
            paramsObject.put(FIELD_TOKEN_TYPE, "unknown");
        }

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
    static String getEventParamName(@NonNull @LoggingEventName String eventName) {
        return ANALYTICS_NAME + '.' + eventName;
    }
}
