package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed class PaymentOptionResult(
    val resultCode: Int
) : Parcelable {
    fun toBundle(): Bundle {
        return bundleOf(EXTRA_RESULT to this)
    }

    sealed class Succeeded : PaymentOptionResult(Activity.RESULT_OK) {
        @Parcelize
        object GooglePay : Succeeded()

        @Parcelize
        data class New(
            val params: PaymentMethodCreateParams,
            val shouldSave: Boolean
        ) : Succeeded()

        @Parcelize
        data class Saved(
            val paymentMethod: PaymentMethod
        ) : Succeeded()
    }

    @Parcelize
    data class Failed(
        val error: Throwable
    ) : PaymentOptionResult(Activity.RESULT_CANCELED)

    @Parcelize
    data class Cancelled(
        val mostRecentError: Throwable?
    ) : PaymentOptionResult(Activity.RESULT_CANCELED)

    internal companion object {
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        @JvmSynthetic
        internal fun fromIntent(intent: Intent): PaymentOptionResult? {
            return intent.getParcelableExtra(EXTRA_RESULT)
        }
    }
}
