package com.stripe.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.exception.InvalidRequestException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A class representing a request to a Stripe-owned service.
 */
abstract class StripeRequest {
    static final String HEADER_USER_AGENT = "User-Agent";

    static final String CHARSET = "UTF-8";

    static final String DEFAULT_USER_AGENT =
            String.format(Locale.ROOT, "Stripe/v1 %s", Stripe.VERSION);

    @NonNull final Method method;
    @Nullable final Map<String, ?> params;

    @NonNull private final String mUrl;
    @NonNull private final String mMimeType;

    StripeRequest(@NonNull Method method,
                  @NonNull String url,
                  @Nullable Map<String, ?> params,
                  @NonNull String mimeType) {
        this.method = method;
        this.mUrl = url;
        this.params = params != null ? compactParams(params) : null;
        mMimeType = mimeType;
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
    String getBaseUrl() {
        return mUrl;
    }

    @NonNull
    String getContentType() {
        return String.format(Locale.ROOT, "%s; charset=%s", mMimeType, CHARSET);
    }

    @NonNull
    final Map<String, String> getHeaders() {
        final Map<String, String> headers = createHeaders();
        headers.put(StripeRequest.HEADER_USER_AGENT, getUserAgent());
        return headers;
    }

    @NonNull
    abstract Map<String, String> createHeaders();

    @NonNull
    abstract String getUserAgent();

    @NonNull
    abstract byte[] getOutputBytes() throws UnsupportedEncodingException, InvalidRequestException;

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
            //noinspection unchecked
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

    /**
     * Copy the {@param params} map and recursively remove null and empty values. The Stripe API
     * requires that parameters with null values are removed from requests.
     *
     * @param params a {@link Map} from which to remove the keys that have {@code null} values
     */
    @SuppressWarnings("unchecked")
    @NonNull
    private static Map<String, Object> compactParams(@NonNull final Map<String, ?> params) {
        final Map<String, Object> compactParams = new HashMap<>(params);

        // Remove all null values; they cause validation errors
        for (String key : new HashSet<>(compactParams.keySet())) {
            if (compactParams.get(key) == null) {
                compactParams.remove(key);
            }

            if (compactParams.get(key) instanceof CharSequence) {
                final CharSequence sequence = (CharSequence) compactParams.get(key);
                if (StripeTextUtils.isEmpty(sequence)) {
                    compactParams.remove(key);
                }
            }

            if (compactParams.get(key) instanceof Map) {
                final Map<String, Object> nestedMap =
                        (Map<String, Object>) compactParams.get(key);
                if (nestedMap != null) {
                    final Map<String, Object> compactNestedMap = compactParams(nestedMap);
                    compactParams.put(key, compactNestedMap);
                }
            }
        }

        return compactParams;
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

    int getBaseHashCode() {
        return Objects.hash(method, mUrl, params);
    }

    boolean typedEquals(@NonNull StripeRequest request) {
        return Objects.equals(method, request.method) &&
                Objects.equals(mUrl, request.mUrl) &&
                Objects.equals(params, request.params);
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
