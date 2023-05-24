package com.stripe.android.customersheet

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed class InternalCustomerSheetResult : Parcelable {
    /**
     * The customer selected a payment method
     */
    @Parcelize
    class Selected internal constructor(
        val paymentMethodId: String,
        val drawableResourceId: Int,
        val label: String,
    ) : InternalCustomerSheetResult()

    /**
     * The customer canceled the sheet
     */
    @Parcelize
    object Canceled : InternalCustomerSheetResult()

    /**
     * An error occurred when presenting the sheet
     */
    @Parcelize
    class Error internal constructor(
        val exception: Exception
    ) : InternalCustomerSheetResult()

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
