package com.stripe.android.paymentsheet.example.samples.model

import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.paymentsheet.PaymentSheet

data class CartState(
    val products: List<CartProduct>,
    val isSubscription: Boolean,
    val subtotal: Long? = null,
    val salesTax: Long? = null,
    val total: Long? = null,
) {

    val formattedSubtotal: String
        get() = subtotal?.toAmountString() ?: "…"

    val formattedTax: String
        get() = salesTax?.toAmountString() ?: "…"

    val formattedTotal: String
        get() = total?.toAmountString() ?: "…"

    fun countOf(id: CartProduct.Id): Int {
        return products.filter { it.id == id }.sumOf { it.quantity }
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

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
fun CartState.toIntentConfiguration(): PaymentSheet.IntentConfiguration {
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
