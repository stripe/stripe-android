package com.stripe.android.paymentsheet.elements.common

import com.stripe.android.paymentsheet.R

class TextFieldStateConstants {

    sealed class Valid : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean): Boolean = false
        override fun isValid(): Boolean = true
        override fun getErrorMessageResId(): Int? = null

        object Full : Valid() {
            override fun isFull(): Boolean = true
        }

        object Limitless : Valid() // no auto-advance
        {
            override fun isFull(): Boolean = false
        }
    }

    sealed class Invalid : TextFieldState {
        override fun isValid(): Boolean = false
        override fun isFull(): Boolean = false

        object AlwaysError : TextFieldState {
            override fun shouldShowError(hasFocus: Boolean): Boolean = true
            override fun isValid(): Boolean = false
            override fun getErrorMessageResId(): Int = R.string.invalid
            override fun isFull(): Boolean = false
        }

        object Incomplete : Invalid() {
            override fun shouldShowError(hasFocus: Boolean): Boolean = !hasFocus
            override fun getErrorMessageResId(): Int = R.string.incomplete
        }

        object Malformed : Invalid() {
            override fun shouldShowError(hasFocus: Boolean): Boolean = true
            override fun getErrorMessageResId(): Int = R.string.malformed
        }

        object BlankAndRequired : Invalid() {
            override fun shouldShowError(hasFocus: Boolean): Boolean = false
            override fun getErrorMessageResId(): Int = R.string.blank_and_required
        }
    }
}