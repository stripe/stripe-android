package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Represents a JSON model used in the Stripe Api.
 */
public abstract class StripeModel {

    @NonNull
    public abstract Map<String, Object> toMap();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(@Nullable Object obj);

    @NonNull
    static List<String> jsonArrayToList(@Nullable JSONArray jsonArray) {
        final List<String> list = new ArrayList<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    list.add(jsonArray.getString(i));
                } catch (JSONException ignored) {
                }
            }
        }

        return list;
    }
}
