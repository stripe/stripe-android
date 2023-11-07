package com.stripe.android.paymentsheet.example.samples.model

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleUpdateResponse

data class CartState(
    val products: List<CartProduct>,
    val isSubscription: Boolean,
    val subtotal: Long? = null,
    val salesTax: Long? = null,
    val total: Long? = null,
    val customerId: String? = null,
    val customerEphemeralKeySecret: String? = null,
) {

    val formattedSubtotal: String
        get() = subtotal?.toAmountString() ?: "…"

    val formattedTax: String
        get() = salesTax?.toAmountString() ?: "…"

    val formattedTotal: String
        get() = total?.toAmountString() ?: "…"

    fun updateQuantity(productId: CartProduct.Id, newQuantity: Int): CartState {
        return copy(
            products = products.map { product ->
                if (product.id == productId) {
                    product.copy(quantity = newQuantity)
                } else {
                    product
                }
            },
        )
    }

    fun countOf(id: CartProduct.Id): Int {
        return products.filter { it.id == id }.sumOf { it.quantity }
    }

    fun makeCustomerConfig() = if (customerId != null && customerEphemeralKeySecret != null) {
        PaymentSheet.CustomerConfiguration(
            id = customerId,
            ephemeralKeySecret = customerEphemeralKeySecret
        )
    } else {
        null
    }

    companion object {
        val default: CartState = CartState(
            products = listOf(CartProduct.hotDog, CartProduct.salad),
            isSubscription = false,
        )

        val defaultWithHardcodedPrices: CartState = CartState(
            products = listOf(CartProduct.hotDog, CartProduct.salad),
            isSubscription = false,
            subtotal = 899,
            salesTax = 74,
            total = 973,
        )
    }
}

internal fun CartState.updateWithResponse(
    response: ExampleUpdateResponse,
): CartState {
    return copy(
        subtotal = response.subtotal,
        salesTax = response.tax,
        total = response.total,
    )
}

internal fun CartState.updateWithResponse(
    response: ExampleCheckoutResponse,
): CartState {
    return copy(
        subtotal = response.subtotal,
        salesTax = response.tax,
        total = response.total,
        customerId = response.customer,
        customerEphemeralKeySecret = response.ephemeralKey,
    )
}

internal fun CartState.toIntentConfiguration(): PaymentSheet.IntentConfiguration {
    return PaymentSheet.IntentConfiguration(
        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
            amount = total ?: 0L,
            currency = "usd",
            setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession.takeIf {
                isSubscription
            },
        )
    )
}
