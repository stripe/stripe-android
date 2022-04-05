package com.stripe.android.core.utils

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.google.common.truth.Truth.assertThat

internal object ParcelUtils {
    /**
     * @param source the source from which to parcel and unparcel a new object
     *
     * @return a new [Source] instance based on the original source
     */
    internal fun <Source : Parcelable> create(
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

    internal fun verifyParcelRoundtrip(expected: Parcelable) {
        val actual: Parcelable = createParcelRoundtrip(expected)

        assertThat(actual)
            .isEqualTo(expected)
    }

    private inline fun <reified T : Parcelable> createParcelRoundtrip(
        source: Parcelable
    ): T {
        val bundle = bundleOf(KEY to source)

        return requireNotNull(
            copy(bundle, Bundle.CREATOR).getParcelable(KEY)
        )
    }

    private const val KEY = "parcelable"
}
