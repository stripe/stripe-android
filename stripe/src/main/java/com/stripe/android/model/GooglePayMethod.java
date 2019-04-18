package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

public class GooglePayMethod extends StripeJsonModel implements StripePaymentSource {

    public static final String VALUE_GOOGLE_PAY = "google_pay";

    private static final String FIELD_OBJECT = "object";

    @NonNull
    @Override
    public JSONObject toJson() {
        final JSONObject object = new JSONObject();
        putStringIfNotNull(object, FIELD_OBJECT, VALUE_GOOGLE_PAY);
        return object;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final AbstractMap<String, Object> map = new HashMap<>();
        map.put(FIELD_OBJECT, VALUE_GOOGLE_PAY);
        return map;
    }

    /**
     * @return a constant string to mark the specific google pay method
     */
    @Nullable
    @Override
    public String getId() {
        return VALUE_GOOGLE_PAY;
    }
}
