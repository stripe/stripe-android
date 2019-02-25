package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a JSON model used in the Stripe Api.
 */
public abstract class StripeJsonModel {

    @NonNull
    public abstract Map<String, Object> toMap();

    @NonNull
    public abstract JSONObject toJson();

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

    static void putStripeJsonModelListIfNotNull(
            @NonNull Map<String, Object> upperLevelMap,
            @NonNull @Size(min = 1) String key,
            @Nullable List<? extends StripeJsonModel> jsonModelList) {
        if (jsonModelList == null) {
            return;
        }

        List<Map<String, Object>> mapList = new ArrayList<>();
        for (int i = 0; i < jsonModelList.size(); i++) {
            mapList.add(jsonModelList.get(i).toMap());
        }
        upperLevelMap.put(key, mapList);
    }


    static void putStripeJsonModelListIfNotNull(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String key,
            @Nullable List<? extends StripeJsonModel> jsonModelList) {
        if (jsonModelList == null) {
            return;
        }

        try {
            JSONArray array = new JSONArray();
            for (StripeJsonModel model : jsonModelList) {
                array.put(model.toJson());
            }
            jsonObject.put(key, array);
        } catch (JSONException ignored) {}
    }
}
