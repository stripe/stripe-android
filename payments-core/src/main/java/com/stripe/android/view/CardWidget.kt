package com.stripe.android.view

import android.text.TextWatcher
import androidx.annotation.IntRange
import com.stripe.android.model.PaymentMethodCreateParams

internal interface CardWidget {

    /**
     * A [PaymentMethodCreateParams.Card] representing the card details if all fields are valid;
     * otherwise `null`
     */
    val paymentMethodCard: PaymentMethodCreateParams.Card?

    /**
     * A [PaymentMethodCreateParams] representing the card details and postal code if all fields
     * are valid; otherwise `null`
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
