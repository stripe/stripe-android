package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

/**
 * Utility class to handle reading and writing potentially null
 * strings and objects to a {@link Parcel}.
 */
public class ParcelUtils {

    static final byte NULL = (byte) 0x00;
    static final byte NONNULL = (byte) 0x01;

    /**
     * Read a potentially null object from a {@link Parcel}. Advances
     * the parcel's data position by at least one byte to read the nullability
     * flag. Should not be used if the object was not written to Parcel with
     * {@link #writeNullableParcelable(Parcel, int, Parcelable)}
     *
     * @param in the {@link Parcel} from which to read
     * @param type the {@link Class} of the object, which must implement {@link Parcelable}
     * @param <T> the type of the object
     * @return the input object, or {@code null} if the check byte is {@link #NULL}
     */
    @Nullable
    public static <T extends Parcelable> T readNullableParcelable(Parcel in, Class<T> type) {
        if (in.readByte() == NONNULL) {
            return in.readParcelable(type.getClassLoader());
        } else {
            return null;
        }
    }

    /**
     * Read a potentially null String from a {@link Parcel}. Advances
     * the parcel's data position by at least one byte to read the nullability
     * flag. Should not be used if the String was not written to Parcel with
     * {@link #writeNullableString(Parcel, String)}
     *
     * @param in the {@link Parcel} from which to read
     * @return the next String stored, or {@code null} if the byte flag is {@link #NULL}
     */
    @Nullable
    public static String readNullableString(Parcel in) {
        if (in.readByte() == NONNULL) {
            return in.readString();
        } else {
            return null;
        }
    }

    /**
     * Write a potentially {@code null} item to an output {@link Parcel}, preceded by a byte
     * flag to signify whether or not the item is null.
     *
     * @param parcel the {@link Parcel} to which to write
     * @param flags any flags to pass along
     * @param parcelable the potentially {@code null} object to be written
     */
    public static void writeNullableParcelable(
            Parcel parcel,
            int flags,
            @Nullable Parcelable parcelable) {
        if (parcelable == null) {
            parcel.writeByte(NULL);
        } else {
            parcel.writeByte(NONNULL);
            parcel.writeParcelable(parcelable, flags);
        }
    }

    /**
     * Write a potentially {@code null} String to an output {@link Parcel},
     * preceded by a byte flag to indicate whether or not the item is {@code null}.
     *
     * @param parcel the {@link Parcel} to which to write
     * @param stringVal the String to be written, if not null
     */
    public static void writeNullableString(
            Parcel parcel,
            @Nullable String stringVal) {
        if (stringVal == null) {
            parcel.writeByte(NULL);
        } else {
            parcel.writeByte(NONNULL);
            parcel.writeString(stringVal);
        }
    }
}
