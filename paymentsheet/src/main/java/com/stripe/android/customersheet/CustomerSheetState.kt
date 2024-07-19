package com.stripe.android.customersheet

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

@OptIn(ExperimentalCustomerSheetApi::class)
internal sealed interface CustomerSheetState {
    object Loading : CustomerSheetState

    data class Full(
        val config: CustomerSheet.Configuration,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val customerPaymentMethods: List<PaymentMethod>,
        val supportedPaymentMethods: List<SupportedPaymentMethod>,
        val paymentSelection: PaymentSelection?,
        val validationError: Throwable?,
    ) : CustomerSheetState
}
