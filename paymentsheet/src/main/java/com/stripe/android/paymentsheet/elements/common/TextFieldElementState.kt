package com.stripe.android.paymentsheet.elements.common

/**
 * This represents the different states a field can be in,
 * in each of these cases there might be a reason to show the
 * error in a different way
 */
internal abstract class TextFieldElementState {

    abstract fun shouldShowError(hasFocus: Boolean): Boolean

    /**
     * Indicates element is valid and field extraction can happen
     * and be used to create PaymentMethod Parameters
     */
    fun isValid(): Boolean = this is TextFieldElementStateValid

    /**
     * If in a state where isValid() returns false it indicates the element cannot be
     * used for extraction.  This function should return the error message.
     */
    abstract fun getErrorMessageResId(): Int?

    abstract class TextFieldElementStateValid : TextFieldElementState() {
        override fun getErrorMessageResId(): Int? = null
        override fun shouldShowError(hasFocus: Boolean) = false
        abstract fun isFull(): Boolean?
    }

    abstract class TextFieldElementStateInvalid : TextFieldElementState()
}