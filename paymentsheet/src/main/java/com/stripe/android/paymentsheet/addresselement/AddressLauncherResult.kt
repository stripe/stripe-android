package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

sealed class AddressLauncherResult(
    val resultCode: Int
) : Parcelable {
    @Parcelize
    data class Succeeded(
        val address: AddressDetails
    ) : AddressLauncherResult(Activity.RESULT_OK)

    @Parcelize
    object Canceled : AddressLauncherResult(Activity.RESULT_CANCELED)

    internal companion object {
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        @JvmSynthetic
        internal fun fromIntent(intent: Intent?): PaymentOptionResult? {
            return intent?.getParcelableExtra(EXTRA_RESULT)
        }
    }
}
