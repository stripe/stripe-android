package com.stripe.android.utils;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public final class ParcelUtils {
    /**
     * @param source  the source from which to parcel and unparcel a new object
     * @param creator the {@link Parcelable.Creator}
     * @return a new {@link Source} instance based on the original source
     */
    @NonNull
    public static <Source extends Parcelable> Source create(
            @NonNull Source source,
            @NonNull Parcelable.Creator<Source> creator) {
        final Parcel parcel = Parcel.obtain();
        source.writeToParcel(parcel, source.describeContents());
        parcel.setDataPosition(0);
        return creator.createFromParcel(parcel);
    }
}
