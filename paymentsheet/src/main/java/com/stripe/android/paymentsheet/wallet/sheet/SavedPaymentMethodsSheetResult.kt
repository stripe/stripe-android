package com.stripe.android.paymentsheet.wallet.sheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.core.os.bundleOf
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.view.ActivityStarter
import java.lang.Exception

sealed class SavedPaymentMethodsSheetResult(
    val resultCode: Int,
) {
    data class Success(
        val paymentOption: PaymentOption?
    ): SavedPaymentMethodsSheetResult(Activity.RESULT_OK)

    data class Error(
        val exception: Exception
    ) : SavedPaymentMethodsSheetResult(Activity.RESULT_CANCELED)

    object Canceled : SavedPaymentMethodsSheetResult(Activity.RESULT_CANCELED)

    fun toBundle(): Bundle {
        return bundleOf(ActivityStarter.Result.EXTRA to this)
    }

    companion object {
        fun fromIntent(intent: Intent?): SavedPaymentMethodsSheetResult? {
            @Suppress("DEPRECATION")
            return intent?.getParcelableExtra(ActivityStarter.Result.EXTRA)
        }
    }
}

data class PaymentOptionSelection(
    val paymentMethodId: String,
    val paymentOption: PaymentOption
)