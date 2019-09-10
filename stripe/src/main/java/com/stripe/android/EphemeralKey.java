package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.model.StripeModel;
import com.stripe.android.utils.ObjectUtils;

import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an Ephemeral Key that can be used temporarily for API operations that typically
 * require a secret key.
 *
 * See <a href="https://stripe.com/docs/mobile/android/standard#prepare-your-api">
 * Using Android Standard UI Components - Prepare your API</a> for more details on ephemeral keys.
 */
abstract class EphemeralKey extends StripeModel implements Parcelable {

    private static final String FIELD_CREATED = "created";
    private static final String FIELD_EXPIRES = "expires";
    private static final String FIELD_SECRET = "secret";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_ID = "id";
    private static final String FIELD_ASSOCIATED_OBJECTS = "associated_objects";
    private static final String FIELD_TYPE = "type";

    @NonNull final String objectId;
    private final long mCreated;
    private final long mExpires;
    @NonNull private final String mId;
    private final boolean mLiveMode;
    @NonNull private final String mObject;
    @NonNull private final String mSecret;
    @NonNull private final String mType;

    /**
     * Parcel constructor for this {@link Parcelable} class.
     * Note that if you change the order of these read values,
     * you must change the order in which they are written in
     * {@link #writeToParcel(Parcel, int)} below.
     *
     * @param in the {@link Parcel} in which this Ephemeral Key has been stored.
     */
    EphemeralKey(@NonNull Parcel in) {
        mCreated = in.readLong();
        objectId = Objects.requireNonNull(in.readString());
        mExpires = in.readLong();
        mId = Objects.requireNonNull(in.readString());
        mLiveMode = in.readInt() == 1;
        mObject = Objects.requireNonNull(in.readString());
        mSecret = Objects.requireNonNull(in.readString());
        mType = Objects.requireNonNull(in.readString());
    }

    EphemeralKey(
            long created,
            @NonNull String objectId,
            long expires,
            @NonNull String id,
            boolean liveMode,
            @NonNull String object,
            @NonNull String secret,
            @NonNull String type) {
        mCreated = created;
        this.objectId = objectId;
        mExpires = expires;
        mId = id;
        mLiveMode = liveMode;
        mObject = object;
        mSecret = secret;
        mType = type;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Write the object into a {@link Parcel}. Note that if the order of these
     * write operations is changed, an identical change must be made to
     * {@link #EphemeralKey(Parcel)} constructor above.
     *
     * @param out   a {@link Parcel} into which to write this object
     * @param flags any flags (unused) for writing this object
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mCreated);
        out.writeString(objectId);
        out.writeLong(mExpires);
        out.writeString(mId);
        // There is no writeBoolean
        out.writeInt(mLiveMode ? 1 : 0);
        out.writeString(mObject);
        out.writeString(mSecret);
        out.writeString(mType);
    }

    long getCreated() {
        return mCreated;
    }

    long getExpires() {
        return mExpires;
    }

    @NonNull
    String getId() {
        return mId;
    }

    boolean isLiveMode() {
        return mLiveMode;
    }

    @NonNull
    String getObject() {
        return mObject;
    }

    @NonNull
    String getSecret() {
        return mSecret;
    }

    @NonNull
    String getType() {
        return mType;
    }

    @NonNull
    protected static <TEphemeralKey extends EphemeralKey> TEphemeralKey fromJson(
            @NonNull JSONObject jsonObject, @NonNull Factory<TEphemeralKey> factory)
            throws JSONException {
        final long created = jsonObject.getLong(FIELD_CREATED);
        final long expires = jsonObject.getLong(FIELD_EXPIRES);
        final String id = jsonObject.getString(FIELD_ID);
        final boolean liveMode = jsonObject.getBoolean(FIELD_LIVEMODE);
        final String object = jsonObject.getString(FIELD_OBJECT);
        final String secret = jsonObject.getString(FIELD_SECRET);

        // Get the values from the associated objects array first element
        final JSONArray associatedObjectArray = jsonObject.getJSONArray(FIELD_ASSOCIATED_OBJECTS);
        final JSONObject typeObject = associatedObjectArray.getJSONObject(0);
        final String type = typeObject.getString(FIELD_TYPE);
        final String objectId = typeObject.getString(FIELD_ID);

        return factory.create(created, objectId, expires, id, liveMode, object, secret, type);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj
                || (obj instanceof EphemeralKey && typedEquals((EphemeralKey) obj));
    }

    private boolean typedEquals(@NonNull EphemeralKey ephemeralKey) {
        return Objects.equals(objectId, ephemeralKey.objectId)
                && mCreated == ephemeralKey.mCreated
                && mExpires == ephemeralKey.mExpires
                && Objects.equals(mId, ephemeralKey.mId)
                && mLiveMode == ephemeralKey.mLiveMode
                && Objects.equals(mObject, ephemeralKey.mObject)
                && Objects.equals(mSecret, ephemeralKey.mSecret)
                && Objects.equals(mType, ephemeralKey.mType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectId, mCreated, mExpires, mId, mLiveMode, mObject, mSecret,
                mType);
    }

    abstract static class Factory<TEphemeralKey extends EphemeralKey> {
        @NonNull
        abstract TEphemeralKey create(long created, @NonNull String objectId, long expires,
                                      @NonNull String id, boolean liveMode, @NonNull String object,
                                      @NonNull String secret, @NonNull String type);
    }
}
