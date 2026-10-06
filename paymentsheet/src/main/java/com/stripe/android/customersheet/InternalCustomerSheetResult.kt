package com.stripe.android.customersheet

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface InternalCustomerSheetResult : Parcelable {
    fun toPublicResult(
        paymentOptionSelectionFactory: PaymentOptionSelectionFactory,
    ): CustomerSheetResult

    /**
     * The customer selected a payment method
     */
    @Parcelize
    data class Selected internal constructor(
        val paymentSelection: PaymentSelection?,
        private val appearance: PaymentSheet.Appearance
    ) : InternalCustomerSheetResult {
        override fun toPublicResult(
            paymentOptionSelectionFactory: PaymentOptionSelectionFactory,
        ): CustomerSheetResult {
            return CustomerSheetResult.Selected(
                selection = paymentOptionSelectionFactory.create(
                    selection = paymentSelection,
                    canUseGooglePay = true,
                    appearance = appearance
                )
            )
        }
    }

    /**
     * The customer canceled the sheet
     */
    @Parcelize
    data class Canceled(
        val paymentSelection: PaymentSelection?,
        private val appearance: PaymentSheet.Appearance
    ) : InternalCustomerSheetResult {
        override fun toPublicResult(
            paymentOptionSelectionFactory: PaymentOptionSelectionFactory,
        ): CustomerSheetResult {
            return CustomerSheetResult.Canceled(
                selection = paymentOptionSelectionFactory.create(
                    selection = paymentSelection,
                    canUseGooglePay = true,
                    appearance = appearance
                )
            )
        }
    }

    /**
     * An error occurred when presenting the sheet
     */
    @Parcelize
    class Error internal constructor(
        val exception: Throwable
    ) : InternalCustomerSheetResult {
        override fun toPublicResult(
            paymentOptionSelectionFactory: PaymentOptionSelectionFactory,
        ): CustomerSheetResult {
            return CustomerSheetResult.Failed(exception)
        }
    }

    companion object {
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        @JvmSynthetic
        internal fun fromIntent(intent: Intent?): InternalCustomerSheetResult? {
            @Suppress("DEPRECATION")
            return intent?.getParcelableExtra(EXTRA_RESULT)
        }
    }

    fun toBundle(): Bundle {
        return bundleOf(EXTRA_RESULT to this)
    }
}
