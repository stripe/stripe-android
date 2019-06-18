package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.Stripe;
import com.stripe.android.utils.ObjectUtils;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Model for PaymentMethod creation parameters.
 * Used by {@link Stripe#createPaymentMethodSynchronous(PaymentMethodCreateParams, String)}
 *
 * See https://stripe.com/docs/api/payment_methods/create for documentation
 *
 * See {@link PaymentMethod} for API object
 */
public final class PaymentMethodCreateParams {

    private static final String FIELD_BILLING_DETAILS = "billing_details";
    private static final String FIELD_CARD = "card";
    private static final String FIELD_IDEAL = "ideal";
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_TYPE = "type";

    @NonNull private final Type type;
    @Nullable private final PaymentMethodCreateParams.Card card;
    @Nullable private final PaymentMethodCreateParams.Ideal ideal;
    @Nullable private final PaymentMethod.BillingDetails billingDetails;
    @Nullable private final Map<String, String> metadata;

    @NonNull
    public static PaymentMethodCreateParams create(
            @NonNull PaymentMethodCreateParams.Card card,
            @Nullable PaymentMethod.BillingDetails billingDetails) {
        return new PaymentMethodCreateParams(card, billingDetails, null);
    }

    @NonNull
    public static PaymentMethodCreateParams create(
            @NonNull PaymentMethodCreateParams.Card card,
            @Nullable PaymentMethod.BillingDetails billingDetails,
            @Nullable Map<String, String> metadata) {
        return new PaymentMethodCreateParams(card, billingDetails, metadata);
    }

    @NonNull
    public static PaymentMethodCreateParams create(
            @NonNull PaymentMethodCreateParams.Ideal ideal,
            @Nullable PaymentMethod.BillingDetails billingDetails) {
        return new PaymentMethodCreateParams(ideal, billingDetails, null);
    }

    @NonNull
    public static PaymentMethodCreateParams create(
            @NonNull PaymentMethodCreateParams.Ideal ideal,
            @Nullable PaymentMethod.BillingDetails billingDetails,
            @Nullable Map<String, String> metadata) {
        return new PaymentMethodCreateParams(ideal, billingDetails, metadata);
    }

    private PaymentMethodCreateParams(@NonNull Card card,
                                      @Nullable PaymentMethod.BillingDetails billingDetails,
                                      @Nullable Map<String, String> metadata) {
        this.type = Type.Card;
        this.card = card;
        this.ideal = null;
        this.billingDetails = billingDetails;
        this.metadata = metadata;
    }

    private PaymentMethodCreateParams(@NonNull PaymentMethodCreateParams.Ideal ideal,
                                      @Nullable PaymentMethod.BillingDetails billingDetails,
                                      @Nullable Map<String, String> metadata) {
        this.type = Type.Ideal;
        this.card = null;
        this.ideal = ideal;
        this.billingDetails = billingDetails;
        this.metadata = metadata;
    }

    @NonNull
    public Map<String, Object> toParamMap() {
        final Map<String, Object> params = new HashMap<>();
        params.put(FIELD_TYPE, type.mCode);

        if (type == Type.Card && card != null) {
            params.put(FIELD_CARD, card.toMap());
        } else if (type == Type.Ideal && ideal != null) {
            params.put(FIELD_IDEAL, ideal.toMap());
        }

        if (billingDetails != null) {
            params.put(FIELD_BILLING_DETAILS, billingDetails.toMap());
        }

        if (metadata != null) {
            params.put(FIELD_METADATA, metadata);
        }

        return params;
    }

    enum Type {
        Card("card"),
        Ideal("ideal");

        @NonNull private final String mCode;

        Type(@NonNull String code) {
            mCode = code;
        }
    }

    public static final class Card {
        private static final String FIELD_NUMBER = "number";
        private static final String FIELD_EXP_MONTH = "exp_month";
        private static final String FIELD_EXP_YEAR = "exp_year";
        private static final String FIELD_CVC = "cvc";
        private static final String FIELD_TOKEN = "token";

        @Nullable private final String mNumber;
        @Nullable private final Integer mExpiryMonth;
        @Nullable private final Integer mExpiryYear;
        @Nullable private final String mCvc;
        @Nullable private final String mToken;

        @NonNull
        public static Card create(@NonNull String token) {
            return new Card(token);
        }

        private Card(@NonNull String token) {
            this.mToken = token;
            this.mNumber = null;
            this.mExpiryMonth = null;
            this.mExpiryYear = null;
            this.mCvc = null;
        }

        private Card(@NonNull Card.Builder builder) {
            this.mNumber = builder.mNumber;
            this.mExpiryMonth = builder.mExpiryMonth;
            this.mExpiryYear = builder.mExpiryYear;
            this.mCvc = builder.mCvc;
            this.mToken = null;
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(mNumber, mExpiryMonth, mExpiryYear, mCvc, mToken);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj || (obj instanceof Card && typedEquals((Card) obj));
        }

        private boolean typedEquals(@NonNull Card card) {
            return ObjectUtils.equals(mNumber, card.mNumber)
                    && ObjectUtils.equals(mCvc, card.mCvc)
                    && ObjectUtils.equals(mExpiryMonth, card.mExpiryMonth)
                    && ObjectUtils.equals(mExpiryYear, card.mExpiryYear)
                    && ObjectUtils.equals(mToken, card.mToken);
        }

        @NonNull
        public Map<String, Object> toMap() {
            final Map<String, Object> map = new HashMap<>();
            if (mNumber != null) {
                map.put(FIELD_NUMBER, mNumber);
            }

            if (mExpiryMonth != null) {
                map.put(FIELD_EXP_MONTH, mExpiryMonth);
            }

            if (mExpiryYear != null) {
                map.put(FIELD_EXP_YEAR, mExpiryYear);
            }

            if (mCvc != null) {
                map.put(FIELD_CVC, mCvc);
            }

            if (mToken != null) {
                map.put(FIELD_TOKEN, mToken);
            }

            return map;
        }

        /**
         * Used to create a {@link Card} object with the user's card details. To create a
         * {@link Card} with a Stripe token (e.g. for Google Pay), use {@link Card#create(String)}.
         */
        public static final class Builder implements ObjectBuilder<Card> {
            @Nullable private String mNumber;
            @Nullable private Integer mExpiryMonth;
            @Nullable private Integer mExpiryYear;
            @Nullable private String mCvc;

            @NonNull
            public Builder setNumber(@Nullable String number) {
                this.mNumber = number;
                return this;
            }

            @NonNull
            public Builder setExpiryMonth(@Nullable Integer expiryMonth) {
                this.mExpiryMonth = expiryMonth;
                return this;
            }

            @NonNull
            public Builder setExpiryYear(@Nullable Integer expiryYear) {
                this.mExpiryYear = expiryYear;
                return this;
            }

            @NonNull
            public Builder setCvc(@Nullable String cvc) {
                this.mCvc = cvc;
                return this;
            }

            @NonNull
            public Card build() {
                return new Card(this);
            }
        }
    }

    public static final class Ideal {
        private static final String FIELD_BANK = "bank";

        @Nullable private final String mBank;

        private Ideal(@NonNull Ideal.Builder builder) {
            this.mBank = builder.mBank;
        }

        @NonNull
        public Map<String, Object> toMap() {
            final AbstractMap<String, Object> map = new HashMap<>();
            map.put(FIELD_BANK, mBank);
            return map;
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(mBank);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj || (obj instanceof Ideal && typedEquals((Ideal) obj));
        }

        private boolean typedEquals(@NonNull Ideal ideal) {
            return ObjectUtils.equals(mBank, ideal.mBank);
        }

        public static final class Builder implements ObjectBuilder<Ideal> {
            @Nullable private String mBank;

            @NonNull
            public Builder setBank(@Nullable String bank) {
                this.mBank = bank;
                return this;
            }

            @NonNull
            public Ideal build() {
                return new Ideal(this);
            }
        }
    }
}
