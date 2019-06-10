package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a JSON model used in the Stripe Api.
 */
public abstract class StripeJsonModel {

    @NonNull
    public abstract Map<String, Object> toMap();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(@Nullable Object obj);

    static void putStripeJsonModelMapIfNotNull(
            @NonNull Map<String, Object> upperLevelMap,
            @NonNull @Size(min = 1) String key,
            @Nullable StripeJsonModel jsonModel) {
        if (jsonModel == null) {
            return;
        }
        upperLevelMap.put(key, jsonModel.toMap());
    }

    static void putStripeJsonModelListIfNotNull(
            @NonNull Map<String, Object> upperLevelMap,
            @NonNull @Size(min = 1) String key,
            @NonNull List<? extends StripeJsonModel> models) {
        final List<Map<String, Object>> mapList = new ArrayList<>();
        for (int i = 0; i < models.size(); i++) {
            mapList.add(models.get(i).toMap());
        }
        upperLevelMap.put(key, mapList);
    }
}
