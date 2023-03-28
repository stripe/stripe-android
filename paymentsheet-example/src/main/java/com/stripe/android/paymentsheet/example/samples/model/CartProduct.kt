package com.stripe.android.paymentsheet.example.samples.model

import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.HOT_DOG_EMOJI
import com.stripe.android.paymentsheet.example.samples.ui.SALAD_EMOJI

data class CartProduct(
    val id: Long,
    val icon: String,
    val nameResId: Int,
    val unitPrice: Long,
    val quantity: Int?,
) {

    val total: Long
        get() = unitPrice * (quantity ?: 0)

    val unitPriceString: String
        get() = unitPrice.toAmountString()

    companion object {

        val hotDog = CartProduct(
            id = 1,
            icon = HOT_DOG_EMOJI,
            nameResId = R.string.hot_dog,
            unitPrice = 99,
            quantity = 1,
        )

        val salad = CartProduct(
            id = 2,
            icon = SALAD_EMOJI,
            nameResId = R.string.salad,
            unitPrice = 800,
            quantity = 1,
        )
    }
}

fun Long.toAmountString(): String {
    val dollars = this / 100
    val cents = (this % 100).toString().padStart(length = 2, padChar = '0')
    return "$$dollars.${cents}"
}
