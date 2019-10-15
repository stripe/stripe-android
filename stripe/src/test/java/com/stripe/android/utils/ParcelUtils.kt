package com.stripe.android.utils

import android.os.Bundle
import android.os.Parcelable

object ParcelUtils {
    /**
     * @param source the source from which to parcel and unparcel a new object
     *
     * @return a new [Source] instance based on the original source
     */
    @JvmStatic
    fun <Source : Parcelable> create(
        source: Source
    ): Source {
        val bundle = Bundle()
        bundle.putParcelable(KEY, source)
        return requireNotNull(bundle.getParcelable(KEY))
    }

    private const val KEY = "parcelable"
}
