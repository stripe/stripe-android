package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * Model of the "data" object inside a {@link Customer} "source" object.
 */
public class CustomerSource extends StripeJsonModel implements StripePaymentSource {

    private StripePaymentSource mStripePaymentSource;

    private CustomerSource(StripePaymentSource paymentSource) {
        mStripePaymentSource = paymentSource;
    }

    @Nullable
    public StripePaymentSource getStripePaymentSource() {
        return mStripePaymentSource;
    }

    @Override
    @Nullable
    public String getId() {
        return mStripePaymentSource == null ? null : mStripePaymentSource.getId();
    }

    @Nullable
    public Source asSource() {
        if (mStripePaymentSource instanceof Source) {
            return (Source) mStripePaymentSource;
        }
        return null;
    }

    @Nullable
    public String getTokenizationMethod() {
        Source paymentAsSource = asSource();
        Card paymentAsCard = asCard();
        if (paymentAsSource != null && paymentAsSource.getType().equals(Source.CARD)) {
            SourceCardData cardData = (SourceCardData) paymentAsSource.getSourceTypeModel();
            if (cardData != null) {
                return cardData.getTokenizationMethod();
            }
        } else if (paymentAsCard != null) {
            return paymentAsCard.getTokenizationMethod();
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

    @NonNull
    @Source.SourceType
    public String getSourceType() {
        if (mStripePaymentSource instanceof Card) {
            return Source.CARD;
        } else if (mStripePaymentSource instanceof Source) {
            return ((Source) mStripePaymentSource).getType();
        } else {
            return Source.UNKNOWN;
        }
    }

    @Nullable
    public static CustomerSource fromString(@Nullable String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static CustomerSource fromJson(@Nullable JSONObject jsonObject) {
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
            return new CustomerSource(sourceObject);
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
