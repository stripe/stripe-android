package com.stripe.android.paymentsheet.elements.common

/**
 * This represents the different states a field can be in,
 * in each of these cases there might be a reason to show the
 * error in a different way
 */
internal sealed class TextFieldElementState {

    abstract class Valid(val resId: Int) : TextFieldElementState() {
        abstract fun isFull(): Boolean?
    }

    abstract class Invalid(val resId: Int) : TextFieldElementState() {
        abstract fun shouldShowError(state: Invalid, hasFocus: Boolean): Boolean
        abstract fun getErrorMessageResId(): Int
    }

//    /**
//     * This indicate the Element state has reached the max number of characters
//     * and focus should shift forward.
//     */
//    abstract fun isFull(): Boolean
//
//    /**
//     * Indicates element is valid and field extraction can happen
//     * and be used to create PaymentMethod Parameters
//     */
//    abstract fun isValid(): Boolean
//
//    /**
//     * If in a state where isValid() returns false it indicates the element cannot be
//     * used for extraction.  This function should return the error message.
//     */
//    abstract fun getErrorMessageResId(): Int?

//    abstract class TextFieldElementStateValid : TextFieldElementState() {
//        fun isValid(): Boolean = true
//        fun isFull(): Boolean = false
//        fun getErrorMessageResId(): Int? = null
//    }
//
//    abstract class TextFieldElementStateError(private val stringResId: Int) :
//        TextFieldElementState() {
//        fun isValid(): Boolean = false
//        fun isFull(): Boolean = false
//        fun getErrorMessageResId(): Int? = stringResId
//    }
}