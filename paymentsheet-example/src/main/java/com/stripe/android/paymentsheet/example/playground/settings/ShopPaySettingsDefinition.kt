package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object ShopPaySettingsDefinition : BooleanSettingsDefinition(
    key = "shopPay",
    displayName = "Enable ShopPay",
    defaultValue = false
) {

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return when (configurationData.integrationType) {
            PlaygroundConfigurationData.IntegrationType.Embedded,
            PlaygroundConfigurationData.IntegrationType.FlowController,
            PlaygroundConfigurationData.IntegrationType.FlowControllerWithSpt -> true
            PlaygroundConfigurationData.IntegrationType.PaymentSheet,
            PlaygroundConfigurationData.IntegrationType.CustomerSheet -> false
        }
    }

    @OptIn(ShopPayPreview::class)
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        if (value.not()) return
        val configuration = PaymentSheet.ShopPayConfiguration(
            shopId = "shop_1234",
            billingAddressRequired = true,
            emailRequired = true,
            shippingAddressRequired = true,
            lineItems = listOf(
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Potato",
                    amount = 5000
                ),
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Bread",
                    amount = 5000
                ),
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Potato",
                    amount = 5000
                ),
            ),
            shippingRates = listOf(
                PaymentSheet.ShopPayConfiguration.ShippingRate(
                    id = "1",
                    amount = 200,
                    displayName = "Delivery Fee",
                    deliveryEstimate = PaymentSheet.ShopPayConfiguration.DeliveryEstimate.Text(
                        value = "2-3 business days"
                    )
                )
            )
        )
        configurationBuilder.shopPayConfiguration(configuration)
    }
}
