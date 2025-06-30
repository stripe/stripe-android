package com.stripe.android.paymentsheet.example.playground.data

import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ShopPayHandlers
import com.stripe.android.paymentsheet.ShopPayHandlers.ShippingContactUpdate
import com.stripe.android.paymentsheet.ShopPayHandlers.ShippingRateUpdate

@OptIn(ShopPayPreview::class)
internal object ShopPayData {
    var billingAddressRequired: Boolean = true
    var emailRequired: Boolean = true
    var shippingAddressRequired: Boolean = true
    var rejectShippingAddressChange: Boolean = false
    var rejectShippingRateChange: Boolean = false

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
            shopId = "shop_id_123",
            billingAddressRequired = billingAddressRequired,
            emailRequired = emailRequired,
            shippingAddressRequired = shippingAddressRequired,
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

    internal fun shopPayHandlers(): ShopPayHandlers {
        return ShopPayHandlers(
            shippingMethodUpdateHandler = {
                if (rejectShippingRateChange) return@ShopPayHandlers null
                shippingMethodUpdateHandler(it)
            },
            shippingContactHandler = {
                if (rejectShippingAddressChange) return@ShopPayHandlers null
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
