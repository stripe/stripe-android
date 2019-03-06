package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

class IssuingCardEphemeralKey extends AbstractEphemeralKey {
    public static final Parcelable.Creator<IssuingCardEphemeralKey> CREATOR
            = new Parcelable.Creator<IssuingCardEphemeralKey>() {

        @Override
        public IssuingCardEphemeralKey createFromParcel(Parcel in) {
            return new IssuingCardEphemeralKey(in);
        }

        @Override
        public IssuingCardEphemeralKey[] newArray(int size) {
            return new IssuingCardEphemeralKey[size];
        }
    };

    private IssuingCardEphemeralKey(@NonNull Parcel in) {
        super(in);
    }

    protected IssuingCardEphemeralKey(
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
    public IssuingCardEphemeralKey(@Nullable JSONObject jsonObject) throws JSONException {
        super(jsonObject);
    }

    @NonNull
    String getIssuingCardId() {
        return mObjectId;
    }

    @NonNull
    static IssuingCardEphemeralKey fromString(@Nullable String rawJson) throws JSONException {
        return AbstractEphemeralKey
                .fromString(rawJson, IssuingCardEphemeralKey.class);
    }

    @NonNull
    static IssuingCardEphemeralKey fromJson(@Nullable JSONObject jsonObject) {
        return AbstractEphemeralKey
                .fromJson(jsonObject, IssuingCardEphemeralKey.class);
    }
}
