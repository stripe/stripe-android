package com.stripe.android.paymentsheet.wallet

import androidx.annotation.RestrictTo
import com.stripe.android.ExperimentalSavedPaymentMethodsApi
import com.stripe.android.paymentsheet.model.PaymentOption

@ExperimentalSavedPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class SavedPaymentMethodsSheetResult {
    /**
     * The customer selected a payment method
     * @param selection, the [PaymentOptionSelection] the customer selected from the
     * [SavedPaymentMethodsSheet]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Selected internal constructor(
        val selection: PaymentOptionSelection
    ) : SavedPaymentMethodsSheetResult()

    /**
     * The customer canceled the sheet
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Canceled internal constructor() : SavedPaymentMethodsSheetResult() {
        override fun equals(other: Any?): Boolean = this === other
        override fun hashCode(): Int = System.identityHashCode(this)
    }

    /**
     * An error occurred when presenting the sheet
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Error internal constructor(
        val exception: Exception
    ) : SavedPaymentMethodsSheetResult()
}

/**
 * The customer's payment method selection
 * @param paymentMethodId, the Stripe payment method ID
 * @param paymentOption, contains the drawable and label to display
 */
@ExperimentalSavedPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentOptionSelection internal constructor(
    val paymentMethodId: String,
    val paymentOption: PaymentOption,
)
