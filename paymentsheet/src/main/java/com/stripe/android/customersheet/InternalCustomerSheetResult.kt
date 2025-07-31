package com.stripe.android.customersheet

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.elements.customersheet.CustomerSheet.Companion.toPaymentOptionSelection
import com.stripe.android.elements.customersheet.CustomerSheet.Result
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed class InternalCustomerSheetResult : Parcelable {
    abstract fun toPublicResult(
        paymentOptionFactory: PaymentOptionFactory,
    ): Result

    /**
     * The customer selected a payment method
     */
    @Parcelize
    data class Selected internal constructor(
        val paymentSelection: PaymentSelection?
    ) : InternalCustomerSheetResult() {
        override fun toPublicResult(
            paymentOptionFactory: PaymentOptionFactory,
        ): Result {
            return Result.Selected(
                selection = paymentSelection?.toPaymentOptionSelection(paymentOptionFactory, canUseGooglePay = true)
            )
        }
    }

    /**
     * The customer canceled the sheet
     */
    @Parcelize
    data class Canceled(
        val paymentSelection: PaymentSelection?
    ) : InternalCustomerSheetResult() {
        override fun toPublicResult(
            paymentOptionFactory: PaymentOptionFactory,
        ): Result {
            return Result.Canceled(
                selection = paymentSelection?.toPaymentOptionSelection(paymentOptionFactory, canUseGooglePay = true)
            )
        }
    }

    /**
     * An error occurred when presenting the sheet
     */
    @Parcelize
    class Error internal constructor(
        val exception: Throwable
    ) : InternalCustomerSheetResult() {
        override fun toPublicResult(
            paymentOptionFactory: PaymentOptionFactory,
        ): Result {
            return Result.Failed(exception)
        }
    }

    internal companion object {
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        @JvmSynthetic
        internal fun fromIntent(intent: Intent?): InternalCustomerSheetResult? {
            @Suppress("DEPRECATION")
            return intent?.getParcelableExtra(EXTRA_RESULT)
        }
    }

    internal fun toBundle(): Bundle {
        return bundleOf(EXTRA_RESULT to this)
    }
}
