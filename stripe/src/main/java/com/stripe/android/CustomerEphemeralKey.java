package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

class CustomerEphemeralKey extends AbstractEphemeralKey {

    protected CustomerEphemeralKey(Parcel in) {
        super(in);
    }

    protected CustomerEphemeralKey(
            long created,
            @NonNull String customerId,
            long expires,
            @NonNull String id,
            boolean liveMode,
            @NonNull String object,
            @NonNull String secret,
            @NonNull String type
    ) {
        super(created,
                customerId,
                expires,
                id,
                liveMode,
                object,
                secret,
                type);

    }

    @SuppressWarnings("checkstyle:RedundantModifier") // Not actually redundant :|
    public CustomerEphemeralKey(
            @Nullable JSONObject jsonObject
    ) throws JSONException {
        super(jsonObject);

    }


    @NonNull
    String getCustomerId() {
        return mObjectId;
    }

    public static final Parcelable.Creator<CustomerEphemeralKey> CREATOR
            = new Parcelable.Creator<CustomerEphemeralKey>() {

        @Override
        public CustomerEphemeralKey createFromParcel(Parcel in) {
            return new CustomerEphemeralKey(in);
        }

        @Override
        public CustomerEphemeralKey[] newArray(int size) {
            return new CustomerEphemeralKey[size];
        }
    };

    @Nullable
    static CustomerEphemeralKey fromString(@Nullable String rawJson) {
        return (CustomerEphemeralKey) AbstractEphemeralKey
                .fromString(rawJson, CustomerEphemeralKey.class);
    }

    @Nullable
    static CustomerEphemeralKey fromJson(@Nullable JSONObject jsonObject) {
        return (CustomerEphemeralKey) AbstractEphemeralKey
                .fromJson(jsonObject, CustomerEphemeralKey.class);
    }
}
