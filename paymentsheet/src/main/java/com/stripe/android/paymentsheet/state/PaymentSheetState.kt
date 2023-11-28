package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PaymentSheetState(
    val config: PaymentSheet.Configuration,
    val stripeIntent: StripeIntent,
    val customerPaymentMethods: List<PaymentMethod>,
    val isGooglePayReady: Boolean,
    val linkState: LinkState?,
    val isEligibleForCardBrandChoice: Boolean,
    val paymentSelection: PaymentSelection?,
) : Parcelable {

    val hasPaymentOptions: Boolean
        get() = isGooglePayReady || linkState != null || customerPaymentMethods.isNotEmpty()
}
