package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheetOrigin
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.ui.core.PaymentSheetMode
import com.stripe.android.ui.core.PaymentSheetSetupFutureUse
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PaymentSheetData(
    val id: String?,
    val origin: PaymentSheetOrigin,
    val mode: PaymentSheetMode,
    val setupFutureUse: PaymentSheetSetupFutureUse?,
    val paymentMethodTypes: List<String>,
    val unactivatedPaymentMethods: List<String>,
    val isLiveMode: Boolean,
    val linkFundingSources: List<String>,
    val shippingDetails: PaymentIntent.Shipping?,
) : Parcelable {

    val clientSecret: ClientSecret?
        get() = (origin as? PaymentSheetOrigin.Intent)?.clientSecret

    val amount: Long?
        get() = when (mode) {
            is PaymentSheetMode.Payment -> mode.amount
            is PaymentSheetMode.Setup -> null
        }

    val currency: String?
        get() = when (mode) {
            is PaymentSheetMode.Payment -> mode.currency
            is PaymentSheetMode.Setup -> mode.currency
        }
}

internal fun StripeIntent.toPaymentSheetData(): PaymentSheetData {
    val clientSecret = when (this) {
        is PaymentIntent -> PaymentIntentClientSecret(this.clientSecret!!)
        is SetupIntent -> SetupIntentClientSecret(this.clientSecret!!)
    }

    return PaymentSheetData(
        id = id,
        origin = PaymentSheetOrigin.Intent(clientSecret),
        mode = mode(),
        setupFutureUse = setupFutureUse(),
        paymentMethodTypes = paymentMethodTypes,
        unactivatedPaymentMethods = unactivatedPaymentMethods,
        isLiveMode = isLiveMode,
        linkFundingSources = linkFundingSources,
        shippingDetails = (this as? PaymentIntent)?.shipping,
    )
}
