package com.stripe.android.elements

import android.os.Parcelable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.GooglePayConfiguration as EceGooglePayConfiguration
import com.stripe.android.elements.PaymentElement.Configuration.GooglePayConfiguration as PeGooglePayConfiguration

@Parcelize
internal data class CheckoutGooglePayConfiguration(
    val display: Display,
    val label: String?,
    val buttonType: PaymentSheet.GooglePayConfiguration.ButtonType,
    val additionalEnabledNetworks: List<String>,
) : Parcelable {
    enum class Display {
        Automatic,
        Never,
    }
}

@OptIn(CheckoutSessionPreview::class)
internal fun EceGooglePayConfiguration.Display.asCheckout(): CheckoutGooglePayConfiguration.Display = when (this) {
    EceGooglePayConfiguration.Display.Automatic -> CheckoutGooglePayConfiguration.Display.Automatic
    EceGooglePayConfiguration.Display.Never -> CheckoutGooglePayConfiguration.Display.Never
}

@OptIn(CheckoutSessionPreview::class)
internal fun PeGooglePayConfiguration.Display.asCheckout(): CheckoutGooglePayConfiguration.Display = when (this) {
    PeGooglePayConfiguration.Display.Automatic -> CheckoutGooglePayConfiguration.Display.Automatic
    PeGooglePayConfiguration.Display.Never -> CheckoutGooglePayConfiguration.Display.Never
}

@OptIn(CheckoutSessionPreview::class)
internal fun EceGooglePayConfiguration.ButtonType.asCheckout(): PaymentSheet.GooglePayConfiguration.ButtonType =
    when (this) {
        EceGooglePayConfiguration.ButtonType.Buy -> PaymentSheet.GooglePayConfiguration.ButtonType.Buy
        EceGooglePayConfiguration.ButtonType.Book -> PaymentSheet.GooglePayConfiguration.ButtonType.Book
        EceGooglePayConfiguration.ButtonType.Checkout -> PaymentSheet.GooglePayConfiguration.ButtonType.Checkout
        EceGooglePayConfiguration.ButtonType.Donate -> PaymentSheet.GooglePayConfiguration.ButtonType.Donate
        EceGooglePayConfiguration.ButtonType.Order -> PaymentSheet.GooglePayConfiguration.ButtonType.Order
        EceGooglePayConfiguration.ButtonType.Pay -> PaymentSheet.GooglePayConfiguration.ButtonType.Pay
        EceGooglePayConfiguration.ButtonType.Subscribe -> PaymentSheet.GooglePayConfiguration.ButtonType.Subscribe
        EceGooglePayConfiguration.ButtonType.Plain -> PaymentSheet.GooglePayConfiguration.ButtonType.Plain
    }

@OptIn(CheckoutSessionPreview::class)
internal fun PeGooglePayConfiguration.ButtonType.asCheckout(): PaymentSheet.GooglePayConfiguration.ButtonType =
    when (this) {
        PeGooglePayConfiguration.ButtonType.Buy -> PaymentSheet.GooglePayConfiguration.ButtonType.Buy
        PeGooglePayConfiguration.ButtonType.Book -> PaymentSheet.GooglePayConfiguration.ButtonType.Book
        PeGooglePayConfiguration.ButtonType.Checkout -> PaymentSheet.GooglePayConfiguration.ButtonType.Checkout
        PeGooglePayConfiguration.ButtonType.Donate -> PaymentSheet.GooglePayConfiguration.ButtonType.Donate
        PeGooglePayConfiguration.ButtonType.Order -> PaymentSheet.GooglePayConfiguration.ButtonType.Order
        PeGooglePayConfiguration.ButtonType.Pay -> PaymentSheet.GooglePayConfiguration.ButtonType.Pay
        PeGooglePayConfiguration.ButtonType.Subscribe -> PaymentSheet.GooglePayConfiguration.ButtonType.Subscribe
        PeGooglePayConfiguration.ButtonType.Plain -> PaymentSheet.GooglePayConfiguration.ButtonType.Plain
    }
