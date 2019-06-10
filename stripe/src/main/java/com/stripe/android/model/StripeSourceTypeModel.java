package com.stripe.android.model;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class StripeSourceTypeModel extends StripeModel {
    @NonNull private final Map<String, Object> mAdditionalFields;
    private static final String NULL = "null";

    StripeSourceTypeModel(@NonNull BaseBuilder builder) {
        mAdditionalFields = builder.mAdditionalFields != null ?
                builder.mAdditionalFields : new HashMap<String, Object>();
    }

    @NonNull
    public Map<String, Object> getAdditionalFields() {
        return mAdditionalFields;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        return new HashMap<>(mAdditionalFields);
    }

    /**
     * Convert a {@link JSONObject} to a flat, string-keyed map.
     *
     * @param jsonObject the input {@link JSONObject} to be converted
     * @param omitKeys a set of keys to be omitted from the map
     * @return a {@link Map} representing the input, or {@code null} if the input is {@code null}
     * or if the output would be an empty map.
     */
    @Nullable
    static Map<String, Object> jsonObjectToMapWithoutKeys(
            @Nullable JSONObject jsonObject,
            @Nullable Set<String> omitKeys) {
        if (jsonObject == null) {
            return null;
        }

        final Set<String> keysToOmit = omitKeys == null ? new HashSet<String>() : omitKeys;
        final Map<String, Object> map = new HashMap<>();
        final Iterator<String> keyIterator = jsonObject.keys();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            Object value = jsonObject.opt(key);
            if (NULL.equals(value) || value == null || keysToOmit.contains(key)) {
                continue;
            }

            map.put(key, value);
        }

        if (map.isEmpty()) {
            return null;
        } else {
            return map;
        }
    }

    /**
     * Put the key-value pairs from the map into the JSON Object. Note: this does
     * not protect against overwriting original values in the JSON. This method assumes
     * a 1-level map.
     *
     * @param jsonObject a {@link JSONObject} into which new values are being written
     * @param additionalFields a {@link Map} of key-value pairs to add to the object.
     */
    static void putAdditionalFieldsIntoJsonObject(
            @Nullable JSONObject jsonObject,
            @Nullable Map<String, Object> additionalFields) {
        if (jsonObject == null || additionalFields == null || additionalFields.isEmpty()) {
            return;
        }

        for (String key : additionalFields.keySet()) {
            try {
                if (additionalFields.get(key) != null) {
                    jsonObject.put(key, additionalFields.get(key));
                }
            } catch (JSONException ignored) { }
        }
    }

    /**
     * Put the key-value pairs from the second map into the first map. Note: this does
     * not protect against overwriting original values. This method assumes
     * a 1-level map.
     *
     * @param map a {@link Map} into which new values are being written
     * @param additionalFields a {@link Map} of key-value pairs to add to the object.
     */
    static void putAdditionalFieldsIntoMap(
            @Nullable Map<String, Object> map,
            @Nullable Map<String, Object> additionalFields) {
        if (map == null || additionalFields == null || additionalFields.isEmpty()) {
            return;
        }

        map.putAll(additionalFields);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof StripeSourceTypeModel
                && typedEquals((StripeSourceTypeModel) obj));
    }

    @CallSuper
    boolean typedEquals(@NonNull StripeSourceTypeModel model) {
        return ObjectUtils.equals(mAdditionalFields, model.mAdditionalFields);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mAdditionalFields);
    }

    abstract static class BaseBuilder {
        @Nullable private Map<String, Object> mAdditionalFields;

        @NonNull
        BaseBuilder setAdditionalFields(@NonNull Map<String, Object> additionalFields) {
            mAdditionalFields = additionalFields;
            return this;
        }
    }
}
