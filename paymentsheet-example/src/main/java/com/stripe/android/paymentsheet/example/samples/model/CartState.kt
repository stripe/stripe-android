package com.stripe.android.paymentsheet.example.samples.model

import com.stripe.android.paymentsheet.PaymentSheet
import kotlin.math.roundToLong

data class CartState(
    val products: List<CartProduct>,
    val isSubscription: Boolean,
) {

    val subtotal: Long
        get() = products.sumOf { it.total }

    val salesTax: Long
        get() = (subtotal * 0.0825f).roundToLong()

    private val totalBeforeDiscount: Long
        get() = subtotal + salesTax

    private val subscriptionDiscount: Long
        get() = if (isSubscription) {
            (totalBeforeDiscount * 0.05).roundToLong()
        } else {
            0
        }

    val total: Long
        get() = totalBeforeDiscount - subscriptionDiscount

    fun updateQuantity(productId: Long, newQuantity: Int?): CartState {
        return copy(
            products = products.map { product ->
                if (product.id == productId) {
                    product.copy(quantity = newQuantity)
                } else {
                    product
                }
            }
        )
    }

    companion object {
        val default: CartState = CartState(
            products = listOf(CartProduct.hotDog, CartProduct.salad),
            isSubscription = false,
        )
    }
}

fun CartState.toIntentConfiguration(): PaymentSheet.IntentConfiguration {
    return PaymentSheet.IntentConfiguration(
        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
            amount = total,
            currency = "usd",
            setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession.takeIf {
                isSubscription
            },
        )
    )
}
