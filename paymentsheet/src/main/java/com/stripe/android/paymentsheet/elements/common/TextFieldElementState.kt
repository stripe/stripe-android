package com.stripe.android.paymentsheet.elements.common

/**
 * This represents the different states a field can be in,
 * in each of these cases there might be a reason to show the
 * error in a different way
 */
internal abstract class TextFieldElementState {

    /**
     * This indicate the Element state has reached the max number of characters
     * and focus should shift forward.
     */
    abstract fun isFull(): Boolean

    /**
     * Indicates element is valid and field extraction can happen
     * and be used to create PaymentMethod Parameters
     */
    abstract fun isValid(): Boolean

    /**
     * If in a state where isValid() returns false it indicates the element cannot be
     * used for extraction.  This function should return the error message.
     */
    abstract fun getErrorMessageResId(): Int?

    abstract class TextFieldElementStateValid : TextFieldElementState() {
        override fun isValid(): Boolean = true
        override fun isFull(): Boolean = false
        override fun getErrorMessageResId(): Int? = null
    }

    abstract class TextFieldElementStateError(private val stringResId: Int) :
        TextFieldElementState() {
        override fun isValid(): Boolean = false
        override fun isFull(): Boolean = false
        override fun getErrorMessageResId(): Int? = stringResId
    }
}