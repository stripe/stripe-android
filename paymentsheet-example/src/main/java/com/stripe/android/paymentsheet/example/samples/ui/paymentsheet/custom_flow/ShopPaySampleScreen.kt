package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.custom_flow

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration.DeliveryEstimate
import com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit
import com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit.TimeUnit
import com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration.LineItem
import com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration.ShippingRate
import com.stripe.android.paymentsheet.SelectedPartialAddress
import com.stripe.android.paymentsheet.SelectedShippingRate
import com.stripe.android.paymentsheet.ShippingContactUpdate
import com.stripe.android.paymentsheet.ShippingRateUpdate
import com.stripe.android.paymentsheet.ShopPayHandlers

private val singleBusinessDay = DeliveryEstimateUnit(
    value = 1,
    unit = TimeUnit.BUSINESS_DAY
)
private val fiveBusinessDays = DeliveryEstimateUnit(
    value = 5,
    unit = TimeUnit.BUSINESS_DAY
)
private val sevenBusinessDays = DeliveryEstimateUnit(
    value = 7,
    unit = TimeUnit.BUSINESS_DAY
)

@Composable
fun ShopPaySampleScreen() {
    val shopPayHandlers = shopPayHandlers()
    val flowControllerBuilder = remember(shopPayHandlers) {
        PaymentSheet.FlowController.Builder(
            resultCallback = { },
            paymentOptionCallback = { },
        ).shopPayHandlers(shopPayHandlers())
    }

    val flowController = flowControllerBuilder
        .build()

    Button(
        onClick = {
            val configuration = PaymentSheet.Configuration.Builder("Example, Inc.")
                .shopPayConfiguration(shopPayConfiguration())
                .build()
            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "fake_stripe_intent",
                configuration = configuration,
                callback = { _, _ -> }
            )
        }
    ) {
        Text("Pay")
    }
}

private fun shopPayHandlers(): ShopPayHandlers {
    return ShopPayHandlers(
        shippingMethodUpdateHandler = ::onShippingMethodUpdate,
        shippingContactHandler = ::onShippingContactUpdate,
    )
}

private fun shopPayConfiguration(): PaymentSheet.ShopPayConfiguration {
    return PaymentSheet.ShopPayConfiguration(
        shippingAddressRequired = true,
        lineItems = listOf(
            LineItem(
                name = "product 1",
                amount = 1099
            )
        ),
        shippingRates = listOf(
            ShippingRate(
                id = "id1",
                amount = 1099,
                displayName = "Overnight",
                deliveryEstimate = DeliveryEstimate.Range(
                    minimum = singleBusinessDay,
                    maximum = singleBusinessDay
                )
            ),
            ShippingRate(
                id = "id2",
                amount = 0,
                displayName = "Free",
                deliveryEstimate = DeliveryEstimate.Range(
                    minimum = fiveBusinessDays,
                    maximum = sevenBusinessDays
                )
            ),
        ),
        shopId = "shop_1234", // This is a fake Shop ID, replace with your own
    )
}

private fun onShippingMethodUpdate(
    selectedRate: SelectedShippingRate,
    updateCallback: (ShippingRateUpdate) -> Unit
) {
    val update = ShippingRateUpdate(
        lineItems = listOf(
            LineItem(name = "Subtotal", amount = 200),
            LineItem(name = "Tax", amount = 200),
            LineItem(
                name = "Shipping",
                amount = selectedRate.shippingRate.amount
            ),
        ),
        shippingRates = listOf(
            ShippingRate(
                id = "standard",
                amount = 500,
                displayName = "Standard Shipping",
                deliveryEstimate = DeliveryEstimate.Range(
                    minimum = DeliveryEstimateUnit(
                        value = 3,
                        unit = TimeUnit.BUSINESS_DAY
                    ),
                    maximum = DeliveryEstimateUnit(
                        value = 5,
                        unit = TimeUnit.BUSINESS_DAY
                    )
                )
            ),
            ShippingRate(
                id = "express",
                amount = 1000,
                displayName = "Express Shipping",
                deliveryEstimate = DeliveryEstimate.Range(
                    minimum = DeliveryEstimateUnit(
                        value = 1,
                        unit = TimeUnit.BUSINESS_DAY
                    ),
                    maximum = DeliveryEstimateUnit(
                        value = 2,
                        unit = TimeUnit.BUSINESS_DAY
                    )
                )
            ),
        )
    )
    updateCallback(update)
}

private fun onShippingContactUpdate(
    address: SelectedPartialAddress,
    updateCallback: (ShippingContactUpdate?) -> Unit
) {
    val canShipToLocation = isValidShippingLocation(address)

    if (canShipToLocation) {
        val shippingRates = getShippingRatesForLocation(address)
        val update = ShippingContactUpdate(
            lineItems = listOf(
                LineItem(name = "Subtotal", amount = 200),
                LineItem(name = "Tax", amount = 200),
                LineItem(
                    name = "Shipping",
                    amount = shippingRates.first().amount
                ),
            ),
            shippingRates = shippingRates
        )
        updateCallback(update)
    } else {
        updateCallback(null)
    }
}

private fun isValidShippingLocation(address: SelectedPartialAddress): Boolean {
    return address.country == "US"
}

private fun getShippingRatesForLocation(
    address: SelectedPartialAddress
): List<ShippingRate> {
    // Return different rates based on the address.
    return if (address.state == "CA") {
        listOf(
            ShippingRate(
                id = "ca-standard",
                amount = 600,
                displayName = "Standard Shipping",
                deliveryEstimate = DeliveryEstimate.Range(
                    minimum = DeliveryEstimateUnit(
                        value = 4,
                        unit = TimeUnit.BUSINESS_DAY
                    ),
                    maximum = DeliveryEstimateUnit(
                        value = 6,
                        unit = TimeUnit.BUSINESS_DAY
                    )
                )
            ),
            ShippingRate(
                id = "ca-express",
                amount = 1200,
                displayName = "Express Shipping",
                deliveryEstimate = DeliveryEstimate.Range(
                    minimum = DeliveryEstimateUnit(
                        value = 2,
                        unit = TimeUnit.BUSINESS_DAY
                    ),
                    maximum = DeliveryEstimateUnit(
                        value = 3,
                        unit = TimeUnit.BUSINESS_DAY
                    )
                )
            ),
        )
    } else {
        listOf(
            ShippingRate(
                id = "us-standard",
                amount = 500,
                displayName = "Standard Shipping",
                deliveryEstimate = DeliveryEstimate.Range(
                    minimum = DeliveryEstimateUnit(
                        value = 3,
                        unit = TimeUnit.BUSINESS_DAY
                    ),
                    maximum = DeliveryEstimateUnit(
                        value = 5,
                        unit = TimeUnit.BUSINESS_DAY
                    )
                )
            ),
            ShippingRate(
                id = "us-express",
                amount = 1000,
                displayName = "Express Shipping",
                deliveryEstimate = DeliveryEstimate.Range(
                    minimum = DeliveryEstimateUnit(
                        value = 1,
                        unit = TimeUnit.BUSINESS_DAY
                    ),
                    maximum = DeliveryEstimateUnit(
                        value = 2,
                        unit = TimeUnit.BUSINESS_DAY
                    )
                )
            ),
        )
    }
}
