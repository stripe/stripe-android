package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.parcelize.Parcelize

internal sealed interface PaymentSheetState : Parcelable {

    @Parcelize
    object Loading : PaymentSheetState

    @Parcelize
    data class Full(
        val config: PaymentSheet.Configuration,
        val customer: CustomerState?,
        val linkState: LinkState?,
        val paymentSelection: PaymentSelection?,
        val validationError: PaymentSheetLoadingException?,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val initializationMode: PaymentSheet.InitializationMode,
    ) : PaymentSheetState {
        val showSavedPaymentMethods: Boolean
            get() = (customer != null && customer.paymentMethods.isNotEmpty()) || paymentMethodMetadata.isGooglePayReady

        val stripeIntent: StripeIntent
            get() = paymentMethodMetadata.stripeIntent
    }
}
