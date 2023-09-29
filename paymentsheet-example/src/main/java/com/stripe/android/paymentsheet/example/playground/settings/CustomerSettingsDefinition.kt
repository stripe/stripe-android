package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CustomerSettingsDefinition : StringSettingDefinition(
    key = "customer",
    displayName = "Customer",
) {
    private val guest = Option("Guest", "guest")

    override val defaultValue: String = guest.value
    override val options: List<Option<String>> = listOf(
        guest,
        Option("New", "new"),
        Option("Returning", "returning"),
    )

    override fun configure(
        value: String,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
        checkoutRequestBuilder.customer(value)
    }

    override fun configure(
        value: String,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PaymentSheetConfigurationData,
    ) {
        configurationBuilder.customer(playgroundState.customerConfig)
    }
}
