package com.stripe.android.paymentsheet.example.playground.data

import com.stripe.android.elements.payment.ShopPayConfiguration
import com.stripe.android.elements.payment.ShopPayHandlers
import com.stripe.android.elements.payment.ShopPayHandlers.ShippingContactUpdate
import com.stripe.android.elements.payment.ShopPayHandlers.ShippingRateUpdate
import com.stripe.android.elements.payment.ShopPayPreview

@OptIn(ShopPayPreview::class)
internal object ShopPayData {

    private val singleBusinessDay = ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit(
        value = 1,
        unit = ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.BUSINESS_DAY
    )
    private val fiveBusinessDays = ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit(
        value = 5,
        unit = ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.BUSINESS_DAY
    )
    private val sevenBusinessDays = ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit(
        value = 7,
        unit = ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.BUSINESS_DAY
    )

    private val shippingRates = listOf(
        ShopPayConfiguration.ShippingRate(
            id = "express",
            amount = 1099,
            displayName = "Overnight",
            deliveryEstimate = ShopPayConfiguration.DeliveryEstimate.Range(
                maximum = singleBusinessDay,
                minimum = singleBusinessDay
            )
        ),
        ShopPayConfiguration.ShippingRate(
            id = "standard",
            amount = 0,
            displayName = "Free",
            deliveryEstimate = ShopPayConfiguration.DeliveryEstimate.Range(
                maximum = sevenBusinessDays,
                minimum = fiveBusinessDays
            )
        ),
    )

    internal fun shopPayConfiguration(): ShopPayConfiguration {
        return ShopPayConfiguration(
            shopId = "92917334038",
            billingAddressRequired = true,
            emailRequired = true,
            shippingAddressRequired = true,
            lineItems = listOf(
                ShopPayConfiguration.LineItem(
                    name = "Golden Potato",
                    amount = 500
                ),
                ShopPayConfiguration.LineItem(
                    name = "Silver Potato",
                    amount = 345
                ),
                ShopPayConfiguration.LineItem(
                    name = "Tax",
                    amount = 200
                ),
                ShopPayConfiguration.LineItem(
                    name = "Shipping",
                    amount = shippingRates.first().amount
                ),
            ),
            shippingRates = shippingRates,
            allowedShippingCountries = listOf("US", "CA")
        )
    }

    internal fun shopPayHandlers(): ShopPayHandlers {
        return ShopPayHandlers(
            shippingMethodUpdateHandler = {
                shippingMethodUpdateHandler(it)
            },
            shippingContactHandler = {
                shippingContactHandler()
            }
        )
    }

    private fun shippingMethodUpdateHandler(
        selectedRate: ShopPayHandlers.SelectedShippingRate
    ): ShippingRateUpdate {
        return ShippingRateUpdate(
            lineItems = listOf(
                ShopPayConfiguration.LineItem(
                    name = "Golden Potato",
                    amount = 500
                ),
                ShopPayConfiguration.LineItem(
                    name = "Silver Potato",
                    amount = 345
                ),
                ShopPayConfiguration.LineItem(
                    name = "Tax",
                    amount = 200
                ),
                ShopPayConfiguration.LineItem(
                    name = "Shipping",
                    amount = selectedRate.shippingRate.amount
                ),
            ),
            shippingRates = shippingRates
        )
    }

    private fun shippingContactHandler(): ShippingContactUpdate? {
        return ShippingContactUpdate(
            lineItems = listOf(
                ShopPayConfiguration.LineItem(
                    name = "Golden Potato",
                    amount = 500
                ),
                ShopPayConfiguration.LineItem(
                    name = "Silver Potato",
                    amount = 345
                ),
                ShopPayConfiguration.LineItem(
                    name = "Tax",
                    amount = 200
                ),
                ShopPayConfiguration.LineItem(
                    name = "Shipping",
                    amount = shippingRates.first().amount
                ),
            ),
            shippingRates = shippingRates
        )
    }
}
