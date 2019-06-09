package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

final class IssuingCardEphemeralKey extends AbstractEphemeralKey {
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

    private IssuingCardEphemeralKey(@NonNull Builder builder) {
        super(builder);
    }

    @NonNull
    String getIssuingCardId() {
        return mObjectId;
    }

    static final class Builder extends AbstractEphemeralKey.Builder<IssuingCardEphemeralKey> {
        @NonNull
        @Override
        IssuingCardEphemeralKey build() {
            return new IssuingCardEphemeralKey(this);
        }
    }

    static final class BuilderFactory extends AbstractEphemeralKey
            .BuilderFactory<AbstractEphemeralKey.Builder<IssuingCardEphemeralKey>> {
        @NonNull
        @Override
        Builder create() {
            return new Builder();
        }
    }
}
