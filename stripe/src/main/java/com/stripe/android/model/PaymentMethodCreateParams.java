package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.Stripe;

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
public class PaymentMethodCreateParams {

    private static final String FIELD_BILLING_DETAILS = "billing_details";
    private static final String FIELD_CARD = "card";
    private static final String FIELD_IDEAL = "ideal";
    private static final String FIELD_TYPE = "type";

    @NonNull private final Type type;
    @Nullable private final PaymentMethodCreateParams.Card card;
    @Nullable private final PaymentMethodCreateParams.Ideal ideal;
    @Nullable private final PaymentMethod.BillingDetails billingDetails;

    @NonNull
    public static PaymentMethodCreateParams create(
            @NonNull PaymentMethodCreateParams.Card card,
            @Nullable PaymentMethod.BillingDetails billingDetails) {
        return new PaymentMethodCreateParams(card, billingDetails);
    }

    @NonNull
    public static PaymentMethodCreateParams create(
            @NonNull PaymentMethodCreateParams.Ideal ideal,
            @Nullable PaymentMethod.BillingDetails billingDetails) {
        return new PaymentMethodCreateParams(ideal, billingDetails);
    }

    private PaymentMethodCreateParams(@NonNull PaymentMethodCreateParams.Card card,
                                      @Nullable PaymentMethod.BillingDetails billingDetails) {
        this.type = Type.Card;
        this.card = card;
        this.ideal = null;
        this.billingDetails = billingDetails;
    }

    private PaymentMethodCreateParams(@NonNull PaymentMethodCreateParams.Ideal ideal,
                                      @Nullable PaymentMethod.BillingDetails billingDetails) {
        this.type = Type.Ideal;
        this.card = null;
        this.ideal = ideal;
        this.billingDetails = billingDetails;
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

        @Nullable private final String mNumber;
        @Nullable private final Integer mExpiryMonth;
        @Nullable private final Integer mExpiryYear;
        @Nullable private final String mCvc;

        private Card(@NonNull Card.Builder builder) {
            this.mNumber = builder.mNumber;
            this.mExpiryMonth = builder.mExpiryMonth;
            this.mExpiryYear = builder.mExpiryYear;
            this.mCvc = builder.mCvc;
        }

        @NonNull
        public Map<String, Object> toMap() {
            final AbstractMap<String, Object> map = new HashMap<>();
            map.put(FIELD_NUMBER, mNumber);
            map.put(FIELD_EXP_MONTH, mExpiryMonth);
            map.put(FIELD_EXP_YEAR, mExpiryYear);
            map.put(FIELD_CVC, mCvc);
            return map;
        }

        public static final class Builder {
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

        public static final class Builder {
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
