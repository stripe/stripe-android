package com.stripe.android.view

/**
 * Represents a listener for card input events. Note that events are
 * not one-time events. For instance, a user can "complete" the CVC many times
 * by deleting and re-entering the value.
 */
interface CardInputListener {
    enum class FocusField {
        CardNumber,
        ExpiryDate,
        Cvc,
        PostalCode
    }

    /**
     * Called whenever the field of focus within the widget changes.
     *
     * @param focusField a [FocusField] to which the focus has just changed.
     */
    fun onFocusChange(focusField: FocusField)

    /**
     * Called when a potentially valid card number has been completed in the
     * [CardNumberEditText]. May be called multiple times if the user edits
     * the field.
     */
    fun onCardComplete()

    /**
     * Called when a expiration date (one that has not yet passed) has been entered.
     * May be called multiple times, if the user edits the date.
     */
    fun onExpirationComplete()

    /**
     * Called when a potentially valid CVC has been entered. The only verification performed
     * on the number is that it is the correct length. May be called multiple times, if
     * the user edits the CVC.
     */
    fun onCvcComplete()

    /**
     * Called when a valid postal code has been entered.
     * May be called multiple times, if the user edits the field.
     * If the [CardWidget] is not collecting US card, any non-empty postal is considered valid.
     */
    fun onPostalCodeComplete()
}
