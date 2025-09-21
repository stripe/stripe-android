package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.twostepcheckout

import androidx.compose.ui.graphics.toArgb
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.model.PaymentOption

data class TwoStepCheckoutViewState(
    val isProcessing: Boolean = false,
    val isError: Boolean = false,
    val cartState: CartState = CartState.default,
    val paymentInfo: PaymentInfo? = null,
    val status: String? = null,
    val paymentOption: PaymentOption? = null,
    val didComplete: Boolean = false,
    val showFinalCheckout: Boolean = false,
    val shippingAddress: AddressDetails? = null,
    val showConfiguration: Boolean = true,
    val customerEmail: String = "email@email.com",
    val enableInlineOtp: Boolean = false,
    val isFlowControllerConfigured: Boolean = false,
) {

    val isPaymentMethodButtonEnabled: Boolean
        get() = !isProcessing && !didComplete

    val isBuyButtonEnabled: Boolean
        get() = !isProcessing && !didComplete && paymentOption != null && showFinalCheckout

    val shippingAddressLabel: String
        get() = shippingAddress?.address?.let { address ->
            listOfNotNull(
                address.line1,
                address.city,
                address.state,
                address.country
            ).joinToString(", ")
        } ?: "Add shipping address"

    val showLoading: Boolean
        get() = isProcessing || !isFlowControllerConfigured

    data class PaymentInfo(
        val clientSecret: String,
        val customerConfiguration: PaymentSheet.CustomerConfiguration?,
        val merchantDisplayName: String,
        val customerEmail: String? = null,
    ) {

        @OptIn(WalletButtonsPreview::class)
        fun paymentSheetConfig(shippingAddress: AddressDetails?): PaymentSheet.Configuration =
            PaymentSheet.Configuration.Builder(merchantDisplayName = merchantDisplayName)
                .customer(customerConfiguration)
                .defaultBillingDetails(
                    customerEmail?.let { email ->
                        PaymentSheet.BillingDetails(
                            email = email
                        )
                    }
                )
                .shippingDetails(shippingAddress)
                .googlePay(
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = "US",
                    )
                )
                .walletButtons(
                    PaymentSheet.WalletButtonsConfiguration()
                )
                .link(
                    PaymentSheet.LinkConfiguration(
                        display = PaymentSheet.LinkConfiguration.Display.Automatic
                    )
                )
                .billingDetailsCollectionConfiguration(
                    PaymentSheet.BillingDetailsCollectionConfiguration(
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    )
                )
                .appearance(
                    PaymentSheet.Appearance(
                        colorsLight = PaymentSheet.Colors(
                            primary = androidx.compose.ui.graphics.Color.Black.toArgb(),
                            surface = PaymentSheet.Colors.defaultLight.surface,
                            component = PaymentSheet.Colors.defaultLight.component,
                            componentBorder = PaymentSheet.Colors.defaultLight.componentBorder,
                            componentDivider = PaymentSheet.Colors.defaultLight.componentDivider,
                            onComponent = PaymentSheet.Colors.defaultLight.onComponent,
                            onSurface = PaymentSheet.Colors.defaultLight.onSurface,
                            subtitle = PaymentSheet.Colors.defaultLight.subtitle,
                            placeholderText = PaymentSheet.Colors.defaultLight.placeholderText,
                            appBarIcon = PaymentSheet.Colors.defaultLight.appBarIcon,
                            error = PaymentSheet.Colors.defaultLight.error
                        ),
                        colorsDark = PaymentSheet.Colors(
                            primary = androidx.compose.ui.graphics.Color.Black.toArgb(),
                            surface = PaymentSheet.Colors.defaultDark.surface,
                            component = PaymentSheet.Colors.defaultDark.component,
                            componentBorder = PaymentSheet.Colors.defaultDark.componentBorder,
                            componentDivider = PaymentSheet.Colors.defaultDark.componentDivider,
                            onComponent = PaymentSheet.Colors.defaultDark.onComponent,
                            onSurface = PaymentSheet.Colors.defaultDark.onSurface,
                            subtitle = PaymentSheet.Colors.defaultDark.subtitle,
                            placeholderText = PaymentSheet.Colors.defaultDark.placeholderText,
                            appBarIcon = PaymentSheet.Colors.defaultDark.appBarIcon,
                            error = PaymentSheet.Colors.defaultDark.error
                        )
                    )
                )
                // Set `allowsDelayedPaymentMethods` to true if your business can handle payment
                // methods that complete payment after a delay, like SEPA Debit and Sofort.
                .allowsDelayedPaymentMethods(true)
                .build()
    }
}
