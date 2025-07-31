package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.os.Parcelable
import com.stripe.android.elements.AddressDetails
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

sealed class AddressLauncherResult : Parcelable {

    internal abstract val resultCode: Int

    @Parcelize
    @Poko
    class Succeeded internal constructor(
        val address: AddressDetails
    ) : AddressLauncherResult() {
        override val resultCode: Int
            get() = Activity.RESULT_OK
    }

    @Parcelize
    @Poko
    class Canceled internal constructor(
        @Suppress("unused") private val irrelevantValueForGeneratedCode: Boolean = true
    ) : AddressLauncherResult() {
        override val resultCode: Int
            get() = Activity.RESULT_CANCELED
    }
}
