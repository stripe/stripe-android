package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

final class IssuingCardEphemeralKey extends EphemeralKey {
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

    private IssuingCardEphemeralKey(long created, @NonNull String customerId, long expires,
                                    @NonNull String id, boolean liveMode, @NonNull String object,
                                    @NonNull String secret, @NonNull String type) {
        super(created, customerId, expires, id, liveMode, object, secret, type);
    }

    @NonNull
    String getIssuingCardId() {
        return mObjectId;
    }

    static final class Factory extends EphemeralKey.Factory<IssuingCardEphemeralKey> {
        @NonNull
        @Override
        IssuingCardEphemeralKey create(long created, @NonNull String objectId, long expires,
                                       @NonNull String id, boolean liveMode, @NonNull String object,
                                       @NonNull String secret, @NonNull String type) {
            return new IssuingCardEphemeralKey(created, objectId, expires, id, liveMode, object,
                    secret, type);
        }
    }
}
