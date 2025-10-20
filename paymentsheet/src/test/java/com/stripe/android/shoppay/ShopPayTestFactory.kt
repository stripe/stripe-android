package com.stripe.android.shoppay

import com.stripe.android.common.model.SHOP_PAY_CONFIGURATION
import com.stripe.android.shoppay.bridge.ECEBillingDetails
import com.stripe.android.shoppay.bridge.ECEDeliveryEstimate
import com.stripe.android.shoppay.bridge.ECEFullAddress
import com.stripe.android.shoppay.bridge.ECEShippingAddressData
import com.stripe.android.shoppay.bridge.ECEShippingRate

internal object ShopPayTestFactory {
    val ECE_SHIPPING_RATE = ECEShippingRate(
        id = "rate_1",
        displayName = "Standard Shipping",
        amount = 500,
        deliveryEstimate = ECEDeliveryEstimate.Text("5-7 business days")
    )

    val ECE_EXPRESS_SHIPPING_RATE = ECEShippingRate(
        id = "rate_2",
        displayName = "Express Shipping",
        amount = 1000,
        deliveryEstimate = ECEDeliveryEstimate.Text("1-2 business days")
    )

    val BILLING_ADDRESS = ECEFullAddress(
        line1 = "123 Main St",
        line2 = "Apt 4B",
        city = "New York",
        state = "NY",
        postalCode = "10001",
        country = "US"
    )

    val SHIPPING_ADDRESS = ECEFullAddress(
        line1 = "456 Shipping Ave",
        line2 = "Unit 2B",
        city = "Shipping City",
        state = "NY",
        postalCode = "10002",
        country = "US"
    )

    val BILLING_DETAILS = ECEBillingDetails(
        name = "John Doe",
        email = "john.doe@example.com",
        phone = "+1234567890",
        address = BILLING_ADDRESS
    )

    val SHIPPING_ADDRESS_DATA = ECEShippingAddressData(
        name = "Jane Smith",
        address = SHIPPING_ADDRESS
    )

    val SHIPPING_ADDRESS_DATA_WITH_NULL_ADDRESS = ECEShippingAddressData(
        name = "Jane Smith",
        address = null
    )

    val MINIMAL_BILLING_DETAILS = ECEBillingDetails(
        name = "Test User",
        email = null,
        phone = null,
        address = null
    )

    val INTERNATIONAL_ADDRESS = ECEFullAddress(
        line1 = "10 Downing Street",
        line2 = null,
        city = "London",
        state = null,
        postalCode = "SW1A 2AA",
        country = "GB"
    )

    val INTERNATIONAL_BILLING_DETAILS = ECEBillingDetails(
        name = "John Smith",
        email = "john.smith@example.co.uk",
        phone = "+447123456789",
        address = INTERNATIONAL_ADDRESS
    )

    val SHOP_PAY_ARGS = ShopPayArgs(
        publishableKey = "pk_1234",
        shopPayConfiguration = SHOP_PAY_CONFIGURATION,
        customerSessionClientSecret = "css_test_123",
        businessName = "Test Business",
        paymentElementCallbackIdentifier = "paymentElementCallbackIdentifier",
        stripeAccountId = "acct_123"
    )
}
