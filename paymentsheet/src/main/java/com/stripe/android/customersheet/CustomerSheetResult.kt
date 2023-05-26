package com.stripe.android.customersheet

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.view.ActivityStarter

@ExperimentalCustomerSheetApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CustomerSheetResult {
    /**
     * The customer selected a payment method
     * @param selection the [PaymentOptionSelection] the customer selected from the [CustomerSheet]
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Selected internal constructor(
        val selection: PaymentOptionSelection
    ) : CustomerSheetResult()

    /**
     * The customer canceled the sheet
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Canceled internal constructor() : CustomerSheetResult() {
        override fun equals(other: Any?): Boolean = this === other
        override fun hashCode(): Int = System.identityHashCode(this)
    }

    /**
     * An error occurred when presenting the sheet
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Error internal constructor(
        val exception: Exception
    ) : CustomerSheetResult()

    internal companion object {
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA
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
data class PaymentOptionSelection internal constructor(
    val paymentMethodId: String,
    val paymentOption: PaymentOption,
)
