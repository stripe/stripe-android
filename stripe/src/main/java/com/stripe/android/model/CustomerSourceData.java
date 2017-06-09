package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.util.StripeJsonUtils.optString;

/**
 * Model of the "data" object inside a {@link Customer} "source" object.
 */
public class CustomerSourceData extends StripeJsonModel {

    private StripePaymentSource mStripePaymentSource;

    private CustomerSourceData(StripePaymentSource paymentSource) {
        mStripePaymentSource = paymentSource;
    }

    @Nullable
    public StripePaymentSource getStripePaymentSource() {
        return mStripePaymentSource;
    }

    @Nullable
    public Source asSource() {
        if (mStripePaymentSource instanceof Source) {
            return (Source) mStripePaymentSource;
        }
        return null;
    }

    @Nullable
    public Card asCard() {
        if (mStripePaymentSource instanceof Card) {
            return (Card) mStripePaymentSource;
        }
        return null;
    }

    @Nullable
    public static CustomerSourceData fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        String objectString = optString(jsonObject, "object");
        StripePaymentSource sourceObject = null;
        if (Card.VALUE_CARD.equals(objectString)) {
            sourceObject = Card.fromJson(jsonObject);
        } else if (Source.VALUE_SOURCE.equals(objectString)) {
            sourceObject = Source.fromJson(jsonObject);
        }

        if (sourceObject == null) {
            return null;
        } else {
            return new CustomerSourceData(sourceObject);
        }
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        if (mStripePaymentSource instanceof Source) {
            return ((Source) mStripePaymentSource).toMap();
        } else if (mStripePaymentSource instanceof Card) {
            return ((Card) mStripePaymentSource).toMap();
        }
        return new HashMap<>();
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        if (mStripePaymentSource instanceof Source) {
            return ((Source) mStripePaymentSource).toJson();
        } else if (mStripePaymentSource instanceof Card) {
            return ((Card) mStripePaymentSource).toJson();
        }
        return new JSONObject();
    }
}
