package com.stripe.android.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

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
    @VisibleForTesting
    static String nullIfNullOrEmpty(@Nullable String possibleNull) {
        return NULL.equals(possibleNull) || EMPTY.equals(possibleNull)
                ? null
                : possibleNull;
    }
}
