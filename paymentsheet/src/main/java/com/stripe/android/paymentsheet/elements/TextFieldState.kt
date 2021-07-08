package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes

/**
 * This represents the different states a field can be in,
 * in each of these cases there might be a reason to show the
 * error in a different way.  This interface separates how the state
 * is implemented from what information is required by clients of the interface.
 * This will allow the implementation to change without impacting the clients.
 */
internal interface TextFieldState {

    /**
     * Indicate if this is an error that should be displayed to the user.
     * This cannot be used to determine if the field is valid or not because
     * there are some cases such as incomplete or blank where the error is not
     * displayed, but also not valid.
     */
    fun shouldShowError(hasFocus: Boolean): Boolean

    /**
     * Indicates an field is valid and field extraction can happen
     * and be used to create PaymentMethod Parameters
     */
    fun isValid(): Boolean

    /**
     * If in a state where isValid() returns false it indicates the field cannot be
     * used for extraction.  This function should return the error message.
     * It is up to calling shouldSHowError to determine if it should be displayed on screen.
     */
    @StringRes
    fun getErrorMessageResId(): Int?

    /**
     * This is used to indicate the field contains the maximum number of characters.
     * This is needed to know when to advance to the next field.
     */
    fun isFull(): Boolean
}
