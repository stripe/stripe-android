package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class AddressLauncherResult : Parcelable {

    internal abstract val resultCode: Int

    @Parcelize
    data class Succeeded(
        val address: AddressDetails
    ) : AddressLauncherResult() {
        override val resultCode: Int
            get() = Activity.RESULT_OK
    }

    @Parcelize
    object Canceled : AddressLauncherResult() {
        override val resultCode: Int
            get() = Activity.RESULT_CANCELED
    }
}
