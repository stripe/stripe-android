package com.stripe.android.customersheet

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.customersheet.CustomerSheet.Companion.toPaymentOptionSelection
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalCustomerSheetApi::class)
internal sealed class InternalCustomerSheetResult : Parcelable {
    abstract fun toPublicResult(
        paymentOptionFactory: PaymentOptionFactory,
    ): CustomerSheetResult

    /**
     * The customer selected a payment method
     */
    @Parcelize
    data class Selected internal constructor(
        val paymentSelection: PaymentSelection?
    ) : InternalCustomerSheetResult() {
        override fun toPublicResult(
            paymentOptionFactory: PaymentOptionFactory,
        ): CustomerSheetResult {
            return CustomerSheetResult.Selected(
                selection = paymentSelection?.toPaymentOptionSelection(paymentOptionFactory)
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
        ): CustomerSheetResult {
            return CustomerSheetResult.Canceled(
                selection = paymentSelection?.toPaymentOptionSelection(paymentOptionFactory)
            )
        }
    }

    /**
     * An error occurred when presenting the sheet
     */
    @Parcelize
    class Error internal constructor(
        val exception: Exception
    ) : InternalCustomerSheetResult() {
        override fun toPublicResult(
            paymentOptionFactory: PaymentOptionFactory,
        ): CustomerSheetResult {
            return CustomerSheetResult.Error(exception)
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
