package com.stripe.android.stripe3ds2.utils

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.bundleOf

internal object ParcelUtils {
    /**
     * @param source the source from which to parcel and unparcel an object
     *
     * @return the parceled and unparceled object
     */
    internal fun <Source : Parcelable> get(
        source: Source
    ): Source {
        val bundle = bundleOf(KEY to source)
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
