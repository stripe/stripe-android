package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A set of JSON parsing utility functions.
 */
public class StripeJsonUtils {

    private static final String EMPTY = "";
    private static final String NULL = "null";

    /**
     * Calls through to {@link JSONObject#optInt(String)} only in the case that the
     * key exists. This returns {@code null} if the key is not in the object.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or {@code null} if the key is not present
     */
    @Nullable
    static Boolean optBoolean(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String fieldName) {
        if (!jsonObject.has(fieldName)) {
            return null;
        }
        return jsonObject.optBoolean(fieldName);
    }

    /**
     * Calls through to {@link JSONObject#optInt(String)} only in the case that the
     * key exists. This returns {@code null} if the key is not in the object.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or {@code null} if the key is not present
     */
    @Nullable
    static Integer optInteger(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String fieldName) {
        if (!jsonObject.has(fieldName)) {
            return null;
        }
        return jsonObject.optInt(fieldName);
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
    static Long optLong(
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
    static String optCountryCode(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String fieldName) {
        final String value = nullIfNullOrEmpty(jsonObject.optString(fieldName));
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
    static String optCurrency(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String fieldName) {
        final String value = nullIfNullOrEmpty(jsonObject.optString(fieldName));
        if (value != null && value.length() == 3) {
            return value;
        }

        return null;
    }

    /**
     * Calls through to {@link JSONObject#optJSONObject(String)} and then
     * uses {@link #jsonObjectToMap(JSONObject)} on the result.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or {@code null} if the key is not present
     */
    @Nullable
    static Map<String, Object> optMap(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String fieldName) {
        final JSONObject foundObject = jsonObject.optJSONObject(fieldName);
        if (foundObject == null) {
            return null;
        }

        return jsonObjectToMap(foundObject);
    }

    /**
     * Calls through to {@link JSONObject#optJSONObject(String)} and then
     * uses {@link #jsonObjectToStringMap(JSONObject)} on the result.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or {@code null} if the key is not present
     */
    @Nullable
    static Map<String, String> optHash(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String fieldName) {
        final JSONObject foundObject = jsonObject.optJSONObject(fieldName);
        if (foundObject == null) {
            return null;
        }

        return jsonObjectToStringMap(foundObject);
    }

    /**
     * Convert a {@link JSONObject} to a {@link Map}.
     *
     * @param jsonObject a {@link JSONObject} to be converted
     * @return a {@link Map} representing the input, or {@code null} if the input is {@code null}
     */
    @Nullable
    static Map<String, Object> jsonObjectToMap(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        final AbstractMap<String, Object> map = new HashMap<>();
        final Iterator<String> keyIterator = jsonObject.keys();
        while (keyIterator.hasNext()) {
            final String key = keyIterator.next();
            final Object value = jsonObject.opt(key);
            if (NULL.equals(value) || value == null) {
                continue;
            }

            if (value instanceof JSONObject) {
                map.put(key, jsonObjectToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.put(key, jsonArrayToList((JSONArray) value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * Convert a {@link JSONObject} to a flat, string-keyed and string-valued map. All values
     * are recorded as strings.
     *
     * @param jsonObject the input {@link JSONObject} to be converted
     * @return a {@link Map} representing the input, or {@code null} if the input is {@code null}
     */
    @Nullable
    static Map<String, String> jsonObjectToStringMap(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        final Map<String, String> map = new HashMap<>();
        final Iterator<String> keyIterator = jsonObject.keys();
        while (keyIterator.hasNext()) {
            final String key = keyIterator.next();
            final Object value = jsonObject.opt(key);
            if (NULL.equals(value) || value == null) {
                continue;
            }

            map.put(key, value.toString());
        }

        return map;
    }

    /**
     * Converts a {@link JSONArray} to a {@link List}.
     *
     * @param jsonArray a {@link JSONArray} to be converted
     * @return a {@link List} representing the input, or {@code null} if said input is {@code null}
     */
    @Nullable
    static List<Object> jsonArrayToList(@Nullable JSONArray jsonArray) {
        if (jsonArray == null) {
            return null;
        }

        final List<Object> objectList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                final Object ob = jsonArray.get(i);
                if (ob instanceof JSONArray) {
                    objectList.add(jsonArrayToList((JSONArray) ob));
                } else if (ob instanceof JSONObject) {
                    final Map<String, Object> objectMap = jsonObjectToMap((JSONObject) ob);
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
    static String nullIfNullOrEmpty(@Nullable String possibleNull) {
        return NULL.equals(possibleNull) || EMPTY.equals(possibleNull)
                ? null
                : possibleNull;
    }
}
