package com.stripe.android.paymentsheet.example.samples.model

import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.HOT_DOG_EMOJI
import com.stripe.android.paymentsheet.example.samples.ui.SALAD_EMOJI

data class CartProduct(
    val id: Id,
    val icon: String,
    val nameResId: Int,
    val unitPrice: Long,
    val quantity: Int,
) {

    val unitPriceString: String
        get() = unitPrice.toAmountString()

    enum class Id(val value: Int) {
        HotDog(1),
        Salad(2),
    }

    companion object {

        val hotDog = CartProduct(
            id = Id.HotDog,
            icon = HOT_DOG_EMOJI,
            nameResId = R.string.hot_dog,
            unitPrice = 99,
            quantity = 1,
        )

        val salad = CartProduct(
            id = Id.Salad,
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
    return "$$dollars.$cents"
}
