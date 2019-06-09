package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

final class CustomerEphemeralKey extends AbstractEphemeralKey {
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

    private CustomerEphemeralKey(@NonNull Parcel in) {
        super(in);
    }

    private CustomerEphemeralKey(@NonNull Builder builder) {
        super(builder);
    }

    @NonNull
    String getCustomerId() {
        return mObjectId;
    }

    @NonNull
    static CustomerEphemeralKey fromJson(@NonNull JSONObject jsonObject) throws JSONException {
        return AbstractEphemeralKey.fromJson(jsonObject, new Builder());
    }

    static final class Builder extends AbstractEphemeralKey.Builder<CustomerEphemeralKey> {
        @NonNull
        @Override
        CustomerEphemeralKey build() {
            return new CustomerEphemeralKey(this);
        }
    }

    static final class BuilderFactory extends AbstractEphemeralKey
            .BuilderFactory<AbstractEphemeralKey.Builder<CustomerEphemeralKey>> {
        @NonNull
        @Override
        Builder create() {
            return new Builder();
        }
    }
}
