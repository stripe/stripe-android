package com.stripe.android.customersheet

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@ExperimentalCustomerSheetApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CustomerSheetResult : Parcelable {
    /**
     * The customer selected a payment method
     * @param selection, the [PaymentOptionSelection] the customer selected from the [CustomerSheet]
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    class Selected internal constructor(
        val selection: PaymentOptionSelection
    ) : CustomerSheetResult()

    /**
     * The customer canceled the sheet
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    object Canceled : CustomerSheetResult()

    /**
     * An error occurred when presenting the sheet
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    class Error internal constructor(
        val exception: Exception
    ) : CustomerSheetResult()

    internal companion object {
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        @JvmSynthetic
        internal fun fromIntent(intent: Intent?): CustomerSheetResult? {
            @Suppress("DEPRECATION")
            return intent?.getParcelableExtra(EXTRA_RESULT)
        }
    }

    internal fun toBundle(): Bundle {
        return bundleOf(EXTRA_RESULT to this)
    }
}

/**
 * The customer's payment option selection
 * @param paymentMethodId, the Stripe payment method ID
 * @param paymentOption, contains the drawable and label to display
 */
@ExperimentalCustomerSheetApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class PaymentOptionSelection internal constructor(
    val paymentMethodId: String,
    val paymentOption: PaymentOption,
) : Parcelable
