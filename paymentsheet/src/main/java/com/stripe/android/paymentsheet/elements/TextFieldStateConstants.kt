package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.R

internal class TextFieldStateConstants {
    sealed class Valid : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean): Boolean = false
        override fun isValid(): Boolean = true
        override fun getError(): FieldError? = null

        object Full : Valid() {
            override fun isFull(): Boolean = true
        }

        object Limitless : Valid() { // no auto-advance
            override fun isFull(): Boolean = false
        }
    }

    sealed class Error(
        @StringRes protected open val errorMessageResId: Int,
        protected open val formatArgs: Array<out Any>? = null
    ) : TextFieldState {
        override fun isValid(): Boolean = false
        override fun isFull(): Boolean = false
        override fun getError() = FieldError(errorMessageResId, formatArgs)

        class Incomplete(
            @StringRes override val errorMessageResId: Int
        ) : Error(errorMessageResId) {
            override fun shouldShowError(hasFocus: Boolean): Boolean = !hasFocus
        }

        class Invalid(
            @StringRes override val errorMessageResId: Int,
            override val formatArgs: Array<out Any>? = null
        ) : Error(errorMessageResId, formatArgs) {
            override fun shouldShowError(hasFocus: Boolean): Boolean = true
        }

        object Blank : Error(R.string.blank_and_required) {
            override fun shouldShowError(hasFocus: Boolean): Boolean = false
        }
    }
}
