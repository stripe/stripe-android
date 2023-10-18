package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.model.PaymentSelection

@OptIn(ExperimentalCustomerSheetApi::class)
internal sealed interface CustomerSheetState {
    object Loading : CustomerSheetState

    data class Full(
        val config: CustomerSheet.Configuration?,
        val stripeIntent: StripeIntent?,
        val customerPaymentMethods: List<PaymentMethod>,
        val isGooglePayReady: Boolean,
        val paymentSelection: PaymentSelection?,
    ) : CustomerSheetState
}
