package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.merchant_checkout

import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.model.PaymentOption

data class MerchantCheckoutViewState(
    val isProcessing: Boolean = false,
    val isError: Boolean = false,
    val cartState: CartState = CartState.default,
    val paymentInfo: PaymentInfo? = null,
    val status: String? = null,
    val paymentOption: PaymentOption? = null,
    val didComplete: Boolean = false,
    val showFinalCheckout: Boolean = false,
    val shippingAddress: AddressDetails? = null,
    val isLinkSelected: Boolean = false,
    val showConfiguration: Boolean = true,
    val customerEmail: String = "email@email.com",
    val merchantName: String = "Merchant Store",
    val enableInlineOtp: Boolean = false,
) {

    val isPaymentMethodButtonEnabled: Boolean
        get() = !isProcessing && !didComplete

    val isBuyButtonEnabled: Boolean
        get() = !isProcessing && !didComplete && (paymentOption != null || isLinkSelected) && showFinalCheckout

    val shippingAddressLabel: String
        get() = shippingAddress?.address?.let { address ->
            listOfNotNull(
                address.line1,
                address.city,
                address.state,
                address.country
            ).joinToString(", ")
        } ?: "Add shipping address"

    data class PaymentInfo(
        val clientSecret: String,
        val customerConfiguration: PaymentSheet.CustomerConfiguration?,
        val merchantDisplayName: String,
        val customerEmail: String? = null,
    ) {

        @OptIn(WalletButtonsPreview::class)
        val paymentSheetConfig: PaymentSheet.Configuration
            get() = PaymentSheet.Configuration.Builder(merchantDisplayName = merchantDisplayName)
                .customer(customerConfiguration)
                .defaultBillingDetails(
                    customerEmail?.let { email ->
                        PaymentSheet.BillingDetails(
                            email = email
                        )
                    }
                )
                .googlePay(
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = "US",
                    )
                )
                .walletButtons(
                    PaymentSheet.WalletButtonsConfiguration(
                        willDisplayExternally = true,
                        walletsToShow = listOf("google_pay", "link") // Explicitly include Link
                    )
                )
                .link(
                    PaymentSheet.LinkConfiguration(
                        display = PaymentSheet.LinkConfiguration.Display.Automatic
                    )
                )
                // Set `allowsDelayedPaymentMethods` to true if your business can handle payment
                // methods that complete payment after a delay, like SEPA Debit and Sofort.
                .allowsDelayedPaymentMethods(true)
                .build()
    }
} 