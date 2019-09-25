package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.Stripe;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model for PaymentMethod creation parameters.
 *
 * Used by {@link Stripe#createPaymentMethodSynchronous(PaymentMethodCreateParams)}
 *
 * See <a href="https://stripe.com/docs/api/payment_methods/create">Create a PaymentMethod</a>.
 *
 * See {@link PaymentMethod} for API object.
 */
public final class PaymentMethodCreateParams implements StripeParamsModel {

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_CARD = "card";
    private static final String FIELD_FPX = "fpx";
    private static final String FIELD_IDEAL = "ideal";
    private static final String FIELD_SEPA_DEBIT = "sepa_debit";

    private static final String FIELD_BILLING_DETAILS = "billing_details";
    private static final String FIELD_METADATA = "metadata";

    @NonNull private final Type type;
    @Nullable private final PaymentMethodCreateParams.Card card;
    @Nullable private final PaymentMethodCreateParams.Ideal ideal;
    @Nullable private final PaymentMethodCreateParams.Fpx fpx;
    @Nullable private final PaymentMethodCreateParams.SepaDebit sepaDebit;
    @Nullable private final PaymentMethod.BillingDetails billingDetails;
    @Nullable private final Map<String, String> metadata;

    @NonNull
    public static PaymentMethodCreateParams create(@NonNull PaymentMethodCreateParams.Card card) {
        return create(card, null);
    }

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
    public static PaymentMethodCreateParams create(@NonNull PaymentMethodCreateParams.Ideal ideal) {
        return new PaymentMethodCreateParams(ideal, null, null);
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

    @NonNull
    public static PaymentMethodCreateParams create(@NonNull PaymentMethodCreateParams.Fpx fpx) {
        return create(fpx, null);
    }


    @NonNull
    public static PaymentMethodCreateParams create(
            @NonNull PaymentMethodCreateParams.Fpx fpx,
            @Nullable PaymentMethod.BillingDetails billingDetails) {
        return create(fpx, billingDetails, null);
    }


    @NonNull
    public static PaymentMethodCreateParams create(
            @NonNull PaymentMethodCreateParams.Fpx fpx,
            @Nullable PaymentMethod.BillingDetails billingDetails,
            @Nullable Map<String, String> metadata) {
        return new PaymentMethodCreateParams(fpx, billingDetails, metadata);
    }

    // TODO(mshafrir-stripe): make public
    @NonNull
    static PaymentMethodCreateParams create(
            @NonNull PaymentMethodCreateParams.SepaDebit sepaDebit) {
        return create(sepaDebit, null);
    }

    // TODO(mshafrir-stripe): make public
    @NonNull
    private static PaymentMethodCreateParams create(
            @NonNull PaymentMethodCreateParams.SepaDebit sepaDebit,
            @Nullable PaymentMethod.BillingDetails billingDetails) {
        return create(sepaDebit, billingDetails, null);
    }

    // TODO(mshafrir-stripe): make public
    @NonNull
    private static PaymentMethodCreateParams create(
            @NonNull PaymentMethodCreateParams.SepaDebit sepaDebit,
            @Nullable PaymentMethod.BillingDetails billingDetails,
            @Nullable Map<String, String> metadata) {
        return new PaymentMethodCreateParams(sepaDebit, billingDetails, metadata);
    }

    /**
     * @param googlePayPaymentData a {@link JSONObject} derived from Google Pay's
     *                             <a href="https://developers.google.com/pay/api/android/reference/client#tojson">PaymentData#toJson()</a>
     */
    @NonNull
    public static PaymentMethodCreateParams createFromGooglePay(
            @NonNull JSONObject googlePayPaymentData)
            throws JSONException {
        final JSONObject paymentMethodData = googlePayPaymentData
                .getJSONObject("paymentMethodData");
        final JSONObject googlePayBillingAddress = paymentMethodData
                .getJSONObject("info")
                .optJSONObject("billingAddress");
        final String paymentToken = paymentMethodData
                .getJSONObject("tokenizationData")
                .getString("token");
        final Token stripeToken = Token.fromJson(new JSONObject(paymentToken));
        final String stripeTokenId = Objects.requireNonNull(stripeToken).getId();

        final PaymentMethod.BillingDetails billingDetails;
        final String email = googlePayPaymentData.optString("email");
        if (googlePayBillingAddress != null) {
            final Address billingAddress = new Address.Builder()
                    .setLine1(googlePayBillingAddress.optString("address1"))
                    .setLine2(googlePayBillingAddress.optString("address2"))
                    .setCity(googlePayBillingAddress.optString("locality"))
                    .setState(googlePayBillingAddress.optString("administrativeArea"))
                    .setCountry(googlePayBillingAddress.optString("countryCode"))
                    .setPostalCode(googlePayBillingAddress.optString("postalCode"))
                    .build();
            billingDetails = new PaymentMethod.BillingDetails.Builder()
                    .setAddress(billingAddress)
                    .setName(googlePayBillingAddress.optString("name"))
                    .setEmail(email)
                    .setPhone(googlePayBillingAddress.optString("phoneNumber"))
                    .build();
        } else {
            billingDetails = new PaymentMethod.BillingDetails.Builder()
                    .setEmail(email)
                    .build();
        }

        return PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.create(stripeTokenId),
                billingDetails
        );
    }

    private PaymentMethodCreateParams(@NonNull Card card,
                                      @Nullable PaymentMethod.BillingDetails billingDetails,
                                      @Nullable Map<String, String> metadata) {
        this.type = Type.Card;
        this.card = card;
        this.ideal = null;
        this.fpx = null;
        this.sepaDebit = null;
        this.billingDetails = billingDetails;
        this.metadata = metadata;
    }

    private PaymentMethodCreateParams(@NonNull PaymentMethodCreateParams.Ideal ideal,
                                      @Nullable PaymentMethod.BillingDetails billingDetails,
                                      @Nullable Map<String, String> metadata) {
        this.type = Type.Ideal;
        this.card = null;
        this.ideal = ideal;
        this.fpx = null;
        this.sepaDebit = null;
        this.billingDetails = billingDetails;
        this.metadata = metadata;
    }

    private PaymentMethodCreateParams(@NonNull PaymentMethodCreateParams.Fpx fpx,
                                      @Nullable PaymentMethod.BillingDetails billingDetails,
                                      @Nullable Map<String, String> metadata) {
        this.type = Type.Fpx;
        this.card = null;
        this.ideal = null;
        this.fpx = fpx;
        this.sepaDebit = null;
        this.billingDetails = billingDetails;
        this.metadata = metadata;
    }

    private PaymentMethodCreateParams(@NonNull PaymentMethodCreateParams.SepaDebit sepaDebit,
                                      @Nullable PaymentMethod.BillingDetails billingDetails,
                                      @Nullable Map<String, String> metadata) {
        this.type = Type.SepaDebit;
        this.card = null;
        this.ideal = null;
        this.fpx = null;
        this.sepaDebit = sepaDebit;
        this.billingDetails = billingDetails;
        this.metadata = metadata;
    }

    @NonNull
    @Override
    public Map<String, Object> toParamMap() {
        final Map<String, Object> params = new HashMap<>();
        params.put(FIELD_TYPE, type.mCode);

        if (type == Type.Card && card != null) {
            params.put(FIELD_CARD, card.toParamMap());
        } else if (type == Type.Ideal && ideal != null) {
            params.put(FIELD_IDEAL, ideal.toParamMap());
        } else if (type == Type.Fpx && fpx != null) {
            params.put(FIELD_FPX, fpx.toParamMap());
        } else if (type == Type.SepaDebit && sepaDebit != null) {
            params.put(FIELD_SEPA_DEBIT, sepaDebit.toParamMap());
        }

        if (billingDetails != null) {
            params.put(FIELD_BILLING_DETAILS, billingDetails.toParamMap());
        }

        if (metadata != null) {
            params.put(FIELD_METADATA, metadata);
        }

        return params;
    }

    @NonNull
    public String getTypeCode() {
        return type.mCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, card, fpx, ideal, billingDetails, metadata);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj ||
                (obj instanceof PaymentMethodCreateParams &&
                        typedEquals((PaymentMethodCreateParams) obj));
    }

    private boolean typedEquals(@NonNull PaymentMethodCreateParams params) {
        return Objects.equals(type, params.type)
                && Objects.equals(card, params.card)
                && Objects.equals(fpx, params.fpx)
                && Objects.equals(ideal, params.ideal)
                && Objects.equals(billingDetails, params.billingDetails)
                && Objects.equals(metadata, params.metadata);
    }

    enum Type {
        Card("card"),
        Ideal("ideal"),
        Fpx("fpx"),
        SepaDebit("sepa_debit");

        @NonNull private final String mCode;

        Type(@NonNull String code) {
            mCode = code;
        }
    }

    public static final class Card implements StripeParamsModel {
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
            return Objects.hash(mNumber, mExpiryMonth, mExpiryYear, mCvc, mToken);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj || (obj instanceof Card && typedEquals((Card) obj));
        }

        private boolean typedEquals(@NonNull Card card) {
            return Objects.equals(mNumber, card.mNumber)
                    && Objects.equals(mCvc, card.mCvc)
                    && Objects.equals(mExpiryMonth, card.mExpiryMonth)
                    && Objects.equals(mExpiryYear, card.mExpiryYear)
                    && Objects.equals(mToken, card.mToken);
        }

        @NonNull
        @Override
        public Map<String, Object> toParamMap() {
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

    public static final class Ideal implements StripeParamsModel {
        private static final String FIELD_BANK = "bank";

        @Nullable private final String mBank;

        private Ideal(@NonNull Ideal.Builder builder) {
            this.mBank = builder.mBank;
        }

        @NonNull
        @Override
        public Map<String, Object> toParamMap() {
            final Map<String, Object> map = new HashMap<>();
            if (mBank != null) {
                map.put(FIELD_BANK, mBank);
            }
            return map;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mBank);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj || (obj instanceof Ideal && typedEquals((Ideal) obj));
        }

        private boolean typedEquals(@NonNull Ideal ideal) {
            return Objects.equals(mBank, ideal.mBank);
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

    public static final class Fpx implements StripeParamsModel {
        private static final String FIELD_BANK = "bank";

        @Nullable private final String mBank;

        private Fpx(@NonNull Fpx.Builder builder) {
            this.mBank = builder.mBank;
        }

        @NonNull
        @Override
        public Map<String, Object> toParamMap() {
            final AbstractMap<String, Object> map = new HashMap<>();
            map.put(FIELD_BANK, mBank);
            return map;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mBank);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj || (obj instanceof Fpx && typedEquals((Fpx) obj));
        }

        private boolean typedEquals(@NonNull Fpx fpx) {
            return Objects.equals(mBank, fpx.mBank);
        }

        public static final class Builder implements ObjectBuilder<Fpx> {
            @Nullable private String mBank;

            @NonNull
            public Fpx.Builder setBank(@Nullable String bank) {
                this.mBank = bank;
                return this;
            }

            @NonNull
            public Fpx build() {
                return new Fpx(this);
            }
        }
    }

    // TODO(mshafrir-stripe): make public
    static final class SepaDebit implements StripeParamsModel {
        private static final String FIELD_IBAN = "iban";

        @Nullable private final String iban;

        private SepaDebit(@NonNull SepaDebit.Builder builder) {
            this.iban = builder.iban;
        }

        @NonNull
        @Override
        public Map<String, Object> toParamMap() {
            final AbstractMap<String, Object> map = new HashMap<>();
            map.put(FIELD_IBAN, iban);
            return map;
        }

        @Override
        public int hashCode() {
            return Objects.hash(iban);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj || (obj instanceof SepaDebit && typedEquals((SepaDebit) obj));
        }

        private boolean typedEquals(@NonNull SepaDebit sepaDebit) {
            return Objects.equals(iban, sepaDebit.iban);
        }

        public static final class Builder implements ObjectBuilder<SepaDebit> {
            @Nullable private String iban;

            @NonNull
            public Builder setIban(@Nullable String iban) {
                this.iban = iban;
                return this;
            }

            @NonNull
            public SepaDebit build() {
                return new SepaDebit(this);
            }
        }
    }
}
