package com.stripe.android.view

import androidx.annotation.IntRange
import com.stripe.android.model.Card
import com.stripe.android.model.PaymentMethodCreateParams

internal interface CardWidget {
    val card: Card?

    val cardBuilder: Card.Builder?

    /**
     * Gets a [PaymentMethodCreateParams.Card] object from the user input, if all fields are
     * valid. If not, returns `null`.
     *
     * @return a valid [PaymentMethodCreateParams.Card] object based on user input, or
     * `null` if any field is invalid
     */
    val paymentMethodCard: PaymentMethodCreateParams.Card?

    /**
     * Gets a [PaymentMethodCreateParams] object from the user input, if all fields are
     * valid. If not, returns `null`.
     *
     * @return a valid [PaymentMethodCreateParams] object based on user input, or
     * `null` if any field is invalid
     */
    val paymentMethodCreateParams: PaymentMethodCreateParams?

    fun setCardInputListener(listener: CardInputListener?)

    fun setCardHint(cardHint: String)

    fun clear()

    fun setCardNumber(cardNumber: String?)

    fun setExpiryDate(
        @IntRange(from = 1, to = 12) month: Int,
        @IntRange(from = 0, to = 9999) year: Int
    )

    fun setCvcCode(cvcCode: String?)
}
