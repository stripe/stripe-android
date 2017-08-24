package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.putDoubleIfNotNull;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

/**
 * Model representing a shipping method in the Android SDK
 */
public class ShippingMethod extends StripeJsonModel {

    private static final String FIELD_LABEL = "label";
    private static final String FIELD_IDENTIFIER = "identifier";
    private static final String FIELD_DETAIL = "detail";
    private static final String FIELD_AMOUNT = "amount";

    private Double mAmount;
    private String mLabel;
    private String mIdentifier;
    private @Nullable String mDetail;

    public ShippingMethod(String label, String identifier, double amount) {
        this.mLabel = label;
        this.mIdentifier = identifier;
        this.mAmount = amount;
    }

    public ShippingMethod(String label,  String identifier, @Nullable String detail, double amount) {
        this.mLabel = label;
        this.mIdentifier = identifier;
        this.mDetail = detail;
        this.mAmount = amount;
    }

    /**
     * @return The cost in major unit of the currency provided in the
     * {@link com.stripe.android.PaymentConfiguration}. For example, dollars in the USA and yen in
     * Japan.
     */
    public double getAmount() {
        return mAmount;
    }

    /**
     * @return Human friendly label specifying the shipping method that can be shown in the UI.
     */
    public String getLabel() {
        return mLabel;
    }

    /**
     * @return Human friendly information such as estimated shipping times that can be shown in the UI
     */
    @Nullable
    public String getDetail() {
        return mDetail;
    }

    /**
     * @return Identifier for the shipping method.
     */
    public String getIdentifier() {
        return mIdentifier;
    }


    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        putStringIfNotNull(jsonObject, FIELD_LABEL, mLabel);
        putStringIfNotNull(jsonObject, FIELD_IDENTIFIER, mIdentifier);
        putStringIfNotNull(jsonObject, FIELD_DETAIL, mDetail);
        putDoubleIfNotNull(jsonObject, FIELD_AMOUNT, mAmount);
        return jsonObject;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_LABEL, mLabel);
        map.put(FIELD_IDENTIFIER, mIdentifier);
        map.put(FIELD_DETAIL, mDetail);
        map.put(FIELD_AMOUNT, mAmount);
        return map;
    }
}
