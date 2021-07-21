package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.R

internal class TextFieldStateConstants {
    sealed class Valid : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean): Boolean = false
        override fun isValid(): Boolean = true

        @StringRes
        override fun getErrorMessageResId(): Int? = null

        object Full : Valid() {
            override fun isFull(): Boolean = true
        }

        object Limitless : Valid() { // no auto-advance
            override fun isFull(): Boolean = false
        }
    }

    sealed class Error : TextFieldState {
        override fun isValid(): Boolean = false
        override fun isFull(): Boolean = false

        object AlwaysError : Error() {
            override fun shouldShowError(hasFocus: Boolean): Boolean = true

            @StringRes
            override fun getErrorMessageResId(): Int = R.string.invalid
        }

        object Incomplete : Error() {
            override fun shouldShowError(hasFocus: Boolean): Boolean = !hasFocus

            @StringRes
            override fun getErrorMessageResId(): Int = R.string.incomplete
        }

        class Invalid(@StringRes val errorMessage: Int? = null) : Error() {
            override fun shouldShowError(hasFocus: Boolean): Boolean = true

            @StringRes
            override fun getErrorMessageResId() = errorMessage ?: R.string.invalid
        }

        object Blank : Error() {
            override fun shouldShowError(hasFocus: Boolean): Boolean = false

            @StringRes
            override fun getErrorMessageResId(): Int = R.string.blank_and_required
        }
    }
}
