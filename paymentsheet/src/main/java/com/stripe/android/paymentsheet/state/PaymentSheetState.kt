package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.parcelize.Parcelize

internal sealed interface PaymentSheetState : Parcelable {

    @Parcelize
    object Loading : PaymentSheetState

    @Parcelize
    data class Full(
        val config: CommonConfiguration,
        val customer: CustomerState?,
        val linkState: LinkState?,
        val paymentSelection: PaymentSelection?,
        val validationError: PaymentSheetLoadingException?,
        val paymentMethodMetadata: PaymentMethodMetadata,
    ) : PaymentSheetState {
        constructor(state: PaymentElementLoader.State) : this(
            config = state.config,
            customer = state.customer,
            linkState = state.linkState,
            paymentSelection = state.paymentSelection,
            validationError = state.validationError,
            paymentMethodMetadata = state.paymentMethodMetadata,
        )

        val showSavedPaymentMethods: Boolean
            get() = (customer != null && customer.paymentMethods.isNotEmpty()) || paymentMethodMetadata.isGooglePayReady

        val stripeIntent: StripeIntent
            get() = paymentMethodMetadata.stripeIntent
    }
}
