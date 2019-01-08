package com.stripe.android;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

class IssuingCardEphemeralKey extends AbstractEphemeralKey {

    protected IssuingCardEphemeralKey(Parcel in) {
        super(in);
    }

    protected IssuingCardEphemeralKey(
            long created,
            @NonNull String issuingCardId,
            long expires,
            @NonNull String id,
            boolean liveMode,
            @NonNull String object,
            @NonNull String secret,
            @NonNull String type
    ) {
        super(created,
                issuingCardId,
                expires,
                id,
                liveMode,
                object,
                secret,
                type);

    }

    @SuppressWarnings("checkstyle:RedundantModifier") // Not actually redundant :|
    public IssuingCardEphemeralKey(
            @Nullable JSONObject jsonObject
    ) throws JSONException {
        super(jsonObject);

    }


    @NonNull
    public String getIssuingCardId() {
        return mObjectId;
    }

    public static final Creator<IssuingCardEphemeralKey> CREATOR
            = new Creator<IssuingCardEphemeralKey>() {

        @Override
        public IssuingCardEphemeralKey createFromParcel(Parcel in) {
            return new IssuingCardEphemeralKey(in);
        }

        @Override
        public IssuingCardEphemeralKey[] newArray(int size) {
            return new IssuingCardEphemeralKey[size];
        }
    };

    @Nullable
    static IssuingCardEphemeralKey fromString(@Nullable String rawJson) {
        return (IssuingCardEphemeralKey) AbstractEphemeralKey
                .fromString(rawJson, IssuingCardEphemeralKey.class);
    }

    @Nullable
    static IssuingCardEphemeralKey fromJson(@Nullable JSONObject jsonObject) {
        return (IssuingCardEphemeralKey) AbstractEphemeralKey
                .fromJson(jsonObject, IssuingCardEphemeralKey.class);
    }
}
