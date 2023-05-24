package com.stripe.android.customersheet

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalCustomerSheetApi::class)
internal sealed class InternalCustomerSheetResult : Parcelable {
    abstract fun toPublicResult(): CustomerSheetResult

    /**
     * The customer selected a payment method
     */
    @Parcelize
    class Selected internal constructor(
        val paymentMethodId: String,
        val drawableResourceId: Int,
        val label: String,
    ) : InternalCustomerSheetResult() {
        @Suppress("DEPRECATION")
        override fun toPublicResult(): CustomerSheetResult {
            return CustomerSheetResult.Selected(
                selection = PaymentOptionSelection(
                    paymentMethodId = paymentMethodId,
                    // Use [PaymentOptionFactory]
                    paymentOption = PaymentOption(
                        drawableResourceId = drawableResourceId,
                        label = label,
                    )
                )
            )
        }
    }

    /**
     * The customer canceled the sheet
     */
    @Parcelize
    object Canceled : InternalCustomerSheetResult() {
        override fun toPublicResult(): CustomerSheetResult {
            return CustomerSheetResult.Canceled()
        }
    }

    /**
     * An error occurred when presenting the sheet
     */
    @Parcelize
    class Error internal constructor(
        val exception: Exception
    ) : InternalCustomerSheetResult() {
        override fun toPublicResult(): CustomerSheetResult {
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
