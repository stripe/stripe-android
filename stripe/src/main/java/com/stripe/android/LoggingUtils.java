package com.stripe.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.model.Source;
import com.stripe.android.model.Token;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
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

    static final String UNKNOWN = "unknown";
    static final String NO_CONTEXT = "no_context";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            EventName.TOKEN_CREATION,
            EventName.ADD_PAYMENT_METHOD,
            EventName.ATTACH_PAYMENT_METHOD,
            EventName.DETACH_PAYMENT_METHOD,
            EventName.SOURCE_CREATION,
            EventName.ADD_SOURCE,
            EventName.DEFAULT_SOURCE,
            EventName.DELETE_SOURCE,
            EventName.SET_SHIPPING_INFO,
            EventName.CONFIRM_PAYMENT_INTENT,
            EventName.RETRIEVE_PAYMENT_INTENT,
            EventName.CONFIRM_SETUP_INTENT,
            EventName.RETRIEVE_SETUP_INTENT,
            EventName.START_3DS2_AUTH,
            EventName.COMPLETE_3DS2_AUTH})
    @interface EventName {
        String TOKEN_CREATION = "token_creation";
        String ADD_PAYMENT_METHOD = "add_payment_method";
        String ATTACH_PAYMENT_METHOD = "attach_payment_method";
        String DETACH_PAYMENT_METHOD = "detach_payment_method";
        String SOURCE_CREATION = "source_creation";
        String ADD_SOURCE = "add_source";
        String DEFAULT_SOURCE = "default_source";
        String DELETE_SOURCE = "delete_source";
        String SET_SHIPPING_INFO = "set_shipping_info";
        String CONFIRM_PAYMENT_INTENT = "payment_intent_confirmation";
        String RETRIEVE_PAYMENT_INTENT = "payment_intent_retrieval";
        String CONFIRM_SETUP_INTENT = "setup_intent_confirmation";
        String RETRIEVE_SETUP_INTENT = "setup_intent_retrieval";

        String START_3DS2_AUTH = "start_3ds2_auth";
        String COMPLETE_3DS2_AUTH = "complete_3ds2_auth";
    }

    static final String FIELD_PRODUCT_USAGE = "product_usage";
    static final String FIELD_ANALYTICS_UA = "analytics_ua";
    static final String FIELD_APP_NAME = "app_name";
    static final String FIELD_APP_VERSION = "app_version";
    static final String FIELD_BINDINGS_VERSION = "bindings_version";
    static final String FIELD_DEVICE_TYPE = "device_type";
    static final String FIELD_EVENT = "event";
    static final String FIELD_OS_NAME = "os_name";
    static final String FIELD_OS_RELEASE = "os_release";
    static final String FIELD_OS_VERSION = "os_version";
    static final String FIELD_PUBLISHABLE_KEY = "publishable_key";
    static final String FIELD_SOURCE_TYPE = "source_type";
    static final String FIELD_TOKEN_TYPE = "token_type";
    static final Set<String> VALID_PARAM_FIELDS = new HashSet<>(Arrays.asList(
            FIELD_ANALYTICS_UA, FIELD_APP_NAME, FIELD_APP_VERSION, FIELD_BINDINGS_VERSION,
            FIELD_DEVICE_TYPE, FIELD_EVENT, FIELD_OS_VERSION, FIELD_OS_NAME, FIELD_OS_RELEASE,
            FIELD_PRODUCT_USAGE, FIELD_PUBLISHABLE_KEY, FIELD_SOURCE_TYPE, FIELD_TOKEN_TYPE));

    private static final String ANALYTICS_PREFIX = "analytics";
    private static final String ANALYTICS_NAME = "stripe_android";
    private static final String ANALYTICS_VERSION = "1.0";

    @Nullable private final PackageManager mPackageManager;
    @Nullable private final String mPackageName;

    LoggingUtils(@NonNull Context context) {
        this(context.getPackageManager(), context.getPackageName());
    }

    @VisibleForTesting
    LoggingUtils(@Nullable PackageManager packageManager, @Nullable String packageName) {
        mPackageManager = packageManager;
        mPackageName = packageName;
    }

    @NonNull
    Map<String, Object> getTokenCreationParams(
            @Nullable List<String> productUsageTokens,
            @NonNull String publishableApiKey,
            @Nullable String tokenType) {
        return getEventLoggingParams(
                productUsageTokens,
                null,
                tokenType,
                publishableApiKey, EventName.TOKEN_CREATION);
    }

    @NonNull
    Map<String, Object> getPaymentMethodCreationParams(@NonNull String publishableApiKey) {
        return getEventLoggingParams(publishableApiKey, EventName.ADD_PAYMENT_METHOD);
    }

    @NonNull
    Map<String, Object> getSourceCreationParams(
            @Nullable List<String> productUsageTokens,
            @NonNull String publishableApiKey,
            @NonNull @Source.SourceType String sourceType) {
        return getEventLoggingParams(
                productUsageTokens,
                sourceType,
                null,
                publishableApiKey, EventName.SOURCE_CREATION);
    }

    @NonNull
    Map<String, Object> getAddSourceParams(
            @Nullable List<String> productUsageTokens,
            @NonNull String publishableKey,
            @NonNull @Source.SourceType String sourceType) {
        return getEventLoggingParams(
                productUsageTokens,
                sourceType,
                null,
                publishableKey, EventName.ADD_SOURCE);
    }

    @NonNull
    Map<String, Object> getDeleteSourceParams(
            @Nullable List<String> productUsageTokens,
            @NonNull String publishableKey) {
        return getEventLoggingParams(productUsageTokens, publishableKey, EventName.DELETE_SOURCE);
    }

    @NonNull
    Map<String, Object> getAttachPaymentMethodParams(
            @Nullable List<String> productUsageTokens,
            @NonNull String publishableKey) {
        return getEventLoggingParams(productUsageTokens, publishableKey,
                EventName.ATTACH_PAYMENT_METHOD);
    }

    @NonNull
    Map<String, Object> getDetachPaymentMethodParams(
            @Nullable List<String> productUsageTokens,
            @NonNull String publishableKey) {
        return getEventLoggingParams(productUsageTokens, publishableKey,
                EventName.DETACH_PAYMENT_METHOD);
    }

    @NonNull
    Map<String, Object> getPaymentIntentConfirmationParams(
            @Nullable List<String> productUsageTokens,
            @NonNull String publishableApiKey,
            @Nullable @Source.SourceType String sourceType) {
        return getEventLoggingParams(
                productUsageTokens,
                sourceType,
                null,
                publishableApiKey, EventName.CONFIRM_PAYMENT_INTENT);
    }

    @NonNull
    Map<String, Object> getPaymentIntentRetrieveParams(
            @Nullable List<String> productUsageTokens,
            @NonNull String publishableApiKey) {
        return getEventLoggingParams(
                productUsageTokens,
                publishableApiKey, EventName.RETRIEVE_PAYMENT_INTENT);
    }

    @NonNull
    Map<String, Object> getSetupIntentConfirmationParams(@NonNull String publishableApiKey) {
        return getEventLoggingParams(publishableApiKey, EventName.CONFIRM_SETUP_INTENT);
    }

    @NonNull
    Map<String, Object> getSetupIntentRetrieveParams(
            @NonNull String publishableApiKey) {
        return getEventLoggingParams(publishableApiKey, EventName.RETRIEVE_SETUP_INTENT);
    }

    @NonNull
    Map<String, Object> getEventLoggingParams(
            @NonNull String publishableApiKey,
            @NonNull @EventName String eventName) {
        return getEventLoggingParams(null, null, null, publishableApiKey, eventName);
    }

    @NonNull
    Map<String, Object> getEventLoggingParams(
            @Nullable List<String> productUsageTokens,
            @NonNull String publishableApiKey,
            @NonNull @EventName String eventName) {
        return getEventLoggingParams(productUsageTokens, null, null, publishableApiKey, eventName);
    }

    @NonNull
    Map<String, Object> getEventLoggingParams(
            @Nullable List<String> productUsageTokens,
            @Nullable @Source.SourceType String sourceType,
            @Nullable @Token.TokenType String tokenType,
            @NonNull String publishableApiKey,
            @NonNull @EventName String eventName) {
        final Map<String, Object> paramsObject = new HashMap<>();
        paramsObject.put(FIELD_ANALYTICS_UA, getAnalyticsUa());
        paramsObject.put(FIELD_EVENT, getEventParamName(eventName));
        paramsObject.put(FIELD_PUBLISHABLE_KEY, publishableApiKey);
        paramsObject.put(FIELD_OS_NAME, Build.VERSION.CODENAME);
        paramsObject.put(FIELD_OS_RELEASE, Build.VERSION.RELEASE);
        paramsObject.put(FIELD_OS_VERSION, Build.VERSION.SDK_INT);
        paramsObject.put(FIELD_DEVICE_TYPE, getDeviceLoggingString());
        paramsObject.put(FIELD_BINDINGS_VERSION, BuildConfig.VERSION_NAME);

        paramsObject.putAll(createNameAndVersionParams());

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
    Map<String, Object> createNameAndVersionParams() {
        final Map<String, Object> paramsObject = new HashMap<>(2);

        if (mPackageManager != null) {
            try {
                final PackageInfo info = mPackageManager.getPackageInfo(mPackageName, 0);

                final String nameString;
                if (info.applicationInfo != null) {
                    final CharSequence name = info.applicationInfo.loadLabel(mPackageManager);
                    nameString = name != null ? name.toString() : null;
                    paramsObject.put(FIELD_APP_NAME, nameString);
                } else {
                    nameString = null;
                }

                if (StripeTextUtils.isBlank(nameString)) {
                    paramsObject.put(FIELD_APP_NAME, info.packageName);
                }

                paramsObject.put(FIELD_APP_VERSION, info.versionCode);
            } catch (PackageManager.NameNotFoundException nameNotFound) {
                paramsObject.put(FIELD_APP_NAME, UNKNOWN);
                paramsObject.put(FIELD_APP_VERSION, UNKNOWN);
            }
        } else {
            paramsObject.put(FIELD_APP_NAME, NO_CONTEXT);
            paramsObject.put(FIELD_APP_VERSION, NO_CONTEXT);
        }

        return paramsObject;
    }

    @NonNull
    private static String getDeviceLoggingString() {
        return Build.MANUFACTURER + '_' + Build.BRAND + '_' + Build.MODEL;
    }

    @NonNull
    static String getAnalyticsUa() {
        return ANALYTICS_PREFIX + "." + ANALYTICS_NAME + "-" + ANALYTICS_VERSION;
    }

    @NonNull
    static String getEventParamName(@NonNull @EventName String eventName) {
        return ANALYTICS_NAME + '.' + eventName;
    }
}
