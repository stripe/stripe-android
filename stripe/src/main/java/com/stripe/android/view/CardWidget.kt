package com.stripe.android.view

import android.text.TextWatcher
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
     * @return a valid [PaymentMethodCreateParams] object based on user input, or `null` if any
     * field is invalid. The object will include any billing details that the user entered.
     */
    val paymentMethodCreateParams: PaymentMethodCreateParams?

    fun setCardValidCallback(callback: CardValidCallback?)

    fun setCardInputListener(listener: CardInputListener?)

    /**
     * Set a `TextWatcher` to receive card number changes.
     */
    fun setCardNumberTextWatcher(cardNumberTextWatcher: TextWatcher?)

    /**
     * Set a `TextWatcher` to receive expiration date changes.
     */
    fun setExpiryDateTextWatcher(expiryDateTextWatcher: TextWatcher?)

    /**
     * Set a `TextWatcher` to receive CVC value changes.
     */
    fun setCvcNumberTextWatcher(cvcNumberTextWatcher: TextWatcher?)

    /**
     * Set a `TextWatcher` to receive postal code changes.
     */
    fun setPostalCodeTextWatcher(postalCodeTextWatcher: TextWatcher?)

    fun setCardHint(cardHint: String)

    fun clear()

    fun setCardNumber(cardNumber: String?)

    fun setExpiryDate(
        @IntRange(from = 1, to = 12) month: Int,
        @IntRange(from = 0, to = 9999) year: Int
    )

    fun setCvcCode(cvcCode: String?)

    companion object {
        internal const val DEFAULT_POSTAL_CODE_ENABLED = true
        internal const val DEFAULT_POSTAL_CODE_REQUIRED = false
        internal const val DEFAULT_US_ZIP_CODE_REQUIRED = false
    }
}
