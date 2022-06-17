package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed class AddressElementResult(
    val resultCode: Int,
) : Parcelable {
    @Parcelize
    data class Succeeded(
        // TODO add the real return type here once we iron it out.
        val address: ShippingAddress
    ) : AddressElementResult(Activity.RESULT_OK)

    @Parcelize
    data class Failed(
        val error: Throwable,
    ) : AddressElementResult(Activity.RESULT_CANCELED)

    @Parcelize
    object Canceled : AddressElementResult(Activity.RESULT_CANCELED)

    internal companion object {
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        @JvmSynthetic
        internal fun fromIntent(intent: Intent?): PaymentOptionResult? {
            return intent?.getParcelableExtra(EXTRA_RESULT)
        }
    }
}