package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * Model of the "data" object inside a {@link Customer} "source" object.
 */
public final class CustomerSource extends StripeModel implements StripePaymentSource {

    @NonNull private final StripePaymentSource mStripePaymentSource;

    private CustomerSource(@NonNull StripePaymentSource paymentSource) {
        mStripePaymentSource = paymentSource;
    }

    @NonNull
    public StripePaymentSource getStripePaymentSource() {
        return mStripePaymentSource;
    }

    @Override
    @Nullable
    public String getId() {
        return mStripePaymentSource.getId();
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
        final Source paymentAsSource = asSource();
        final Card paymentAsCard = asCard();
        if (paymentAsSource != null && Source.CARD.equals(paymentAsSource.getType())) {
            final SourceCardData cardData = (SourceCardData) paymentAsSource.getSourceTypeModel();
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

        final String objectString = optString(jsonObject, "object");
        final StripePaymentSource sourceObject;
        if (Card.VALUE_CARD.equals(objectString)) {
            sourceObject = Card.fromJson(jsonObject);
        } else if (Source.VALUE_SOURCE.equals(objectString)) {
            sourceObject = Source.fromJson(jsonObject);
        } else {
            sourceObject = null;
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

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof CustomerSource && typedEquals((CustomerSource) obj));
    }

    private boolean typedEquals(@NonNull CustomerSource customerSource) {
        return ObjectUtils.equals(mStripePaymentSource, customerSource.mStripePaymentSource);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mStripePaymentSource);
    }
}
