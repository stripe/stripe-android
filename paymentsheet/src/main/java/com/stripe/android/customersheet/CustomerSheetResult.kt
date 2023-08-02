package com.stripe.android.customersheet

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.view.ActivityStarter
import com.stripe.android.model.PaymentMethod as StripePaymentMethod

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
        val selection: PaymentOptionSelection?
    ) : CustomerSheetResult()

    /**
     * The customer canceled the sheet
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Canceled internal constructor(
        val selection: PaymentOptionSelection?
    ) : CustomerSheetResult()

    /**
     * An error occurred when presenting the sheet
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Error internal constructor(
        val exception: Throwable
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
 * @param paymentOption, contains the drawable and label to display
 */
@ExperimentalCustomerSheetApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PaymentOptionSelection private constructor(
    open val paymentOption: PaymentOption
) {

    /**
     * A Stripe payment method was selected.
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class PaymentMethod internal constructor(
        val paymentMethod: StripePaymentMethod,
        override val paymentOption: PaymentOption,
    ) : PaymentOptionSelection(paymentOption)

    /**
     * Google Pay is the selected payment option.
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class GooglePay internal constructor(
        override val paymentOption: PaymentOption,
    ) : PaymentOptionSelection(paymentOption)
}
