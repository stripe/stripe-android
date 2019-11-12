package com.stripe.android.view

import androidx.annotation.StringDef

/**
 * Represents a listener for card input events. Note that events are
 * not one-time events. For instance, a user can "complete" the CVC many times
 * by deleting and re-entering the value.
 */
interface CardInputListener {

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(FocusField.FOCUS_CARD, FocusField.FOCUS_EXPIRY, FocusField.FOCUS_CVC,
        FocusField.FOCUS_POSTAL)
    annotation class FocusField {
        companion object {
            const val FOCUS_CARD: String = "focus_card"
            const val FOCUS_EXPIRY: String = "focus_expiry"
            const val FOCUS_CVC: String = "focus_cvc"
            const val FOCUS_POSTAL: String = "focus_postal"
        }
    }

    /**
     * Called whenever the field of focus within the widget changes.
     *
     * @param focusField a [FocusField] to which the focus has just changed.
     */
    fun onFocusChange(@FocusField focusField: String)

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
     * Called when a potentially valid postal code or zip code has been entered.
     * May be called multiple times.
     */
    fun onPostalCodeComplete()
}
