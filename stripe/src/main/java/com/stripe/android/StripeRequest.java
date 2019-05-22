package com.stripe.android;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class StripeRequest {
    private static final String CHARSET = "UTF-8";

    @NonNull final Method method;
    @Nullable final Map<String, ?> params;
    @NonNull final RequestOptions options;

    @NonNull private final String mUrl;

    @NonNull
    static StripeRequest createGet(@NonNull String url,
                                   @NonNull RequestOptions options) {
        return new StripeRequest(Method.GET, url, null, options);
    }

    @NonNull
    static StripeRequest createGet(@NonNull String url,
                                   @NonNull Map<String, ?> params,
                                   @NonNull RequestOptions options) {
        return new StripeRequest(Method.GET, url, params, options);
    }

    @NonNull
    static StripeRequest createPost(@NonNull String url,
                                    @NonNull RequestOptions options) {
        return new StripeRequest(Method.POST, url, null, options);
    }

    @NonNull
    static StripeRequest createPost(@NonNull String url,
                                    @NonNull Map<String, ?> params,
                                    @NonNull RequestOptions options) {
        return new StripeRequest(Method.POST, url, params, options);
    }

    @NonNull
    static StripeRequest createDelete(@NonNull String url,
                                      @NonNull RequestOptions options) {
        return new StripeRequest(Method.DELETE, url, null, options);
    }

    private StripeRequest(@NonNull Method method,
                          @NonNull String url,
                          @Nullable Map<String, ?> params,
                          @NonNull RequestOptions options) {
        this.method = method;
        this.mUrl = url;
        this.params = params;
        this.options = options;
    }

    /**
     * @return if the HTTP method is {@link Method#GET}, return URL with query string;
     *         otherwise, return the URL
     */
    @NonNull
    String getUrl()
            throws UnsupportedEncodingException, InvalidRequestException {
        return StripeRequest.Method.GET == method ? urlWithQuery() : mUrl;
    }

    @NonNull
    String getContentType() {
        final String mimeType;
        if (RequestOptions.RequestType.JSON.equals(options.getRequestType())) {
            mimeType = "application/json";
        } else {
            mimeType = "application/x-www-form-urlencoded";
        }
        return String.format(Locale.ROOT, "%s; charset=%s", mimeType, CHARSET);
    }

    @NonNull
    Map<String, String> getHeaders(@NonNull ApiVersion apiVersion) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Accept-Charset", CHARSET);
        headers.put("Accept", "application/json");
        headers.put("User-Agent",
                String.format(Locale.ROOT, "Stripe/v1 AndroidBindings/%s",
                        BuildConfig.VERSION_NAME));

        headers.put("Authorization", String.format(Locale.ENGLISH,
                "Bearer %s", options.getPublishableApiKey()));

        // debug headers
        final AbstractMap<String, String> propertyMap = new HashMap<>();
        propertyMap.put("java.version", System.getProperty("java.version"));
        propertyMap.put("os.name", "android");
        propertyMap.put("os.version", String.valueOf(Build.VERSION.SDK_INT));
        propertyMap.put("bindings.version", BuildConfig.VERSION_NAME);
        propertyMap.put("lang", "Java");
        propertyMap.put("publisher", "Stripe");

        headers.put("X-Stripe-Client-User-Agent", new JSONObject(propertyMap).toString());
        headers.put("Stripe-Version", apiVersion.getCode());

        if (options.getStripeAccount() != null) {
            headers.put("Stripe-Account", options.getStripeAccount());
        }

        if (options.getIdempotencyKey() != null) {
            headers.put("Idempotency-Key", options.getIdempotencyKey());
        }

        return headers;
    }

    @NonNull
    String createQuery() throws InvalidRequestException, UnsupportedEncodingException {
        final StringBuilder queryStringBuffer = new StringBuilder();
        for (Parameter flatParam : flattenParams(params)) {
            if (queryStringBuffer.length() > 0) {
                queryStringBuffer.append("&");
            }
            queryStringBuffer.append(urlEncodePair(flatParam.key, flatParam.value));
        }

        return queryStringBuffer.toString();
    }

    boolean urlStartsWith(@NonNull String... urlBases) {
        for (String urlBase : urlBases) {
            if (mUrl.startsWith(urlBase)) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    private String urlWithQuery()
            throws InvalidRequestException, UnsupportedEncodingException {
        final String query = createQuery();
        if (query.isEmpty()) {
            return mUrl;
        } else {
            // In some cases, URL can already contain a question mark
            // (eg, upcoming invoice lines)
            final String separator = mUrl.contains("?") ? "&" : "?";
            return String.format(Locale.ROOT, "%s%s%s", mUrl, separator, query);
        }
    }

    @NonNull
    private List<Parameter> flattenParams(@Nullable Map<String, ?> params)
            throws InvalidRequestException {
        return flattenParamsMap(params, null);
    }

    @NonNull
    private List<Parameter> flattenParamsList(@NonNull List<?> params,
                                                              @NonNull String keyPrefix)
            throws InvalidRequestException {
        final List<Parameter> flatParams = new LinkedList<>();

        // Because application/x-www-form-urlencoded cannot represent an empty
        // list, convention is to take the list parameter and just set it to an
        // empty string. (e.g. A regular list might look like `a[]=1&b[]=2`.
        // Emptying it would look like `a=`.)
        if (params.isEmpty()) {
            flatParams.add(new Parameter(keyPrefix, ""));
        } else {
            final String newPrefix = String.format(Locale.ROOT, "%s[]", keyPrefix);
            for (Object param : params) {
                flatParams.addAll(flattenParamsValue(param, newPrefix));
            }
        }

        return flatParams;
    }

    @NonNull
    private List<Parameter> flattenParamsMap(@Nullable Map<String, ?> params,
                                                             @Nullable String keyPrefix)
            throws InvalidRequestException {
        final List<Parameter> flatParams = new LinkedList<>();
        if (params == null) {
            return flatParams;
        }

        for (Map.Entry<String, ?> entry : params.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            final String newPrefix;
            if (keyPrefix != null) {
                newPrefix = String.format(Locale.ROOT, "%s[%s]", keyPrefix, key);
            } else {
                newPrefix = key;
            }

            flatParams.addAll(flattenParamsValue(value, newPrefix));
        }

        return flatParams;
    }

    @NonNull
    private List<Parameter> flattenParamsValue(@Nullable Object value,
                                                               @NonNull String keyPrefix)
            throws InvalidRequestException {
        final List<Parameter> flatParams;
        if (value instanceof Map<?, ?>) {
            flatParams = flattenParamsMap((Map<String, Object>) value, keyPrefix);
        } else if (value instanceof List<?>) {
            flatParams = flattenParamsList((List<?>) value, keyPrefix);
        } else if ("".equals(value)) {
            throw new InvalidRequestException("You cannot set '" + keyPrefix + "' to an empty "
                    + "string. " + "We interpret empty strings as null in requests. "
                    + "You may set '" + keyPrefix + "' to null to delete the property.",
                    keyPrefix, null, 0, null, null, null, null);
        } else if (value == null) {
            flatParams = new LinkedList<>();
            flatParams.add(new Parameter(keyPrefix, ""));
        } else {
            flatParams = new LinkedList<>();
            flatParams.add(new Parameter(keyPrefix, value.toString()));
        }

        return flatParams;
    }


    @NonNull
    private String urlEncodePair(@NonNull String k, @NonNull String v)
            throws UnsupportedEncodingException {
        return String.format(Locale.ROOT, "%s=%s", urlEncode(k), urlEncode(v));
    }

    @Nullable
    private String urlEncode(@Nullable String str) throws UnsupportedEncodingException {
        // Preserve original behavior that passing null for an object id will lead
        // to us actually making a request to /v1/foo/null
        if (str == null) {
            return null;
        } else {
            return URLEncoder.encode(str, CHARSET);
        }
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(method, mUrl, params, options);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj) ||
                (obj instanceof StripeRequest && typedEquals((StripeRequest) obj));
    }

    private boolean typedEquals(@NonNull StripeRequest request) {
        return ObjectUtils.equals(method, request.method) &&
                ObjectUtils.equals(mUrl, request.mUrl) &&
                ObjectUtils.equals(params, request.params) &&
                ObjectUtils.equals(options, request.options);
    }

    enum Method {
        GET("GET"),
        POST("POST"),
        DELETE("DELETE");

        @NonNull final String code;

        Method(@NonNull String code) {
            this.code = code;
        }
    }

    private static final class Parameter {
        @NonNull private final String key;
        @NonNull private final String value;

        Parameter(@NonNull String key, @NonNull String value) {
            this.key = key;
            this.value = value;
        }
    }
}
