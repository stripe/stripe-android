package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Represents a JSON model used in the Stripe Api.
 */
public abstract class StripeJsonModel {

    @NonNull
    public abstract Map<String, Object> toMap();

    @NonNull
    public abstract JSONObject toJson();

    @Override
    public String toString() {
        return this.toJson().toString();
    }

    static void putStripeJsonModelMapIfNotNull(
        @NonNull Map<String, Object> upperLevelMap,
        @NonNull @Size(min = 1) String key,
        @Nullable StripeJsonModel jsonModel){
        if (jsonModel == null) {
            return;
        }
        upperLevelMap.put(key, jsonModel.toMap());
    }

    static void putStripeJsonModelIfNotNull(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String key,
            @Nullable StripeJsonModel jsonModel) {
        if (jsonModel == null) {
            return;
        }

        try {
            jsonObject.put(key, jsonModel.toJson());
        } catch (JSONException ignored) {}
    }

}
