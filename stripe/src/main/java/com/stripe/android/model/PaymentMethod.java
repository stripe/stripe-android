package com.stripe.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.model.wallets.Wallet;
import com.stripe.android.model.wallets.WalletFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import static com.stripe.android.model.StripeJsonUtils.optBoolean;
import static com.stripe.android.model.StripeJsonUtils.optHash;
import static com.stripe.android.model.StripeJsonUtils.optInteger;
import static com.stripe.android.model.StripeJsonUtils.optLong;
import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * Model for a <a href="https://stripe.com/docs/payments/payment-methods">Payment Methods API</a>
 * object.
 *
 * See <a href="https://stripe.com/docs/api/payment_methods">Payment Methods API reference</a>.
 *
 * See {@link PaymentMethodCreateParams} for PaymentMethod creation
 */
@SuppressWarnings("WeakerAccess")
public final class PaymentMethod extends StripeModel implements Parcelable {
    private static final String FIELD_ID = "id";
    private static final String FIELD_BILLING_DETAILS = "billing_details";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_CUSTOMER = "customer";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_METADATA = "metadata";

    // types
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_CARD = "card";
    private static final String FIELD_CARD_PRESENT = "card_present";
    private static final String FIELD_FPX = "fpx";
    private static final String FIELD_IDEAL = "ideal";

    @Nullable public final String id;
    @Nullable public final Long created;
    public final boolean liveMode;
    @Nullable public final String type;
    @Nullable public final BillingDetails billingDetails;
    @Nullable public final String customerId;
    @Nullable public final Map<String, String> metadata;

    @Nullable public final Card card;
    @Nullable public final CardPresent cardPresent;
    @Nullable public final Fpx fpx;
    @Nullable public final Ideal ideal;

    public enum Type implements Parcelable {
        Card("card"),
        CardPresent("card_present"),
        Fpx("fpx", false),
        Ideal("ideal"),
        SepaDebit("sepa_debit");

        @NonNull public final String code;
        public final boolean isReusable;

        Type(@NonNull String code) {
            this(code, true);
        }

        Type(@NonNull String code, boolean isReusable) {
            this.code = code;
            this.isReusable = isReusable;
        }

        @NonNull
        @Override
        public String toString() {
            return code;
        }

        @Nullable
        public static Type lookup(@Nullable String code) {
            for (Type type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }

            return null;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(ordinal());
        }

        public static final Creator<Type> CREATOR = new Creator<Type>() {
            @Override
            public Type createFromParcel(@NonNull Parcel in) {
                return values()[in.readInt()];
            }

            @Override
            public Type[] newArray(int size) {
                return new Type[size];
            }
        };
    }

    private PaymentMethod(@NonNull Builder builder) {
        id = builder.mId;
        liveMode = builder.mLiveMode;
        type = builder.mType;
        created = builder.mCreated;
        billingDetails = builder.mBillingDetails;
        customerId = builder.mCustomerId;
        metadata = builder.mMetadata;

        card = builder.mCard;
        cardPresent = builder.mCardPresent;
        fpx = builder.mFpx;
        ideal = builder.mIdeal;
    }

    /**
     * @return true if the data in the model is valid
     */
    public boolean isValid() {
        return type != null;
    }

    @Nullable
    public static PaymentMethod fromString(@Nullable String jsonString) {
        if (jsonString == null) {
            return null;
        }

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
        } else if (FIELD_FPX.equals(type)) {
            builder.setFpx(Fpx.fromJson(paymentMethod.optJSONObject(FIELD_FPX)));
        }

        return builder.build();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof PaymentMethod && typedEquals((PaymentMethod) obj));
    }

    private boolean typedEquals(@NonNull PaymentMethod paymentMethod) {
        return Objects.equals(id, paymentMethod.id)
                && Objects.equals(created, paymentMethod.created)
                && liveMode == paymentMethod.liveMode
                && Objects.equals(type, paymentMethod.type)
                && Objects.equals(billingDetails, paymentMethod.billingDetails)
                && Objects.equals(card, paymentMethod.card)
                && Objects.equals(cardPresent, paymentMethod.cardPresent)
                && Objects.equals(fpx, paymentMethod.fpx)
                && Objects.equals(ideal, paymentMethod.ideal)
                && Objects.equals(customerId, paymentMethod.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, created, liveMode, type, billingDetails, card, cardPresent,
                fpx, ideal, customerId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        if (created == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeLong(created);
        }
        dest.writeByte((byte) (liveMode ? 0x01 : 0x00));
        dest.writeString(type);
        dest.writeParcelable(billingDetails, flags);
        dest.writeParcelable(card, flags);
        dest.writeParcelable(cardPresent, flags);
        dest.writeParcelable(fpx, flags);
        dest.writeParcelable(ideal, flags);
        dest.writeString(customerId);
        dest.writeInt(metadata == null ? -1 : metadata.size());
        if (metadata != null) {
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeString(entry.getValue());
            }
        }
    }

    private PaymentMethod(@NonNull Parcel in) {
        id = in.readString();
        created = in.readByte() == 0x00 ? null : in.readLong();
        liveMode = in.readByte() != 0x00;
        type = in.readString();
        billingDetails = in.readParcelable(BillingDetails.class.getClassLoader());
        card = in.readParcelable(Card.class.getClassLoader());
        cardPresent = in.readParcelable(CardPresent.class.getClassLoader());
        fpx = in.readParcelable(Fpx.class.getClassLoader());
        ideal = in.readParcelable(Ideal.class.getClassLoader());
        customerId = in.readString();
        final int mapSize = in.readInt();
        if (mapSize >= 0) {
            final Map<String, String> metadata = new HashMap<>(mapSize);
            for (int i = 0; i < mapSize; i++) {
                final String key = in.readString();
                final String value = in.readString();
                if (key != null && value != null) {
                    metadata.put(key, value);
                }
            }
            this.metadata = metadata;
        } else {
            this.metadata = null;
        }
    }

    public static final Parcelable.Creator<PaymentMethod> CREATOR =
            new Parcelable.Creator<PaymentMethod>() {
                @NonNull
                @Override
                public PaymentMethod createFromParcel(@NonNull Parcel in) {
                    return new PaymentMethod(in);
                }

                @Override
                public PaymentMethod[] newArray(int size) {
                    return new PaymentMethod[size];
                }
            };


    public static final class Builder implements ObjectBuilder<PaymentMethod> {
        private String mId;
        private Long mCreated;
        private boolean mLiveMode;
        private String mType;
        private BillingDetails mBillingDetails;
        private Map<String, String> mMetadata;
        private String mCustomerId;

        private Card mCard;
        private CardPresent mCardPresent;
        private Ideal mIdeal;
        private Fpx mFpx;

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
        public Builder setFpx(@Nullable Fpx fpx) {
            this.mFpx = fpx;
            return this;
        }

        @NonNull
        public PaymentMethod build() {
            return new PaymentMethod(this);
        }
    }

    public static final class BillingDetails
            extends StripeModel
            implements StripeParamsModel, Parcelable {
        static final String FIELD_ADDRESS = "address";
        static final String FIELD_EMAIL = "email";
        static final String FIELD_NAME = "name";
        static final String FIELD_PHONE = "phone";

        @Nullable public final Address address;
        public final String email;
        public final String name;
        public final String phone;

        private BillingDetails(@NonNull Builder builder) {
            address = builder.mAddress;
            email = builder.mEmail;
            name = builder.mName;
            phone = builder.mPhone;
        }

        private BillingDetails(@NonNull Parcel in) {
            address = in.readParcelable(Address.class.getClassLoader());
            email = in.readString();
            name = in.readString();
            phone = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeParcelable(address, flags);
            dest.writeString(email);
            dest.writeString(name);
            dest.writeString(phone);
        }

        @NonNull
        public Builder toBuilder() {
            return new Builder()
                    .setAddress(address)
                    .setEmail(email)
                    .setName(name)
                    .setPhone(phone);
        }

        public static final Parcelable.Creator<BillingDetails> CREATOR =
                new Parcelable.Creator<BillingDetails>() {
                    @Override
                    public BillingDetails createFromParcel(@NonNull Parcel in) {
                        return new BillingDetails(in);
                    }

                    @Override
                    public BillingDetails[] newArray(int size) {
                        return new BillingDetails[size];
                    }
                };

        @NonNull
        @Override
        public Map<String, Object> toParamMap() {
            final Map<String, Object> billingDetails = new HashMap<>();
            if (address != null) {
                billingDetails.put(FIELD_ADDRESS, address.toParamMap());
            }
            if (email != null) {
                billingDetails.put(FIELD_EMAIL, email);
            }
            if (name != null) {
                billingDetails.put(FIELD_NAME, name);
            }
            if (phone != null) {
                billingDetails.put(FIELD_PHONE, phone);
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
            return Objects.hash(address, email, name, phone);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj
                    || obj instanceof BillingDetails && typedEquals((BillingDetails) obj);
        }

        private boolean typedEquals(@NonNull BillingDetails obj) {
            return Objects.equals(address, obj.address)
                    && Objects.equals(email, obj.email)
                    && Objects.equals(name, obj.name)
                    && Objects.equals(phone, obj.phone);
        }

        public static final class Builder implements ObjectBuilder<BillingDetails> {
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

    public static final class Card extends PaymentMethodTypeImpl {
        private static final String FIELD_BRAND = "brand";
        private static final String FIELD_CHECKS = "checks";
        private static final String FIELD_COUNTRY = "country";
        private static final String FIELD_EXP_MONTH = "exp_month";
        private static final String FIELD_EXP_YEAR = "exp_year";
        private static final String FIELD_FUNDING = "funding";
        private static final String FIELD_LAST4 = "last4";
        private static final String FIELD_THREE_D_SECURE_USAGE = "three_d_secure_usage";
        private static final String FIELD_WALLET = "wallet";

        @Retention(RetentionPolicy.SOURCE)
        @StringDef({
                Brand.AMERICAN_EXPRESS,
                Brand.DISCOVER,
                Brand.JCB,
                Brand.DINERS_CLUB,
                Brand.VISA,
                Brand.MASTERCARD,
                Brand.UNIONPAY,
                Brand.UNKNOWN
        })
        public @interface Brand {
            String AMERICAN_EXPRESS = "amex";
            String DISCOVER = "discover";
            String JCB = "jcb";
            String DINERS_CLUB = "diners";
            String VISA = "visa";
            String MASTERCARD = "mastercard";
            String UNIONPAY = "unionpay";
            String UNKNOWN = "unknown";
        }

        @Nullable @Brand public final String brand;
        @Nullable public final Checks checks;
        @Nullable public final String country;
        @Nullable public final Integer expiryMonth;
        @Nullable public final Integer expiryYear;
        @Nullable public final String funding;
        @Nullable public final String last4;
        @Nullable public final ThreeDSecureUsage threeDSecureUsage;
        @Nullable public final Wallet wallet;

        private Card(@NonNull Builder builder) {
            super(Type.Card);
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

        private Card(@NonNull Parcel in) {
            super(in);
            brand = in.readString();
            checks = in.readParcelable(Checks.class.getClassLoader());
            country = in.readString();
            expiryMonth = in.readByte() == 0x00 ? null : in.readInt();
            expiryYear = in.readByte() == 0x00 ? null : in.readInt();
            funding = in.readString();
            last4 = in.readString();
            threeDSecureUsage = in.readParcelable(ThreeDSecureUsage.class.getClassLoader());
            wallet = in.readParcelable(Wallet.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(brand);
            dest.writeParcelable(checks, flags);
            dest.writeString(country);
            if (expiryMonth == null) {
                dest.writeByte((byte) (0x00));
            } else {
                dest.writeByte((byte) (0x01));
                dest.writeInt(expiryMonth);
            }
            if (expiryYear == null) {
                dest.writeByte((byte) (0x00));
            } else {
                dest.writeByte((byte) (0x01));
                dest.writeInt(expiryYear);
            }
            dest.writeString(funding);
            dest.writeString(last4);
            dest.writeParcelable(threeDSecureUsage, flags);
            dest.writeParcelable(wallet, flags);
        }

        public static final Parcelable.Creator<Card> CREATOR = new Parcelable.Creator<Card>() {
            @Override
            public Card createFromParcel(@NonNull Parcel in) {
                return new Card(in);
            }

            @Override
            public Card[] newArray(int size) {
                return new Card[size];
            }
        };

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
            return Objects.equals(brand, card.brand)
                    && Objects.equals(checks, card.checks)
                    && Objects.equals(country, card.country)
                    && Objects.equals(expiryMonth, card.expiryMonth)
                    && Objects.equals(expiryYear, card.expiryYear)
                    && Objects.equals(funding, card.funding)
                    && Objects.equals(last4, card.last4)
                    && Objects.equals(threeDSecureUsage, card.threeDSecureUsage)
                    && Objects.equals(wallet, card.wallet);
        }

        @Override
        public int hashCode() {
            return Objects.hash(brand, checks, country, expiryMonth, expiryYear, funding,
                    last4, threeDSecureUsage, wallet);
        }

        public static final class Builder implements ObjectBuilder<Card> {
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
            public Builder setBrand(@Nullable @Brand String brand) {
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

        public static final class Checks extends StripeModel implements Parcelable {
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

            private Checks(@NonNull Parcel in) {
                addressLine1Check = in.readString();
                addressPostalCodeCheck = in.readString();
                cvcCheck = in.readString();
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(@NonNull Parcel dest, int flags) {
                dest.writeString(addressLine1Check);
                dest.writeString(addressPostalCodeCheck);
                dest.writeString(cvcCheck);
            }

            public static final Parcelable.Creator<Checks> CREATOR =
                    new Parcelable.Creator<Checks>() {
                        @Override
                        public Checks createFromParcel(@NonNull Parcel in) {
                            return new Checks(in);
                        }

                        @Override
                        public Checks[] newArray(int size) {
                            return new Checks[size];
                        }
                    };

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
                return Objects.equals(addressLine1Check, checks.addressLine1Check)
                        && Objects.equals(addressPostalCodeCheck, checks.addressPostalCodeCheck)
                        && Objects.equals(cvcCheck, checks.cvcCheck);
            }

            @Override
            public int hashCode() {
                return Objects.hash(addressLine1Check, addressPostalCodeCheck, cvcCheck);
            }

            public static final class Builder implements ObjectBuilder<Checks> {
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

        public static final class ThreeDSecureUsage extends StripeModel implements Parcelable {
            private static final String FIELD_IS_SUPPORTED = "supported";

            public final boolean isSupported;

            private ThreeDSecureUsage(@NonNull Builder builder) {
                isSupported = builder.mIsSupported;
            }

            private ThreeDSecureUsage(@NonNull Parcel in) {
                isSupported = in.readByte() != 0x00;
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(@NonNull Parcel dest, int flags) {
                dest.writeByte((byte) (isSupported ? 0x01 : 0x00));
            }

            public static final Parcelable.Creator<ThreeDSecureUsage> CREATOR =
                    new Parcelable.Creator<ThreeDSecureUsage>() {
                        @Override
                        public ThreeDSecureUsage createFromParcel(@NonNull Parcel in) {
                            return new ThreeDSecureUsage(in);
                        }

                        @Override
                        public ThreeDSecureUsage[] newArray(int size) {
                            return new ThreeDSecureUsage[size];
                        }
                    };

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
                return Objects.hash(isSupported);
            }

            @Override
            public boolean equals(@Nullable Object obj) {
                return this == obj || (obj instanceof ThreeDSecureUsage
                        && typedEquals((ThreeDSecureUsage) obj));
            }

            private boolean typedEquals(@NonNull ThreeDSecureUsage threeDSecureUsage) {
                return isSupported == threeDSecureUsage.isSupported;
            }

            public static final class Builder implements ObjectBuilder<ThreeDSecureUsage> {
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

    public static final class CardPresent extends PaymentMethodTypeImpl {
        public static final CardPresent EMPTY = new CardPresent();

        private CardPresent() {
            super(Type.CardPresent);
        }

        private CardPresent(@NonNull Parcel in) {
            super(in);
        }

        public static final Parcelable.Creator<CardPresent> CREATOR =
                new Parcelable.Creator<CardPresent>() {
                    @Override
                    public CardPresent createFromParcel(@NonNull Parcel in) {
                        return new CardPresent(in);
                    }

                    @Override
                    public CardPresent[] newArray(int size) {
                        return new CardPresent[size];
                    }
                };

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj || obj instanceof CardPresent && typedEquals((CardPresent) obj);
        }

        private boolean typedEquals(@NonNull CardPresent obj) {
            return Objects.equals(type, obj.type);
        }
    }

    public static final class Ideal extends PaymentMethodTypeImpl {
        private static final String FIELD_BANK = "bank";
        private static final String FIELD_BIC = "bic";

        @Nullable public final String bank;
        @Nullable public final String bankIdentifierCode;

        private Ideal(@NonNull Builder builder) {
            super(Type.Ideal);
            bank = builder.mBank;
            bankIdentifierCode = builder.mBankIdentifierCode;
        }

        private Ideal(@NonNull Parcel in) {
            super(in);
            bank = in.readString();
            bankIdentifierCode = in.readString();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(bank);
            dest.writeString(bankIdentifierCode);
        }

        public static final Parcelable.Creator<Ideal> CREATOR = new Parcelable.Creator<Ideal>() {
            @Override
            public Ideal createFromParcel(@NonNull Parcel in) {
                return new Ideal(in);
            }

            @Override
            public Ideal[] newArray(int size) {
                return new Ideal[size];
            }
        };

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
            return Objects.hash(bank, bankIdentifierCode);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj || obj instanceof Ideal && typedEquals((Ideal) obj);
        }

        private boolean typedEquals(@NonNull Ideal obj) {
            return Objects.equals(bank, obj.bank)
                    && Objects.equals(bankIdentifierCode, obj.bankIdentifierCode);
        }

        public static final class Builder implements ObjectBuilder<Ideal> {
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

    public static final class Fpx extends PaymentMethodTypeImpl {
        private static final String FIELD_ACCOUNT_HOLDER_TYPE = "account_holder_type";
        private static final String FIELD_BANK = "bank";

        @Nullable public final String bank;
        @Nullable public final String accountHolderType;

        private Fpx(@NonNull Builder builder) {
            super(Type.Fpx);
            bank = builder.mBank;
            accountHolderType = builder.mAccountHolderType;
        }

        private Fpx(@NonNull Parcel in) {
            super(in);
            bank = in.readString();
            accountHolderType = in.readString();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(bank);
            dest.writeString(accountHolderType);
        }

        public static final Parcelable.Creator<Fpx> CREATOR = new Parcelable.Creator<Fpx>() {
            @Override
            public Fpx createFromParcel(@NonNull Parcel in) {
                return new Fpx(in);
            }

            @Override
            public Fpx[] newArray(int size) {
                return new Fpx[size];
            }
        };

        @Nullable
        public static Fpx fromJson(@Nullable JSONObject fpx) {
            if (fpx == null) {
                return null;
            }

            return new Fpx.Builder()
                    .setBank(optString(fpx, FIELD_BANK))
                    .setAccountHolderType(optString(fpx, FIELD_ACCOUNT_HOLDER_TYPE))
                    .build();
        }

        @Override
        public int hashCode() {
            return Objects.hash(bank, accountHolderType);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj || obj instanceof Fpx && typedEquals((Fpx) obj);
        }

        private boolean typedEquals(@NonNull Fpx obj) {
            return Objects.equals(bank, obj.bank)
                    && Objects.equals(accountHolderType, obj.accountHolderType);
        }

        public static final class Builder implements ObjectBuilder<Fpx> {
            private String mBank;
            private String mAccountHolderType;

            @NonNull
            public Builder setBank(@Nullable String bank) {
                this.mBank = bank;
                return this;
            }

            @NonNull
            public Builder setAccountHolderType(@Nullable String bankIdentifierCode) {
                this.mAccountHolderType = bankIdentifierCode;
                return this;
            }

            @NonNull
            public Fpx build() {
                return new Fpx(this);
            }
        }
    }

    private abstract static class PaymentMethodTypeImpl extends StripeModel
            implements Parcelable {
        @NonNull public final Type type;

        private PaymentMethodTypeImpl(@NonNull Type type) {
            this.type = type;
        }

        private PaymentMethodTypeImpl(@NonNull Parcel in) {
            type = Type.valueOf(in.readString());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeString(type.name());
        }
    }


}
