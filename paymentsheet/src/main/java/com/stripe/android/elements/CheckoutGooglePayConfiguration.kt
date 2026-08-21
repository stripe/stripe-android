package com.stripe.android.elements

import android.os.Parcelable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.parcelize.Parcelize
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.GooglePayConfiguration as EceGooglePayConfiguration
import com.stripe.android.elements.PaymentElement.Configuration.GooglePayConfiguration as PeGooglePayConfiguration

@Parcelize
internal data class CheckoutGooglePayConfiguration(
    val display: Display,
    val label: String?,
    val buttonType: ButtonType,
    val additionalEnabledNetworks: List<String>,
) : Parcelable {
    enum class Display {
        Automatic,
        Never,
    }

    enum class ButtonType {
        Buy,
        Book,
        Checkout,
        Donate,
        Order,
        Pay,
        Subscribe,
        Plain,
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
internal fun EceGooglePayConfiguration.ButtonType.asCheckout(): CheckoutGooglePayConfiguration.ButtonType =
    when (this) {
        EceGooglePayConfiguration.ButtonType.Buy -> CheckoutGooglePayConfiguration.ButtonType.Buy
        EceGooglePayConfiguration.ButtonType.Book -> CheckoutGooglePayConfiguration.ButtonType.Book
        EceGooglePayConfiguration.ButtonType.Checkout -> CheckoutGooglePayConfiguration.ButtonType.Checkout
        EceGooglePayConfiguration.ButtonType.Donate -> CheckoutGooglePayConfiguration.ButtonType.Donate
        EceGooglePayConfiguration.ButtonType.Order -> CheckoutGooglePayConfiguration.ButtonType.Order
        EceGooglePayConfiguration.ButtonType.Pay -> CheckoutGooglePayConfiguration.ButtonType.Pay
        EceGooglePayConfiguration.ButtonType.Subscribe -> CheckoutGooglePayConfiguration.ButtonType.Subscribe
        EceGooglePayConfiguration.ButtonType.Plain -> CheckoutGooglePayConfiguration.ButtonType.Plain
    }

@OptIn(CheckoutSessionPreview::class)
internal fun PeGooglePayConfiguration.ButtonType.asCheckout(): CheckoutGooglePayConfiguration.ButtonType =
    when (this) {
        PeGooglePayConfiguration.ButtonType.Buy -> CheckoutGooglePayConfiguration.ButtonType.Buy
        PeGooglePayConfiguration.ButtonType.Book -> CheckoutGooglePayConfiguration.ButtonType.Book
        PeGooglePayConfiguration.ButtonType.Checkout -> CheckoutGooglePayConfiguration.ButtonType.Checkout
        PeGooglePayConfiguration.ButtonType.Donate -> CheckoutGooglePayConfiguration.ButtonType.Donate
        PeGooglePayConfiguration.ButtonType.Order -> CheckoutGooglePayConfiguration.ButtonType.Order
        PeGooglePayConfiguration.ButtonType.Pay -> CheckoutGooglePayConfiguration.ButtonType.Pay
        PeGooglePayConfiguration.ButtonType.Subscribe -> CheckoutGooglePayConfiguration.ButtonType.Subscribe
        PeGooglePayConfiguration.ButtonType.Plain -> CheckoutGooglePayConfiguration.ButtonType.Plain
    }
