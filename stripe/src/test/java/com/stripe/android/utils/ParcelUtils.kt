package com.stripe.android.utils

import android.os.Parcel
import android.os.Parcelable

object ParcelUtils {
    /**
     * @param source the source from which to parcel and unparcel a new object
     * @param creator the [Parcelable.Creator]
     * @return a new [Source] instance based on the original source
     */
    @JvmStatic
    fun <Source : Parcelable> create(
        source: Source,
        creator: Parcelable.Creator<Source>
    ): Source {
        val parcel = Parcel.obtain()
        source.writeToParcel(parcel, source.describeContents())
        parcel.setDataPosition(0)
        return creator.createFromParcel(parcel)
    }
}
