package com.stripe.android.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;
import android.widget.LinearLayout;

import com.stripe.android.Stripe;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A set of JSON parsing utility functions.
 */
public class StripeJsonUtils {

    static final String EMPTY = "";
    static final String NULL = "null";

    /**
     * Calls through to {@link JSONObject#getString(String)} while safely
     * converting the raw string "null" and the empty string to {@code null}.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field
     * @throws JSONException if the field does not exist
     */
    @Nullable
    public static String getString(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String fieldName) throws JSONException {
        return nullIfNullOrEmpty(jsonObject.getString(fieldName));
    }

    /**
     * Calls through to {@link JSONObject#optLong(String)} only in the case that the
     * key exists. This returns {@code null} if the key is not in the object.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or {@code null} if the key is not present
     */
    @Nullable
    public static Long optLong(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String fieldName) {
        if (!jsonObject.has(fieldName)) {
            return null;
        }
        return jsonObject.optLong(fieldName);
    }

    /**
     * Calls through to {@link JSONObject#optString(String)} while safely
     * converting the raw string "null" and the empty string to {@code null}. Will not throw
     * an exception if the field isn't found.
     *
     * @param jsonObject the input object
     * @param fieldName the optional field name
     * @return the value stored in the field, or {@code null} if the field isn't present
     */
    @Nullable
    public static String optString(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String fieldName) {
        return nullIfNullOrEmpty(jsonObject.optString(fieldName));
    }

    /**
     * Calls through to {@link JSONObject#optString(String)} while safely converting
     * the raw string "null" and the empty string to {@code null}, along with any value that isn't
     * a two-character string.
     * @param jsonObject the object from which to retrieve the country code
     * @param fieldName the name of the field in which the country code is stored
     * @return a two-letter country code if one is found, or {@code null}
     */
    @Nullable
    @Size(2)
    public static String optCountryCode(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String fieldName) {
        String value = nullIfNullOrEmpty(jsonObject.optString(fieldName));
        if (value != null && value.length() == 2) {
            return value;
        }
        return null;
    }

    /**
     * Calls through to {@link JSONObject#optString(String)} while safely converting
     * the raw string "null" and the empty string to {@code null}, along with any value that isn't
     * a three-character string.
     * @param jsonObject the object from which to retrieve the currency code
     * @param fieldName the name of the field in which the currency code is stored
     * @return a three-letter currency code if one is found, or {@code null}
     */
    @Nullable
    @Size(3)
    public static String optCurrency(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String fieldName) {
        String value = nullIfNullOrEmpty(jsonObject.optString(fieldName));
        if (value != null && value.length() == 3) {
            return value;
        }
        return null;
    }

    @Nullable
    public static Map<String, Object> jsonObjectToMap(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        while(jsonObject.keys().hasNext()) {
            String key = jsonObject.keys().next();
            Object value = jsonObject.opt(key);
            if (NULL.equals(value)) {
                continue;
            }

            if (value instanceof JSONObject) {
                map.put(key, jsonObjectToMap(jsonObject));
            } else if (value instanceof JSONArray) {
                map.put(key, jsonArrayToList((JSONArray) value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    @NonNull
    public static List<Object> jsonArrayToList(@NonNull JSONArray jsonArray) {
        List<Object> objectList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                Object ob = jsonArray.get(i);
                if (ob instanceof JSONArray) {
                    objectList.add(jsonArrayToList((JSONArray) ob));
                } else if (ob instanceof JSONObject) {
                    Map<String, Object> objectMap = jsonObjectToMap((JSONObject) ob);
                    if (objectMap != null) {
                        objectList.add(objectMap);
                    }
                } else {
                    if (NULL.equals(ob)) {
                        continue;
                    }
                    objectList.add(ob);
                }
            } catch (JSONException ignored) {
                // Nothing to do in this case.
            }
        }
        return objectList;
    }

    @Nullable
    public static JSONObject putMapAsJson(@Nullable Map<String, Object> mapObject) {
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
                        Map<String, Object> mapValue = (Map<String, Object>) value;
                        jsonObject.put(key, putMapAsJson(mapValue));
                    } catch (ClassCastException classCastException) {
                        // We don't include the item in the JSONObject if the keys are not Strings.
                    }
                } else if (value instanceof List<?>) {
                    jsonObject.put(key, putListAsJson((List<Object>) value));
                } else {
                    jsonObject.put(key, value.toString());
                }
            } catch (JSONException jsonException) {
                continue;
            }
        }
        return jsonObject;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static JSONArray putListAsJson(@Nullable List<Object> values) {
        if (values == null) {
            return null;
        }

        JSONArray jsonArray = new JSONArray();
        for (Object object : values) {
            if (object instanceof Map<?, ?>) {
                try {
                    Map<String, Object> mapObject = (Map<String, Object>) object;
                    jsonArray.put(putMapAsJson(mapObject));
                } catch (ClassCastException classCastException) {
                    // We don't include the item in the array if the keys are not Strings.
                }
            } else if (object instanceof List<?>) {
                jsonArray.put(putListAsJson((List<Object>) object));
            }
            jsonArray.put(object.toString());
        }
        return jsonArray;
    }

    /**
     * Util function for putting a string value into a {@link JSONObject} if that
     * string is not null or empty. This ignores any {@link JSONException} that may be thrown
     * due to insertion.
     *
     * @param jsonObject the {@link JSONObject} into which to put the field
     * @param fieldName the field name
     * @param value the potential field value
     */
    public static void putStringIfNotNull(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String fieldName,
            @Nullable String value) {
        if (!StripeTextUtils.isBlank(value)) {
            try {
                jsonObject.put(fieldName, value);
            } catch (JSONException ignored) { }
        }
    }

    @Nullable
    @VisibleForTesting
    static String nullIfNullOrEmpty(@Nullable String possibleNull) {
        return NULL.equals(possibleNull) || EMPTY.equals(possibleNull)
                ? null
                : possibleNull;
    }
}
