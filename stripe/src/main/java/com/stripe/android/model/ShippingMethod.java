package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.putLongIfNotNull;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

/**
 * Model representing a shipping method in the Android SDK
 */
public class ShippingMethod extends StripeJsonModel {

    private static final String FIELD_AMOUNT = "amount";
    /*ISO Currency Code*/
    private static final String FIELD_CURRENCY_CODE = "currency_code";
    private static final String FIELD_DETAIL = "detail";
    private static final String FIELD_IDENTIFIER = "identifier";
    private static final String FIELD_LABEL = "label";

    private long mAmount;
    private String mCurrencyCode;
    private @Nullable String mDetail;
    private String mLabel;
    private String mIdentifier;

    public ShippingMethod(String label, String identifier, long amount, String currencyCode) {
        this(label, identifier, null, amount, currencyCode);
    }

    public ShippingMethod(String label, String identifier, @Nullable String detail, long amount, String currencyCode) {
        mLabel = label;
        mIdentifier = identifier;
        mDetail = detail;
        mAmount = amount;
        mCurrencyCode = currencyCode;
    }

    /**
     * @return the currency that the specified amount will be rendered in.
     */
    public Currency getCurrency() {
        return Currency.getInstance(mCurrencyCode);
    }

    /**
     * @return The cost in minor unit of the currency provided in the
     * {@link com.stripe.android.PaymentConfiguration}. For example, cents in the USA and yen in
     * Japan.
     */
    public long getAmount() {
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
        putLongIfNotNull(jsonObject, FIELD_AMOUNT, mAmount);
        putStringIfNotNull(jsonObject, FIELD_CURRENCY_CODE, mCurrencyCode);
        putStringIfNotNull(jsonObject, FIELD_DETAIL, mDetail);
        putStringIfNotNull(jsonObject, FIELD_IDENTIFIER, mIdentifier);
        putStringIfNotNull(jsonObject, FIELD_LABEL, mLabel);
        return jsonObject;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_AMOUNT, mAmount);
        map.put(FIELD_CURRENCY_CODE, mCurrencyCode);
        map.put(FIELD_DETAIL, mDetail);
        map.put(FIELD_IDENTIFIER, mIdentifier);
        map.put(FIELD_LABEL, mLabel);
        return map;
    }
}
