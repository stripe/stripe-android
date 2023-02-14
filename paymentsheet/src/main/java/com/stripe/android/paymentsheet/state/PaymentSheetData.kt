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
import com.stripe.android.ui.core.PaymentSheetSetupFutureUsage
import kotlinx.parcelize.Parcelize

/**
 * Encapsulates all the information required to populate Payment Sheet. The [origin] allows access
 * to the resource that was used to fetch this information, such as a [StripeIntent].
 */
@Parcelize
internal data class PaymentSheetData(
    val id: String?,
    val mode: PaymentSheetMode,
    val setupFutureUsage: PaymentSheetSetupFutureUsage?,
    val supportedPaymentMethodTypes: List<String>,
    val unactivatedPaymentMethodTypes: List<String>,
    val isLiveMode: Boolean,
    val origin: PaymentSheetOrigin,
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

internal fun StripeIntent.toPaymentSheetOptions(): PaymentSheetData {
    return when (this) {
        is PaymentIntent -> toPaymentSheetOptions()
        is SetupIntent -> toPaymentSheetOptions()
    }
}

internal fun SetupIntent.toPaymentSheetOptions(): PaymentSheetData {
    return PaymentSheetData(
        id = id,
        mode = PaymentSheetMode.Setup(currency = null),
        setupFutureUsage = null,
        supportedPaymentMethodTypes = paymentMethodTypes,
        unactivatedPaymentMethodTypes = emptyList(),
        isLiveMode = isLiveMode,
        linkFundingSources = linkFundingSources,
        origin = PaymentSheetOrigin.Intent(
            clientSecret = SetupIntentClientSecret(clientSecret!!),
        ),
        shippingDetails = null,
    )
}

internal fun PaymentIntent.toPaymentSheetOptions(): PaymentSheetData {
    return PaymentSheetData(
        id = id,
        mode = PaymentSheetMode.Payment(
            amount = amount!!,
            currency = currency!!,
        ),
        setupFutureUsage = when (setupFutureUsage) {
            StripeIntent.Usage.OnSession -> PaymentSheetSetupFutureUsage.OnSession
            StripeIntent.Usage.OffSession -> PaymentSheetSetupFutureUsage.OffSession
            StripeIntent.Usage.OneTime, null -> null
        },
        supportedPaymentMethodTypes = paymentMethodTypes,
        unactivatedPaymentMethodTypes = emptyList(),
        isLiveMode = isLiveMode,
        linkFundingSources = linkFundingSources,
        origin = PaymentSheetOrigin.Intent(
            clientSecret = PaymentIntentClientSecret(clientSecret!!),
        ),
        shippingDetails = shipping,
    )
}
