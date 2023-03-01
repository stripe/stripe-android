package com.stripe.android.paymentsheet.wallet.sheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed class SavedPaymentMethodsSheetResult(
    val resultCode: Int,
) : Parcelable {
    @Parcelize
    data class Succeeded(
        val paymentMethod: PaymentMethod,
    ) : SavedPaymentMethodsSheetResult(Activity.RESULT_OK)

    @Parcelize
    data class Failed(
        val error: Throwable,
    ) : SavedPaymentMethodsSheetResult(Activity.RESULT_CANCELED)

    @Parcelize
    object Canceled : SavedPaymentMethodsSheetResult(Activity.RESULT_CANCELED)

    fun toBundle(): Bundle {
        return bundleOf(EXTRA_RESULT to this)
    }

    internal companion object {
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        @JvmSynthetic
        internal fun fromIntent(intent: Intent?): SavedPaymentMethodsSheetResult? {
            @Suppress("DEPRECATION")
            return intent?.getParcelableExtra(EXTRA_RESULT)
        }
    }
}
