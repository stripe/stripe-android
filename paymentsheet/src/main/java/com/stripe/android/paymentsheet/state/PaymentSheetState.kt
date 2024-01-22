package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
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
        val stripeIntent: StripeIntent,
        val customerPaymentMethods: List<PaymentMethod>,
        val isGooglePayReady: Boolean,
        val linkState: LinkState?,
        val isEligibleForCardBrandChoice: Boolean,
        val paymentSelection: PaymentSelection?,
    ) : PaymentSheetState {

        val showSavedPaymentMethods: Boolean
            get() = customerPaymentMethods.isNotEmpty() || isGooglePayReady
    }
}
