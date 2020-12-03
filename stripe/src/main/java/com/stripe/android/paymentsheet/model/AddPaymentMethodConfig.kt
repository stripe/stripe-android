package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize

/**
 * Configuration data for `PaymentSheetAddCardFragment`.
 */
@Parcelize
internal data class AddPaymentMethodConfig(
    val paymentIntent: PaymentIntent,
    val paymentMethods: List<PaymentMethod>,
    val isGooglePayReady: Boolean
) : Parcelable {
    val shouldShowGooglePayButton: Boolean
        get() = isGooglePayReady && paymentMethods.isEmpty()
}
