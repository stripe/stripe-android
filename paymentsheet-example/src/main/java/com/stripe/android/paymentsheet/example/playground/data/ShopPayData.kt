package com.stripe.android.paymentsheet.example.playground.data

import com.stripe.android.elements.payment.ShopPayPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ShopPayHandlers
import com.stripe.android.paymentsheet.ShopPayHandlers.ShippingContactUpdate
import com.stripe.android.paymentsheet.ShopPayHandlers.ShippingRateUpdate

@OptIn(ShopPayPreview::class)
internal object ShopPayData {

    private val singleBusinessDay = PaymentSheet.ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit(
        value = 1,
        unit = PaymentSheet.ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.BUSINESS_DAY
    )
    private val fiveBusinessDays = PaymentSheet.ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit(
        value = 5,
        unit = PaymentSheet.ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.BUSINESS_DAY
    )
    private val sevenBusinessDays = PaymentSheet.ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit(
        value = 7,
        unit = PaymentSheet.ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.BUSINESS_DAY
    )

    private val shippingRates = listOf(
        PaymentSheet.ShopPayConfiguration.ShippingRate(
            id = "express",
            amount = 1099,
            displayName = "Overnight",
            deliveryEstimate = PaymentSheet.ShopPayConfiguration.DeliveryEstimate.Range(
                maximum = singleBusinessDay,
                minimum = singleBusinessDay
            )
        ),
        PaymentSheet.ShopPayConfiguration.ShippingRate(
            id = "standard",
            amount = 0,
            displayName = "Free",
            deliveryEstimate = PaymentSheet.ShopPayConfiguration.DeliveryEstimate.Range(
                maximum = sevenBusinessDays,
                minimum = fiveBusinessDays
            )
        ),
    )

    internal fun shopPayConfiguration(): PaymentSheet.ShopPayConfiguration {
        return PaymentSheet.ShopPayConfiguration(
            shopId = "92917334038",
            billingAddressRequired = true,
            emailRequired = true,
            shippingAddressRequired = true,
            lineItems = listOf(
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Golden Potato",
                    amount = 500
                ),
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Silver Potato",
                    amount = 345
                ),
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Tax",
                    amount = 200
                ),
                PaymentSheet.ShopPayConfiguration.LineItem(
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
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Golden Potato",
                    amount = 500
                ),
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Silver Potato",
                    amount = 345
                ),
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Tax",
                    amount = 200
                ),
                PaymentSheet.ShopPayConfiguration.LineItem(
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
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Golden Potato",
                    amount = 500
                ),
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Silver Potato",
                    amount = 345
                ),
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Tax",
                    amount = 200
                ),
                PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Shipping",
                    amount = shippingRates.first().amount
                ),
            ),
            shippingRates = shippingRates
        )
    }
}
