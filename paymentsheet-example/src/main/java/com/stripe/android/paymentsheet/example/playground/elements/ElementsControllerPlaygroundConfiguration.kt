package com.stripe.android.paymentsheet.example.playground.elements

import com.stripe.android.elements.ElementsController
import com.stripe.android.elements.ElementsController.ExpressCheckoutElement.LineItem
import com.stripe.android.elements.ElementsController.ExpressCheckoutElement.ShippingAddressChangeEvent
import com.stripe.android.elements.ElementsController.ExpressCheckoutElement.ShippingChangeResult
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.ElementsShipping
import com.stripe.android.paymentsheet.example.playground.settings.ElementsShippingSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.ElementsWalletOrder
import com.stripe.android.paymentsheet.example.playground.settings.ElementsWalletOrderSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.GooglePaySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkDisplaySetting
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.createBillingDetails

internal suspend fun configureElementsController(
    elementsController: ElementsController,
    state: PlaygroundState.Payment,
): Result<Unit> {
    return elementsController.configure(state.intentConfiguration(), state.elementsConfiguration())
}

private fun PlaygroundState.Payment.elementsConfiguration(): ElementsController.Configuration {
    return ElementsController.Configuration("Example, Inc.")
        .expressCheckoutElement(expressCheckoutConfiguration())
        .shippingAddress(
            required = snapshot[ElementsShippingSettingsDefinition] != ElementsShipping.Off,
            allowedCountryCodes = emptySet(),
        )
        .apply {
            GooglePaySettingsDefinition.createGooglePayConfiguration(
                configOption = snapshot[GooglePaySettingsDefinition],
                merchant = merchantCode,
                currency = currencyCode,
            )?.let { googlePay(it) }
            createBillingDetails(
                snapshot[DefaultBillingAddressSettingsDefinition],
            )?.let { defaultBillingDetails(it) }
        }
}

private fun PlaygroundState.Payment.expressCheckoutConfiguration(): ExpressCheckoutElement.Configuration {
    return ExpressCheckoutElement.Configuration()
        .linkConfiguration(
            ExpressCheckoutElement.Configuration.LinkConfiguration().display(
                when (snapshot[LinkSettingsDefinition]) {
                    LinkDisplaySetting.Automatic ->
                        ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic
                    LinkDisplaySetting.Never,
                    LinkDisplaySetting.WalletButtonHidden ->
                        ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never
                }
            )
        )
        .paymentMethodOrder(
            when (snapshot[ElementsWalletOrderSettingsDefinition]) {
                ElementsWalletOrder.Session -> emptyList()
                ElementsWalletOrder.GooglePayFirst -> listOf("google_pay", "link")
                ElementsWalletOrder.LinkFirst -> listOf("link", "google_pay")
            }
        )
}

internal fun elementsShippingResponse(
    state: PlaygroundState.Payment,
    event: ShippingAddressChangeEvent,
): ShippingChangeResult {
    return if (state.snapshot[ElementsShippingSettingsDefinition] == ElementsShipping.Reject) {
        ShippingChangeResult.Reject("This shipping address was rejected by the playground callback.")
    } else {
        // Keep the configured amount. A real integration updates its server order before creating an intent.
        ShippingChangeResult.Resolve(
            amount = null,
            lineItems = listOf(LineItem("Order shipping to ${event.address.country}", state.amount ?: 0L)),
        )
    }
}
