package com.stripe.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.utils.ObjectUtils;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class representing a fingerprint request.
 */
final class FingerprintRequest extends StripeRequest {
    private static final String MIME_TYPE = "application/json";
    private static final String URL = "https://m.stripe.com/4";

    @NonNull private final String guid;

    FingerprintRequest(@NonNull Map<String, Object> params, @NonNull String guid) {
        super(Method.POST, URL, params, MIME_TYPE);
        this.guid = guid;
    }

    @NonNull
    @Override
    Map<String, String> createHeaders() {
        final Map<String, String> props = new HashMap<>();
        props.put("Cookie", "m=" + guid);
        return props;
    }

    @NonNull
    @Override
    String getUserAgent() {
        return DEFAULT_USER_AGENT;
    }

    @NonNull
    @Override
    byte[] getOutputBytes() throws UnsupportedEncodingException, InvalidRequestException {
        final JSONObject jsonData = mapToJsonObject(params);
        if (jsonData == null) {
            throw new InvalidRequestException("Unable to create JSON data from " +
                    "parameters. Please contact support@stripe.com for assistance.",
                    null, null, 0, null, null, null, null);
        }
        return jsonData.toString().getBytes(CHARSET);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(getBaseHashCode(), guid);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj) || (obj instanceof FingerprintRequest &&
                typedEquals((FingerprintRequest) obj));
    }

    private boolean typedEquals(@NonNull FingerprintRequest obj) {
        return super.typedEquals(obj) && ObjectUtils.equals(guid, obj.guid);
    }

    /**
     * Converts a string-keyed {@link Map} into a {@link JSONObject}. This will cause a
     * {@link ClassCastException} if any sub-map has keys that are not {@link String Strings}.
     *
     * @param mapObject the {@link Map} that you'd like in JSON form
     * @return a {@link JSONObject} representing the input map, or {@code null} if the input
     * object is {@code null}
     */
    @Nullable
    private static JSONObject mapToJsonObject(@Nullable Map<String, ?> mapObject) {
        if (mapObject == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        for (String key : mapObject.keySet()) {
            Object value = mapObject.get(key);
            if (value == null) {
                continue;
            }

            try {
                if (value instanceof Map<?, ?>) {
                    try {
                        //noinspection unchecked
                        Map<String, Object> mapValue = (Map<String, Object>) value;
                        jsonObject.put(key, mapToJsonObject(mapValue));
                    } catch (ClassCastException classCastException) {
                        // don't include the item in the JSONObject if the keys are not Strings
                    }
                } else if (value instanceof List<?>) {
                    //noinspection unchecked
                    jsonObject.put(key, listToJsonArray((List<Object>) value));
                } else if (value instanceof Number || value instanceof Boolean) {
                    jsonObject.put(key, value);
                } else {
                    jsonObject.put(key, value.toString());
                }
            } catch (JSONException jsonException) {
                // Simply skip this value
            }
        }
        return jsonObject;
    }

    /**
     * Converts a {@link List} into a {@link JSONArray}. A {@link ClassCastException} will be
     * thrown if any object in the list (or any sub-list or sub-map) is a {@link Map} whose keys
     * are not {@link String Strings}.
     *
     * @param values a {@link List} of values to be put in a {@link JSONArray}
     * @return a {@link JSONArray}, or {@code null} if the input was {@code null}
     */
    @Nullable
    private static JSONArray listToJsonArray(@Nullable List<?> values) {
        if (values == null) {
            return null;
        }

        final JSONArray jsonArray = new JSONArray();
        for (Object object : values) {
            if (object instanceof Map<?, ?>) {
                // We are ignoring type erasure here and crashing on bad input.
                // Now that this method is not public, we have more control on what is
                // passed to it.
                //noinspection unchecked
                final Map<String, Object> mapObject = (Map<String, Object>) object;
                jsonArray.put(mapToJsonObject(mapObject));
            } else if (object instanceof List<?>) {
                jsonArray.put(listToJsonArray((List) object));
            } else if (object instanceof Number || object instanceof Boolean) {
                jsonArray.put(object);
            } else {
                jsonArray.put(object.toString());
            }
        }
        return jsonArray;
    }
}
