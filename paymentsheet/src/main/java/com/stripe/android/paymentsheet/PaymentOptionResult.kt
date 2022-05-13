package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed class PaymentOptionResult(
    val resultCode: Int,
    open val paymentMethods: List<PaymentMethod>? = null
) : Parcelable {
    fun toBundle(): Bundle {
        return bundleOf(EXTRA_RESULT to this)
    }

    @Parcelize
    data class Succeeded(val paymentSelection: PaymentSelection) :
        PaymentOptionResult(Activity.RESULT_OK)

    @Parcelize
    data class Failed(
        val error: Throwable
    ) : PaymentOptionResult(Activity.RESULT_CANCELED)

    @Parcelize
    data class Canceled(
        val mostRecentError: Throwable?,
        // The user could have removed a payment method and canceled the flow. We should update
        // the list of paymentMethods
        override val paymentMethods: List<PaymentMethod>? = null
    ) : PaymentOptionResult(Activity.RESULT_CANCELED, paymentMethods)

    internal companion object {
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        @JvmSynthetic
        internal fun fromIntent(intent: Intent?): PaymentOptionResult? {
            return intent?.getParcelableExtra(EXTRA_RESULT)
        }
    }
}
