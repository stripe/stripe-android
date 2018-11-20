package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

// Provided for backward compatibility
// Use CustomerEphemeralKey instead
@Deprecated
public class EphemeralKey extends CustomerEphemeralKey {

    protected EphemeralKey(Parcel in) {
        super(in);
    }

    protected EphemeralKey(
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

    public static final Parcelable.Creator<EphemeralKey> CREATOR
            = new Parcelable.Creator<EphemeralKey>() {

        @Override
        public EphemeralKey createFromParcel(Parcel in) {
            return new EphemeralKey(in);
        }

        @Override
        public EphemeralKey[] newArray(int size) {
            return new EphemeralKey[size];
        }
    };

    @Nullable
    static EphemeralKey fromString(@Nullable String rawJson) {
        return (EphemeralKey) AbstractEphemeralKey.fromString(rawJson, EphemeralKey.class);
    }

    @Nullable
    static EphemeralKey fromJson(@Nullable JSONObject jsonObject) {
        return (EphemeralKey) AbstractEphemeralKey.fromJson(jsonObject, EphemeralKey.class);
    }
}
