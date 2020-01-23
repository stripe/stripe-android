package com.stripe.android.utils

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

internal object ParcelUtils {
    /**
     * @param source the source from which to parcel and unparcel a new object
     *
     * @return a new [Source] instance based on the original source
     */
    internal fun <Source : Parcelable> create(
        source: Source
    ): Source {
        val bundle = Bundle()
        bundle.putParcelable(KEY, source)
        return requireNotNull(bundle.getParcelable(KEY))
    }

    /**
     * @param source the source from which to parcel and unparcel a new object
     * @param creator the [Parcelable.Creator]
     *
     * @return a new [SOURCE] instance based on the original source
     */
    @JvmStatic
    fun <SOURCE : Parcelable> copy(
        source: SOURCE,
        creator: Parcelable.Creator<SOURCE>
    ): SOURCE {
        val parcel = Parcel.obtain()
        source.writeToParcel(parcel, source.describeContents())
        parcel.setDataPosition(0)
        return creator.createFromParcel(parcel)
    }

    private const val KEY = "parcelable"
}
