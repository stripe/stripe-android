package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

final class CustomerEphemeralKey extends EphemeralKey {
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

    private CustomerEphemeralKey(long created, @NonNull String customerId, long expires,
                                 @NonNull String id, boolean liveMode, @NonNull String object,
                                 @NonNull String secret, @NonNull String type) {
        super(created, customerId, expires, id, liveMode, object, secret, type);
    }

    @NonNull
    String getCustomerId() {
        return mObjectId;
    }

    @NonNull
    static CustomerEphemeralKey fromJson(@NonNull JSONObject jsonObject) throws JSONException {
        return EphemeralKey.fromJson(jsonObject, new Factory());
    }

    static final class Factory extends EphemeralKey.Factory<CustomerEphemeralKey> {
        @NonNull
        @Override
        CustomerEphemeralKey create(long created, @NonNull String objectId, long expires,
                                       @NonNull String id, boolean liveMode, @NonNull String object,
                                       @NonNull String secret, @NonNull String type) {
            return new CustomerEphemeralKey(created, objectId, expires, id, liveMode, object,
                    secret, type);
        }
    }
}
