package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.StripeModel;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an Ephemeral Key that can be used temporarily for certain operations.
 */
abstract class AbstractEphemeralKey extends StripeModel implements Parcelable {

    static final String FIELD_CREATED = "created";
    static final String FIELD_EXPIRES = "expires";
    static final String FIELD_SECRET = "secret";
    static final String FIELD_LIVEMODE = "livemode";
    static final String FIELD_OBJECT = "object";
    static final String FIELD_ID = "id";
    static final String FIELD_ASSOCIATED_OBJECTS = "associated_objects";
    static final String FIELD_TYPE = "type";

    @NonNull final String mObjectId;
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
    AbstractEphemeralKey(@NonNull Parcel in) {
        mCreated = in.readLong();
        mObjectId = Objects.requireNonNull(in.readString());
        mExpires = in.readLong();
        mId = Objects.requireNonNull(in.readString());
        mLiveMode = in.readInt() == 1;
        mObject = Objects.requireNonNull(in.readString());
        mSecret = Objects.requireNonNull(in.readString());
        mType = Objects.requireNonNull(in.readString());
    }

    AbstractEphemeralKey(@NonNull Builder builder) {
        mCreated = builder.mCreated;
        mObjectId = Objects.requireNonNull(builder.mObjectId);
        mExpires = builder.mExpires;
        mId = Objects.requireNonNull(builder.mId);
        mLiveMode = builder.mLiveMode;
        mObject = Objects.requireNonNull(builder.mObject);
        mSecret = Objects.requireNonNull(builder.mSecret);
        mType = Objects.requireNonNull(builder.mType);
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put(FIELD_CREATED, mCreated);
        map.put(FIELD_EXPIRES, mExpires);
        map.put(FIELD_OBJECT, mObject);
        map.put(FIELD_ID, mId);
        map.put(FIELD_SECRET, mSecret);
        map.put(FIELD_LIVEMODE, mLiveMode);

        final List<Object> associatedObjectsList = new ArrayList<>();
        final Map<String, String> associatedObjectMap = new HashMap<>();
        associatedObjectMap.put(FIELD_ID, mObjectId);
        associatedObjectMap.put(FIELD_TYPE, mType);
        associatedObjectsList.add(associatedObjectMap);

        map.put(FIELD_ASSOCIATED_OBJECTS, associatedObjectsList);
        return map;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Write the object into a {@link Parcel}. Note that if the order of these
     * write operations is changed, an identical change must be made to
     * {@link #AbstractEphemeralKey(Parcel)} constructor above.
     *
     * @param out   a {@link Parcel} into which to write this object
     * @param flags any flags (unused) for writing this object
     */
    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeLong(mCreated);
        out.writeString(mObjectId);
        out.writeLong(mExpires);
        out.writeString(mId);
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
    protected static <TEphemeralKey extends AbstractEphemeralKey> TEphemeralKey fromJson(
            @NonNull JSONObject jsonObject, @NonNull Builder<TEphemeralKey> builder)
            throws JSONException {
        // Get the values from the associated objects array first element
        final JSONArray associatedObjectArray = jsonObject.getJSONArray(FIELD_ASSOCIATED_OBJECTS);
        final JSONObject typeObject = associatedObjectArray.getJSONObject(0);

        return builder
                .setCreated(jsonObject.getLong(FIELD_CREATED))
                .setExpires(jsonObject.getLong(FIELD_EXPIRES))
                .setId(jsonObject.getString(FIELD_ID))
                .setLiveMode(jsonObject.getBoolean(FIELD_LIVEMODE))
                .setObject(jsonObject.getString(FIELD_OBJECT))
                .setSecret(jsonObject.getString(FIELD_SECRET))
                .setType(typeObject.getString(FIELD_TYPE))
                .setObjectId(typeObject.getString(FIELD_ID))
                .build();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj
                || (obj instanceof AbstractEphemeralKey && typedEquals((AbstractEphemeralKey) obj));
    }

    private boolean typedEquals(@NonNull AbstractEphemeralKey ephemeralKey) {
        return ObjectUtils.equals(mObjectId, ephemeralKey.mObjectId)
                && mCreated == ephemeralKey.mCreated
                && mExpires == ephemeralKey.mExpires
                && ObjectUtils.equals(mId, ephemeralKey.mId)
                && mLiveMode == ephemeralKey.mLiveMode
                && ObjectUtils.equals(mObject, ephemeralKey.mObject)
                && ObjectUtils.equals(mSecret, ephemeralKey.mSecret)
                && ObjectUtils.equals(mType, ephemeralKey.mType);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mObjectId, mCreated, mExpires, mId, mLiveMode, mObject, mSecret,
                mType);
    }
    
    abstract static class Builder<T extends AbstractEphemeralKey> {
        @Nullable private String mObjectId;
        private long mCreated;
        private long mExpires;
        @Nullable private String mId;
        private boolean mLiveMode;
        @Nullable private String mObject;
        @Nullable private String mSecret;
        @Nullable private String mType;

        @NonNull
        Builder<T> setObjectId(@NonNull String objectId) {
            this.mObjectId = objectId;
            return this;
        }

        @NonNull
        Builder<T> setCreated(long created) {
            this.mCreated = created;
            return this;
        }

        @NonNull
        Builder<T> setExpires(long expires) {
            this.mExpires = expires;
            return this;
        }

        @NonNull
        Builder<T> setId(@NonNull String id) {
            this.mId = id;
            return this;
        }

        @NonNull
        Builder<T> setLiveMode(boolean liveMode) {
            this.mLiveMode = liveMode;
            return this;
        }

        @NonNull
        Builder<T> setObject(@NonNull String object) {
            this.mObject = object;
            return this;
        }

        @NonNull
        Builder<T> setSecret(@NonNull String secret) {
            this.mSecret = secret;
            return this;
        }

        @NonNull
        Builder<T> setType(@NonNull String type) {
            this.mType = type;
            return this;
        }

        @NonNull
        abstract T build();
    }

    abstract static class BuilderFactory<Builder extends AbstractEphemeralKey.Builder<?>> {
        @NonNull
        abstract Builder create();
    }
}
