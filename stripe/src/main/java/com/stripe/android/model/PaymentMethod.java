package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * https://site-admin.stripe.com/docs/api/payment_methods
 */
public class PaymentMethod extends StripeJsonModel {
    private static final String FIELD_ID = "id";
    private static final String FIELD_BILLING_DETAILS = "billing_details";
    private static final String FIELD_CARD = "card";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_CUSTOMER = "customer";
    private static final String FIELD_IDEAL = "ideal";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_SEPA_DEBIT = "sepa_debit";
    private static final String FIELD_TYPE = "type";

    @NonNull public final String id;
    public final long created;
    public final boolean liveMode;
    @Nullable public final String type;
    @Nullable public final BillingDetails billingDetails;
    @Nullable public final Card card;
    @Nullable public final SourceSepaDebitData sepaDebit;
    @Nullable public final Ideal ideal;
    @Nullable public final String customerId;
    @Nullable public final Map<String, String> metadata;

    private PaymentMethod(@NonNull Builder builder) {
        id = builder.mId;
        liveMode = builder.mLiveMode;
        type = builder.mType;
        created = builder.mCreated;
        billingDetails = builder.mBillingDetails;
        metadata = builder.mMetadata;
        customerId = builder.mCustomerId;

        card = builder.mCard;
        sepaDebit = builder.mSepaDebit;
        ideal = builder.mIdeal;
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
        paymentMethod.put(FIELD_METADATA, metadata);
        paymentMethod.put(FIELD_CARD,
                card != null ? card.toMap() : null);
        paymentMethod.put(FIELD_SEPA_DEBIT,
                sepaDebit != null ? sepaDebit.toMap() : null);
        paymentMethod.put(FIELD_IDEAL,
                ideal != null ? ideal.toMap() : null);
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
            paymentMethod.put(FIELD_TYPE, type);
            paymentMethod.put(FIELD_BILLING_DETAILS,
                    billingDetails != null ? billingDetails.toJson() : null);
            paymentMethod.put(FIELD_METADATA,
                    metadata != null ? new JSONObject(metadata) : null);
            paymentMethod.put(FIELD_CARD,
                    card != null ? card.toJson() : null);
            paymentMethod.put(FIELD_SEPA_DEBIT,
                    sepaDebit != null ? sepaDebit.toJson() : null);
            paymentMethod.put(FIELD_IDEAL,
                    ideal != null ? ideal.toJson() : null);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return paymentMethod;
    }

    public static final class Builder {
        private String mId;
        private long mCreated;
        private boolean mLiveMode;
        private String mType;
        private BillingDetails mBillingDetails;
        private Card mCard;
        private SourceSepaDebitData mSepaDebit;
        private Ideal mIdeal;
        private String mCustomerId;
        private Map<String, String> mMetadata;

        @NonNull
        public Builder setId(String id) {
            this.mId = id;
            return this;
        }

        @NonNull
        public Builder setCreated(long created) {
            this.mCreated = created;
            return this;
        }

        @NonNull
        public Builder setLiveMode(boolean liveMode) {
            this.mLiveMode = liveMode;
            return this;
        }

        @NonNull
        public Builder setType(String type) {
            this.mType = type;
            return this;
        }

        @NonNull
        public Builder setBillingDetails(BillingDetails billingDetails) {
            this.mBillingDetails = billingDetails;
            return this;
        }

        @NonNull
        public Builder setCard(Card card) {
            this.mCard = card;
            return this;
        }

        @NonNull
        public Builder setSepaDebit(SourceSepaDebitData sepaDebit) {
            this.mSepaDebit = sepaDebit;
            return this;
        }

        @NonNull
        public Builder setCustomerId(String customerId) {
            this.mCustomerId = customerId;
            return this;
        }

        @NonNull
        public Builder setMetadata(Map<String, String> metadata) {
            this.mMetadata = metadata;
            return this;
        }

        @NonNull
        public Builder setIdeal(Ideal ideal) {
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
            public Builder setBank(@NonNull String bank) {
                this.mBank = bank;
                return this;
            }

            @NonNull
            public Builder setBankIdentifierCode(@NonNull String bankIdentifierCode) {
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
