package com.stripe.android.checkout.analytics

internal enum class CheckoutAnalyticsOperation(val value: String) {
    Init("init"),
    Confirm("confirm"),
    ApplyPromotionCode("apply_promotion_code"),
    RemovePromotionCode("remove_promotion_code"),
    UpdateShippingAddress("update_shipping_address"),
    UpdateBillingAddress("update_billing_address"),
    UpdateCurrency("update_currency"),
    RunServerUpdate("run_server_update"),
}

internal enum class CheckoutInvocationSource(val value: String?) {
    Merchant("merchant"),
    PaymentElement("payment_element"),
    ShippingAddressElement("shipping_address_element"),
    ExpressCheckoutElement("express_checkout_element"),
    Unspecified(null),
}
