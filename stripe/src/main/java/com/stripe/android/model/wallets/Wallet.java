package com.stripe.android.model.wallets;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.StripeJsonModel;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.stripe.android.model.StripeJsonUtils.optString;

public abstract class Wallet extends StripeJsonModel implements Parcelable {
    static final String FIELD_DYNAMIC_LAST4 = "dynamic_last4";
    static final String FIELD_TYPE = "type";

    @Nullable private final String dynamicLast4;
    @NonNull private final Type walletType;

    Wallet(@NonNull Type walletType, @NonNull Builder builder) {
        this.walletType = walletType;
        dynamicLast4 = builder.mDynamicLast4;
    }

    Wallet(@NonNull Parcel in) {
        dynamicLast4 = in.readString();
        walletType = Objects.requireNonNull(Type.fromCode(in.readString()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(dynamicLast4);
        dest.writeString(walletType.code);
    }

    @NonNull
    @Override
    public final Map<String, Object> toMap() {
        final AbstractMap<String, Object> wallet = new HashMap<>();
        wallet.put(FIELD_TYPE, walletType.code);
        wallet.put(FIELD_DYNAMIC_LAST4, dynamicLast4);
        wallet.put(walletType.code, getWalletTypeMap());
        return wallet;
    }

    @NonNull
    @Override
    public final JSONObject toJson() {
        final JSONObject wallet = new JSONObject();
        try {
            wallet.put(FIELD_TYPE, walletType.code);
            wallet.put(FIELD_DYNAMIC_LAST4, dynamicLast4);
            wallet.put(walletType.code, getWalletTypeJson());
        } catch (JSONException ignore) {
        }
        return wallet;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(dynamicLast4, walletType);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof Wallet && typedEquals((Wallet) obj));
    }

    private boolean typedEquals(@NonNull Wallet wallet) {
        return ObjectUtils.equals(dynamicLast4, wallet.dynamicLast4)
                && ObjectUtils.equals(walletType, wallet.walletType);
    }

    @NonNull
    abstract Map<String, Object> getWalletTypeMap();

    @NonNull
    abstract JSONObject getWalletTypeJson();

    abstract static class Builder<W extends Wallet> {
        @Nullable private String mDynamicLast4;

        @NonNull
        public Builder setDynamicLast4(@Nullable String dynamicLast4) {
            this.mDynamicLast4 = dynamicLast4;
            return this;
        }

        @NonNull
        abstract W build();
    }

    enum Type {
        AmexExpressCheckout("amex_express_checkout"),
        ApplePay("apple_pay"),
        GooglePay("google_pay"),
        Masterpass("master_pass"),
        SamsungPay("samsung_pay"),
        VisaCheckout("visa_checkout");

        @NonNull public final String code;

        Type(@NonNull String code) {
            this.code = code;
        }

        @Nullable
        static Type fromCode(@Nullable String code) {
            for (Type type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }

            return null;
        }
    }

    public static class Address extends StripeJsonModel implements Parcelable {
        static final String FIELD_CITY = "city";
        static final String FIELD_COUNTRY = "country";
        static final String FIELD_LINE1 = "line1";
        static final String FIELD_LINE2 = "line2";
        static final String FIELD_POSTAL_CODE = "postal_code";
        static final String FIELD_STATE = "state";

        @Nullable public final String city;
        @Nullable public final String country;
        @Nullable public final String line1;
        @Nullable public final String line2;
        @Nullable public final String postalCode;
        @Nullable public final String state;

        private Address(@NonNull Builder builder) {
            city = builder.mCity;
            country = builder.mCountry;
            line1 = builder.mLine1;
            line2 = builder.mLine2;
            postalCode = builder.mPostalCode;
            state = builder.mState;
        }

        private Address(@NonNull Parcel in) {
            city = in.readString();
            country = in.readString();
            line1 = in.readString();
            line2 = in.readString();
            postalCode = in.readString();
            state = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(city);
            dest.writeString(country);
            dest.writeString(line1);
            dest.writeString(line2);
            dest.writeString(postalCode);
            dest.writeString(state);
        }

        public static final Parcelable.Creator<Address> CREATOR =
                new Parcelable.Creator<Address>() {
                    @Override
                    public Address createFromParcel(@NonNull Parcel in) {
                        return new Address(in);
                    }

                    @Override
                    public Address[] newArray(int size) {
                        return new Address[size];
                    }
                };

        @NonNull
        @Override
        public Map<String, Object> toMap() {
            final AbstractMap<String, Object> address = new HashMap<>();
            address.put(FIELD_CITY, city);
            address.put(FIELD_COUNTRY, country);
            address.put(FIELD_LINE1, line1);
            address.put(FIELD_LINE2, line2);
            address.put(FIELD_POSTAL_CODE, postalCode);
            address.put(FIELD_STATE, state);
            return address;
        }

        @NonNull
        @Override
        public JSONObject toJson() {
            final JSONObject address = new JSONObject();
            try {
                address.put(FIELD_CITY, city);
                address.put(FIELD_COUNTRY, country);
                address.put(FIELD_LINE1, line1);
                address.put(FIELD_LINE2, line2);
                address.put(FIELD_POSTAL_CODE, postalCode);
                address.put(FIELD_STATE, state);
            } catch (JSONException ignore) {
            }
            return address;
        }

        @Nullable
        static Address fromJson(@Nullable JSONObject addressJson) {
            if (addressJson == null) {
                return null;
            }

            return new Builder()
                    .setCity(optString(addressJson, FIELD_CITY))
                    .setCountry(optString(addressJson, FIELD_COUNTRY))
                    .setLine1(optString(addressJson, FIELD_LINE1))
                    .setLine2(optString(addressJson, FIELD_LINE2))
                    .setPostalCode(optString(addressJson, FIELD_POSTAL_CODE))
                    .setState(optString(addressJson, FIELD_STATE))
                    .build();
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(city, country, line1, line2, postalCode, state);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj || (obj instanceof Address && typedEquals((Address) obj));
        }

        private boolean typedEquals(@NonNull Address address) {
            return ObjectUtils.equals(city, address.city)
                    && ObjectUtils.equals(country, address.country)
                    && ObjectUtils.equals(line1, address.line1)
                    && ObjectUtils.equals(line2, address.line2)
                    && ObjectUtils.equals(postalCode, address.postalCode)
                    && ObjectUtils.equals(state, address.state);
        }

        static final class Builder {
            @Nullable private String mCity;
            @Nullable private String mCountry;
            @Nullable private String mLine1;
            @Nullable private String mLine2;
            @Nullable private String mPostalCode;
            @Nullable private String mState;

            @NonNull
            public Builder setCity(@Nullable String city) {
                this.mCity = city;
                return this;
            }

            @NonNull
            public Builder setCountry(@Nullable String country) {
                this.mCountry = country;
                return this;
            }

            @NonNull
            public Builder setLine1(@Nullable String line1) {
                this.mLine1 = line1;
                return this;
            }

            @NonNull
            public Builder setLine2(@Nullable String line2) {
                this.mLine2 = line2;
                return this;
            }

            @NonNull
            public Builder setPostalCode(@Nullable String postalCode) {
                this.mPostalCode = postalCode;
                return this;
            }

            @NonNull
            public Builder setState(@Nullable String state) {
                this.mState = state;
                return this;
            }

            @NonNull
            public Address build() {
                return new Address(this);
            }
        }
    }
}
