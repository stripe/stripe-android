package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.wallets.Wallet;
import com.stripe.android.model.wallets.WalletFactory;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optBoolean;
import static com.stripe.android.model.StripeJsonUtils.optHash;
import static com.stripe.android.model.StripeJsonUtils.optInteger;
import static com.stripe.android.model.StripeJsonUtils.optLong;
import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * Model for PaymentMethod object
 *
 * See https://stripe.com/docs/api/payment_methods for documentation
 *
 * See {@link PaymentMethodCreateParams} for PaymentMethod creation
 */
public class PaymentMethod extends StripeJsonModel {
    private static final String FIELD_ID = "id";
    private static final String FIELD_BILLING_DETAILS = "billing_details";
    private static final String FIELD_CARD = "card";
    private static final String FIELD_CARD_PRESENT = "card_present";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_CUSTOMER = "customer";
    private static final String FIELD_IDEAL = "ideal";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_TYPE = "type";

    @Nullable public final String id;
    @Nullable public final Long created;
    public final boolean liveMode;
    @Nullable public final String type;
    @Nullable public final BillingDetails billingDetails;
    @Nullable public final Card card;
    @Nullable public final CardPresent cardPresent;
    @Nullable public final Ideal ideal;
    @Nullable public final String customerId;
    @Nullable public final Map<String, String> metadata;

    private PaymentMethod(@NonNull Builder builder) {
        id = builder.mId;
        liveMode = builder.mLiveMode;
        type = builder.mType;
        created = builder.mCreated;
        billingDetails = builder.mBillingDetails;
        customerId = builder.mCustomerId;

        card = builder.mCard;
        cardPresent = builder.mCardPresent;
        ideal = builder.mIdeal;
        metadata = builder.mMetadata;
    }

    /**
     * @return true if the data in the model is valid
     */
    public boolean isValid() {
        return type != null;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final AbstractMap<String, Object> paymentMethod = new HashMap<>();
        paymentMethod.put(FIELD_ID, id);
        paymentMethod.put(FIELD_CREATED, created);
        paymentMethod.put(FIELD_CUSTOMER, customerId);
        paymentMethod.put(FIELD_LIVEMODE, liveMode);
        paymentMethod.put(FIELD_TYPE, type);
        paymentMethod.put(FIELD_BILLING_DETAILS,
                billingDetails != null ? billingDetails.toMap() : null);
        paymentMethod.put(FIELD_CARD,
                card != null ? card.toMap() : null);
        paymentMethod.put(FIELD_CARD_PRESENT,
                cardPresent != null ? cardPresent.toMap() : null);
        paymentMethod.put(FIELD_IDEAL,
                ideal != null ? ideal.toMap() : null);
        paymentMethod.put(FIELD_METADATA, metadata);
        return paymentMethod;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        final JSONObject paymentMethod = new JSONObject();
        try {
            paymentMethod.put(FIELD_ID, id);
            paymentMethod.put(FIELD_CREATED, created);
            paymentMethod.put(FIELD_CUSTOMER, customerId);
            paymentMethod.put(FIELD_LIVEMODE, liveMode);
            paymentMethod.put(FIELD_METADATA, metadata != null ? new JSONObject(metadata) : null);
            paymentMethod.put(FIELD_TYPE, type);
            paymentMethod.put(FIELD_BILLING_DETAILS,
                    billingDetails != null ? billingDetails.toJson() : null);
            paymentMethod.put(FIELD_CARD,
                    card != null ? card.toJson() : null);
            paymentMethod.put(FIELD_CARD_PRESENT,
                    cardPresent != null ? cardPresent.toJson() : null);
            paymentMethod.put(FIELD_IDEAL,
                    ideal != null ? ideal.toJson() : null);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return paymentMethod;
    }

    @Nullable
    public static PaymentMethod fromString(@Nullable String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static PaymentMethod fromJson(@Nullable JSONObject paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }

        final String type = optString(paymentMethod, FIELD_TYPE);
        final Builder builder = new Builder()
                .setId(optString(paymentMethod, FIELD_ID))
                .setType(type)
                .setCreated(optLong(paymentMethod, FIELD_CREATED))
                .setBillingDetails(BillingDetails.fromJson(
                        paymentMethod.optJSONObject(FIELD_BILLING_DETAILS)))
                .setCustomerId(optString(paymentMethod, FIELD_CUSTOMER))
                .setLiveMode(Boolean.TRUE.equals(paymentMethod.optBoolean(FIELD_LIVEMODE)))
                .setMetadata(optHash(paymentMethod, FIELD_METADATA));

        if (FIELD_CARD.equals(type)) {
            builder.setCard(Card.fromJson(paymentMethod.optJSONObject(FIELD_CARD)));
        } else if (FIELD_CARD_PRESENT.equals(type)) {
            builder.setCardPresent(CardPresent.EMPTY);
        } else if (FIELD_IDEAL.equals(type)) {
            builder.setIdeal(Ideal.fromJson(paymentMethod.optJSONObject(FIELD_IDEAL)));
        }

        return builder.build();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof PaymentMethod && typedEquals((PaymentMethod) obj));
    }

    private boolean typedEquals(@NonNull PaymentMethod paymentMethod) {
        return ObjectUtils.equals(id, paymentMethod.id)
                && ObjectUtils.equals(created, paymentMethod.created)
                && liveMode == paymentMethod.liveMode
                && ObjectUtils.equals(type, paymentMethod.type)
                && ObjectUtils.equals(billingDetails, paymentMethod.billingDetails)
                && ObjectUtils.equals(card, paymentMethod.card)
                && ObjectUtils.equals(cardPresent, paymentMethod.cardPresent)
                && ObjectUtils.equals(ideal, paymentMethod.ideal)
                && ObjectUtils.equals(customerId, paymentMethod.customerId);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(id, created, liveMode, type, billingDetails, card, cardPresent,
                ideal, customerId);
    }

    public static final class Builder {
        private String mId;
        private Long mCreated;
        private boolean mLiveMode;
        private String mType;
        private BillingDetails mBillingDetails;
        private Map<String, String> mMetadata;
        private Card mCard;
        private CardPresent mCardPresent;
        private Ideal mIdeal;
        private String mCustomerId;

        @NonNull
        public Builder setId(@Nullable String id) {
            this.mId = id;
            return this;
        }

        @NonNull
        public Builder setCreated(@Nullable Long created) {
            this.mCreated = created;
            return this;
        }

        @NonNull
        public Builder setLiveMode(boolean liveMode) {
            this.mLiveMode = liveMode;
            return this;
        }

        @NonNull
        public Builder setMetadata(@Nullable Map<String, String> metadata) {
            this.mMetadata = metadata;
            return this;
        }

        @NonNull
        public Builder setType(@Nullable String type) {
            this.mType = type;
            return this;
        }

        @NonNull
        public Builder setBillingDetails(@Nullable BillingDetails billingDetails) {
            this.mBillingDetails = billingDetails;
            return this;
        }

        @NonNull
        public Builder setCard(@Nullable Card card) {
            this.mCard = card;
            return this;
        }

        @NonNull
        public Builder setCardPresent(@Nullable CardPresent cardPresent) {
            this.mCardPresent = cardPresent;
            return this;
        }

        @NonNull
        public Builder setCustomerId(@Nullable String customerId) {
            this.mCustomerId = customerId;
            return this;
        }

        @NonNull
        public Builder setIdeal(@Nullable Ideal ideal) {
            this.mIdeal = ideal;
            return this;
        }

        @NonNull
        public PaymentMethod build() {
            return new PaymentMethod(this);
        }
    }

    public static final class BillingDetails extends StripeJsonModel {
        private static final String FIELD_ADDRESS = "address";
        private static final String FIELD_EMAIL = "email";
        private static final String FIELD_NAME = "name";
        private static final String FIELD_PHONE = "phone";

        @NonNull public final Address address;
        public final String email;
        public final String name;
        public final String phone;

        private BillingDetails(@NonNull Builder builder) {
            address = builder.mAddress;
            email = builder.mEmail;
            name = builder.mName;
            phone = builder.mPhone;
        }

        @NonNull
        @Override
        public Map<String, Object> toMap() {
            final Map<String, Object> billingDetails = new HashMap<>();
            billingDetails.put(FIELD_ADDRESS, address.toMap());
            billingDetails.put(FIELD_EMAIL, email);
            billingDetails.put(FIELD_NAME, name);
            billingDetails.put(FIELD_PHONE, phone);
            return billingDetails;
        }

        @NonNull
        @Override
        public JSONObject toJson() {
            final JSONObject billingDetails = new JSONObject();
            try {
                billingDetails.put(FIELD_ADDRESS, address.toJson());
                billingDetails.put(FIELD_EMAIL, email);
                billingDetails.put(FIELD_NAME, name);
                billingDetails.put(FIELD_PHONE, phone);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return billingDetails;
        }

        @Nullable
        public static BillingDetails fromJson(@Nullable JSONObject billingDetails) {
            if (billingDetails == null) {
                return null;
            }

            return new BillingDetails.Builder()
                    .setAddress(Address.fromJson(billingDetails.optJSONObject(FIELD_ADDRESS)))
                    .setEmail(optString(billingDetails, FIELD_EMAIL))
                    .setName(optString(billingDetails, FIELD_NAME))
                    .setPhone(optString(billingDetails, FIELD_PHONE))
                    .build();
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(address, email, name, phone);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj
                    || obj instanceof BillingDetails && typedEquals((BillingDetails) obj);
        }

        private boolean typedEquals(@NonNull BillingDetails obj) {
            return ObjectUtils.equals(address, obj.address)
                    && ObjectUtils.equals(email, obj.email)
                    && ObjectUtils.equals(name, obj.name)
                    && ObjectUtils.equals(phone, obj.phone);
        }

        public static final class Builder {
            private Address mAddress;
            private String mEmail;
            private String mName;
            private String mPhone;

            @NonNull
            public Builder setAddress(@Nullable Address address) {
                this.mAddress = address;
                return this;
            }

            @NonNull
            public Builder setEmail(@Nullable String email) {
                this.mEmail = email;
                return this;
            }

            @NonNull
            public Builder setName(@Nullable String name) {
                this.mName = name;
                return this;
            }

            @NonNull
            public Builder setPhone(@Nullable String phone) {
                this.mPhone = phone;
                return this;
            }

            @NonNull
            public BillingDetails build() {
                return new BillingDetails(this);
            }
        }
    }

    public static final class Card extends StripeJsonModel {
        private static final String FIELD_BRAND = "brand";
        private static final String FIELD_CHECKS = "checks";
        private static final String FIELD_COUNTRY = "country";
        private static final String FIELD_EXP_MONTH = "exp_month";
        private static final String FIELD_EXP_YEAR = "exp_year";
        private static final String FIELD_FUNDING = "funding";
        private static final String FIELD_LAST4 = "last4";
        private static final String FIELD_THREE_D_SECURE_USAGE = "three_d_secure_usage";
        private static final String FIELD_WALLET = "wallet";

        @Nullable public final String brand;
        @Nullable public final Checks checks;
        @Nullable public final String country;
        @Nullable public final Integer expiryMonth;
        @Nullable public final Integer expiryYear;
        @Nullable public final String funding;
        @Nullable public final String last4;
        @Nullable public final ThreeDSecureUsage threeDSecureUsage;
        @Nullable public final Wallet wallet;

        private Card(@NonNull Builder builder) {
            brand = builder.mBrand;
            checks = builder.checks;
            country = builder.mCountry;
            expiryMonth = builder.mExpiryMonth;
            expiryYear = builder.mExpiryYear;
            funding = builder.mFunding;
            last4 = builder.mLast4;
            threeDSecureUsage = builder.mThreeDSecureUsage;
            wallet = builder.mWallet;
        }

        @NonNull
        @Override
        public Map<String, Object> toMap() {
            final AbstractMap<String, Object> map = new HashMap<>();
            map.put(FIELD_BRAND, brand);
            map.put(FIELD_CHECKS, checks != null ? checks.toMap() : null);
            map.put(FIELD_COUNTRY, country);
            map.put(FIELD_EXP_MONTH, expiryMonth);
            map.put(FIELD_EXP_YEAR, expiryYear);
            map.put(FIELD_FUNDING, funding);
            map.put(FIELD_LAST4, last4);
            map.put(FIELD_THREE_D_SECURE_USAGE,
                    threeDSecureUsage != null ? threeDSecureUsage.toMap() : null);
            map.put(FIELD_WALLET, wallet);
            return map;
        }

        @NonNull
        @Override
        public JSONObject toJson() {
            final JSONObject json = new JSONObject();
            try {
                json.put(FIELD_BRAND, brand);
                json.put(FIELD_CHECKS, checks != null ? checks.toJson() : null);
                json.put(FIELD_COUNTRY, country);
                json.put(FIELD_EXP_MONTH, expiryMonth);
                json.put(FIELD_EXP_YEAR, expiryYear);
                json.put(FIELD_FUNDING, funding);
                json.put(FIELD_LAST4, last4);
                json.put(FIELD_THREE_D_SECURE_USAGE, threeDSecureUsage != null ?
                        threeDSecureUsage.toJson() : null);
                json.put(FIELD_WALLET, wallet);
            } catch (JSONException ignore) {}
            return json;
        }

        @Nullable
        public static Card fromJson(@Nullable JSONObject cardJson) {
            if (cardJson == null) {
                return null;
            }

            return new Card.Builder()
                    .setBrand(optString(cardJson, FIELD_BRAND))
                    .setChecks(Checks.fromJson(cardJson.optJSONObject(FIELD_CHECKS)))
                    .setCountry(optString(cardJson, FIELD_COUNTRY))
                    .setExpiryMonth(optInteger(cardJson, FIELD_EXP_MONTH))
                    .setExpiryYear(optInteger(cardJson, FIELD_EXP_YEAR))
                    .setFunding(optString(cardJson, FIELD_FUNDING))
                    .setLast4(optString(cardJson, FIELD_LAST4))
                    .setThreeDSecureUsage(ThreeDSecureUsage
                            .fromJson(cardJson.optJSONObject(FIELD_THREE_D_SECURE_USAGE)))
                    .setWallet(new WalletFactory().create(cardJson.optJSONObject(FIELD_WALLET)))
                    .build();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof Card && typedEquals((Card) obj));
        }

        private boolean typedEquals(@NonNull Card card) {
            return ObjectUtils.equals(brand, card.brand)
                    && ObjectUtils.equals(checks, card.checks)
                    && ObjectUtils.equals(country, card.country)
                    && ObjectUtils.equals(expiryMonth, card.expiryMonth)
                    && ObjectUtils.equals(expiryYear, card.expiryYear)
                    && ObjectUtils.equals(funding, card.funding)
                    && ObjectUtils.equals(last4, card.last4)
                    && ObjectUtils.equals(threeDSecureUsage, card.threeDSecureUsage)
                    && ObjectUtils.equals(wallet, card.wallet);
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(brand, checks, country, expiryMonth, expiryYear, funding,
                    last4, threeDSecureUsage, wallet);
        }

        public static final class Builder {
            private String mBrand;
            private Checks checks;
            private String mCountry;
            private Integer mExpiryMonth;
            private Integer mExpiryYear;
            private String mFunding;
            private String mLast4;
            private ThreeDSecureUsage mThreeDSecureUsage;
            private Wallet mWallet;

            @NonNull
            public Builder setBrand(@Nullable String brand) {
                this.mBrand = brand;
                return this;
            }

            @NonNull
            public Builder setChecks(@Nullable Checks checks) {
                this.checks = checks;
                return this;
            }

            @NonNull
            public Builder setCountry(@Nullable String country) {
                this.mCountry = country;
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
            public Builder setFunding(@Nullable String funding) {
                this.mFunding = funding;
                return this;
            }

            @NonNull
            public Builder setLast4(@Nullable String last4) {
                this.mLast4 = last4;
                return this;
            }

            @NonNull
            public Builder setThreeDSecureUsage(@Nullable ThreeDSecureUsage threeDSecureUsage) {
                this.mThreeDSecureUsage = threeDSecureUsage;
                return this;
            }

            @NonNull
            public Builder setWallet(@Nullable Wallet wallet) {
                this.mWallet = wallet;
                return this;
            }

            @NonNull
            public Card build() {
                return new Card(this);
            }
        }

        public static final class Checks extends StripeJsonModel {
            private static final String FIELD_ADDRESS_LINE1_CHECK = "address_line1_check";
            private static final String FIELD_ADDRESS_POSTAL_CODE_CHECK =
                    "address_postal_code_check";
            private static final String FIELD_CVC_CHECK = "cvc_check";

            @Nullable public final String addressLine1Check;
            @Nullable public final String addressPostalCodeCheck;
            @Nullable public final String cvcCheck;

            private Checks(@NonNull Builder builder) {
                this.addressLine1Check = builder.addressLine1Check;
                this.addressPostalCodeCheck = builder.addressPostalCodeCheck;
                this.cvcCheck = builder.cvcCheck;
            }

            @NonNull
            @Override
            public Map<String, Object> toMap() {
                final AbstractMap<String, Object> map = new HashMap<>();
                map.put(FIELD_ADDRESS_LINE1_CHECK, addressLine1Check);
                map.put(FIELD_ADDRESS_POSTAL_CODE_CHECK, addressPostalCodeCheck);
                map.put(FIELD_CVC_CHECK, cvcCheck);
                return map;
            }

            @NonNull
            @Override
            public JSONObject toJson() {
                final JSONObject json = new JSONObject();
                try {
                    json.put(FIELD_ADDRESS_LINE1_CHECK, addressLine1Check);
                    json.put(FIELD_ADDRESS_POSTAL_CODE_CHECK, addressPostalCodeCheck);
                    json.put(FIELD_CVC_CHECK, cvcCheck);
                } catch (JSONException ignore) {}
                return json;
            }

            @Nullable
            public static Checks fromJson(@Nullable JSONObject checksJson) {
                if (checksJson == null) {
                    return null;
                }

                return new Checks.Builder()
                        .setAddressLine1Check(optString(checksJson, FIELD_ADDRESS_LINE1_CHECK))
                        .setAddressPostalCodeCheck(
                                optString(checksJson, FIELD_ADDRESS_POSTAL_CODE_CHECK))
                        .setCvcCheck(optString(checksJson, FIELD_CVC_CHECK))
                        .build();
            }

            @Override
            public boolean equals(Object obj) {
                return this == obj || (obj instanceof Checks && typedEquals((Checks) obj));
            }

            private boolean typedEquals(@NonNull Checks checks) {
                return ObjectUtils.equals(addressLine1Check, checks.addressLine1Check)
                        && ObjectUtils.equals(addressPostalCodeCheck, checks.addressPostalCodeCheck)
                        && ObjectUtils.equals(cvcCheck, checks.cvcCheck);
            }

            @Override
            public int hashCode() {
                return ObjectUtils.hash(addressLine1Check, addressPostalCodeCheck, cvcCheck);
            }
            
            public static final class Builder {
                @Nullable private String addressLine1Check;
                @Nullable private String addressPostalCodeCheck;
                @Nullable private String cvcCheck;

                @NonNull
                public Builder setAddressLine1Check(@Nullable String addressLine1Check) {
                    this.addressLine1Check = addressLine1Check;
                    return this;
                }

                @NonNull
                public Builder setAddressPostalCodeCheck(@Nullable String addressPostalCodeCheck) {
                    this.addressPostalCodeCheck = addressPostalCodeCheck;
                    return this;
                }

                @NonNull
                public Builder setCvcCheck(@Nullable String cvcCheck) {
                    this.cvcCheck = cvcCheck;
                    return this;
                }

                @NonNull
                public Checks build() {
                    return new Checks(this);
                }
            }
        }

        public static final class ThreeDSecureUsage extends StripeJsonModel {
            private static final String FIELD_IS_SUPPORTED = "supported";

            public final boolean isSupported;

            private ThreeDSecureUsage(@NonNull Builder builder) {
                isSupported = builder.mIsSupported;
            }

            @NonNull
            @Override
            public Map<String, Object> toMap() {
                final Map<String, Object> map = new HashMap<>();
                map.put(FIELD_IS_SUPPORTED, isSupported);
                return map;
            }

            @NonNull
            @Override
            public JSONObject toJson() {
                final JSONObject json = new JSONObject();
                try {
                    json.put(FIELD_IS_SUPPORTED, isSupported);
                } catch (JSONException ignore) {}
                return json;
            }

            @Nullable
            public static ThreeDSecureUsage fromJson(@Nullable JSONObject threeDSecureUsage) {
                if (threeDSecureUsage == null) {
                    return null;
                }

                return new Builder()
                        .setSupported(Boolean.TRUE
                                .equals(optBoolean(threeDSecureUsage, FIELD_IS_SUPPORTED)))
                        .build();
            }

            @Override
            public int hashCode() {
                return ObjectUtils.hash(isSupported);
            }

            @Override
            public boolean equals(@Nullable Object obj) {
                return this == obj || (obj instanceof ThreeDSecureUsage
                        && typedEquals((ThreeDSecureUsage) obj));
            }

            private boolean typedEquals(@NonNull ThreeDSecureUsage threeDSecureUsage) {
                return isSupported == threeDSecureUsage.isSupported;
            }

            public static final class Builder {
                private boolean mIsSupported;

                @NonNull
                public Builder setSupported(boolean supported) {
                    mIsSupported = supported;
                    return this;
                }

                @NonNull
                public ThreeDSecureUsage build() {
                    return new ThreeDSecureUsage(this);
                }
            }
        }
    }

    public static final class CardPresent extends StripeJsonModel {
        public static final CardPresent EMPTY = new CardPresent();

        private CardPresent() {}

        @NonNull
        @Override
        public Map<String, Object> toMap() {
            return new HashMap<>();
        }

        @NonNull
        @Override
        public JSONObject toJson() {
            return new JSONObject();
        }
    }

    public static final class Ideal extends StripeJsonModel {
        private static final String FIELD_BANK = "bank";
        private static final String FIELD_BIC = "bic";

        @Nullable public final String bank;
        @Nullable public final String bankIdentifierCode;

        private Ideal(@NonNull Builder builder) {
            bank = builder.mBank;
            bankIdentifierCode = builder.mBankIdentifierCode;
        }

        @NonNull
        @Override
        public Map<String, Object> toMap() {
            final AbstractMap<String, Object> ideal = new HashMap<>();
            ideal.put(FIELD_BANK, bank);
            ideal.put(FIELD_BIC, bankIdentifierCode);
            return ideal;
        }

        @NonNull
        @Override
        public JSONObject toJson() {
            final JSONObject ideal = new JSONObject();
            try {
                ideal.put(FIELD_BANK, bank);
                ideal.put(FIELD_BIC, bankIdentifierCode);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return ideal;
        }

        @Nullable
        public static Ideal fromJson(@Nullable JSONObject ideal) {
            if (ideal == null) {
                return null;
            }

            return new Ideal.Builder()
                    .setBank(optString(ideal, FIELD_BANK))
                    .setBankIdentifierCode(optString(ideal, FIELD_BIC))
                    .build();
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(bank, bankIdentifierCode);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj || obj instanceof Ideal && typedEquals((Ideal) obj);
        }

        private boolean typedEquals(@NonNull Ideal obj) {
            return ObjectUtils.equals(bank, obj.bank)
                    && ObjectUtils.equals(bankIdentifierCode, obj.bankIdentifierCode);
        }

        public static final class Builder {
            private String mBank;
            private String mBankIdentifierCode;

            @NonNull
            public Builder setBank(@Nullable String bank) {
                this.mBank = bank;
                return this;
            }

            @NonNull
            public Builder setBankIdentifierCode(@Nullable String bankIdentifierCode) {
                this.mBankIdentifierCode = bankIdentifierCode;
                return this;
            }

            @NonNull
            public Ideal build() {
                return new Ideal(this);
            }
        }
    }
}
