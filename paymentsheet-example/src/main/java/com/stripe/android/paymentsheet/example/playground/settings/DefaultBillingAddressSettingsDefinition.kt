package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object DefaultBillingAddressSettingsDefinition : BooleanSettingsDefinition(
    key = "defaultBillingAddress",
    displayName = "Default Billing Address",
    defaultValue = true,
) {
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        if (value) {
            configurationBuilder.defaultBillingDetails(
                PaymentSheet.BillingDetails(
                    address = PaymentSheet.Address(
                        line1 = "354 Oyster Point Blvd",
                        line2 = null,
                        city = "South San Francisco",
                        state = "CA",
                        postalCode = "94080",
                        country = "US",
                    ),
                    email = "email@email.com",
                    name = "Jenny Rosen",
                    phone = "+18008675309",
                )
            )
        }
    }
}
