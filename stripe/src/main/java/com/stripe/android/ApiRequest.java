package com.stripe.android;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A class representing a Stripe API or Analytics request.
 */
final class ApiRequest extends StripeRequest {
    static final String MIME_TYPE = "application/x-www-form-urlencoded";

    static final String API_HOST = "https://api.stripe.com";

    @NonNull final Options options;
    @NonNull private final String mApiVersion;
    @Nullable private final AppInfo mAppInfo;

    ApiRequest(@NonNull Method method,
               @NonNull String url,
               @Nullable Map<String, ?> params,
               @NonNull Options options,
               @Nullable AppInfo appInfo) {
        super(method, url, params, MIME_TYPE);
        this.options = options;
        mApiVersion = ApiVersion.get().code;
        mAppInfo = appInfo;
    }

    @NonNull
    static ApiRequest createGet(@NonNull String url,
                                @NonNull Options options,
                                @Nullable AppInfo appInfo) {
        return new ApiRequest(Method.GET, url, null, options, appInfo);
    }

    @NonNull
    static ApiRequest createGet(@NonNull String url,
                                @NonNull Map<String, ?> params,
                                @NonNull Options options,
                                @Nullable AppInfo appInfo) {
        return new ApiRequest(Method.GET, url, params, options, appInfo);
    }

    @NonNull
    static ApiRequest createPost(@NonNull String url,
                                 @NonNull Options options,
                                 @Nullable AppInfo appInfo) {
        return new ApiRequest(Method.POST, url, null, options, appInfo);
    }

    @NonNull
    static ApiRequest createPost(@NonNull String url,
                                 @NonNull Map<String, ?> params,
                                 @NonNull Options options,
                                 @Nullable AppInfo appInfo) {
        return new ApiRequest(Method.POST, url, params, options, appInfo);
    }

    @NonNull
    static ApiRequest createDelete(@NonNull String url,
                                   @NonNull Options options,
                                   @Nullable AppInfo appInfo) {
        return new ApiRequest(Method.DELETE, url, null, options, appInfo);
    }

    @NonNull
    @Override
    Map<String, String> createHeaders() {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Accept-Charset", CHARSET);
        headers.put("Accept", "application/json");
        headers.put("X-Stripe-Client-User-Agent", createStripeClientUserAgent());
        headers.put("Stripe-Version", mApiVersion);
        headers.put("Authorization",
                String.format(Locale.ENGLISH, "Bearer %s", options.apiKey));
        if (options.stripeAccount != null) {
            headers.put("Stripe-Account", options.stripeAccount);
        }
        return headers;
    }

    @NonNull
    private String createStripeClientUserAgent() {
        final Map<String, String> propertyMap = new HashMap<>();
        final String javaVersion = System.getProperty("java.version");
        if (javaVersion != null) {
            propertyMap.put("java.version", javaVersion);
        }
        propertyMap.put("os.name", "android");
        propertyMap.put("os.version", String.valueOf(Build.VERSION.SDK_INT));
        propertyMap.put("bindings.version", BuildConfig.VERSION_NAME);
        propertyMap.put("lang", "Java");
        propertyMap.put("publisher", "Stripe");
        if (mAppInfo != null) {
            propertyMap.putAll(mAppInfo.createClientHeaders());
        }

        return new JSONObject(propertyMap).toString();
    }

    @NonNull
    @Override
    String getUserAgent() {
        final StringBuilder userAgent = new StringBuilder(DEFAULT_USER_AGENT);
        if (mAppInfo != null) {
            userAgent
                    .append(" ")
                    .append(mAppInfo.toUserAgent());
        }
        return userAgent.toString();
    }

    @NonNull
    @Override
    byte[] getOutputBytes() throws UnsupportedEncodingException, InvalidRequestException {
        return createQuery().getBytes(CHARSET);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(getBaseHashCode(), options, mAppInfo);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj) ||
                (obj instanceof ApiRequest && typedEquals((ApiRequest) obj));
    }

    private boolean typedEquals(@NonNull ApiRequest obj) {
        return super.typedEquals(obj) &&
                ObjectUtils.equals(options, obj.options) &&
                ObjectUtils.equals(mAppInfo, obj.mAppInfo);
    }

    /**
     * Data class representing options for a Stripe API request.
     */
    static final class Options {
        @NonNull final String apiKey;
        @Nullable final String stripeAccount;

        @NonNull
        static Options create(@NonNull String apiKey) {
            return new Options(apiKey, null);
        }

        @NonNull
        static Options create(@NonNull String apiKey, @Nullable String stripeAccount) {
            return new Options(apiKey, stripeAccount);
        }

        private Options(
                @NonNull String apiKey,
                @Nullable String stripeAccount) {
            this.apiKey = new ApiKeyValidator().requireValid(apiKey);
            this.stripeAccount = stripeAccount;
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(apiKey, stripeAccount);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof Options && typedEquals((Options) obj));
        }

        private boolean typedEquals(@NonNull Options obj) {
            return ObjectUtils.equals(apiKey, obj.apiKey) &&
                    ObjectUtils.equals(stripeAccount, obj.stripeAccount);
        }
    }
}
