package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.uicore.R

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TextFieldStateConstants {
    sealed class Valid : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean): Boolean = false
        override fun isValid(): Boolean = true
        override fun getError(): FieldError? = null
        override fun isBlank(): Boolean = false

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
            override fun isBlank(): Boolean = false
        }

        class Invalid(
            @StringRes override val errorMessageResId: Int,
            override val formatArgs: Array<out Any>? = null,
            private val preventMoreInput: Boolean = false,
        ) : Error(errorMessageResId, formatArgs) {
            override fun shouldShowError(hasFocus: Boolean): Boolean = true
            override fun isBlank(): Boolean = false
            override fun isFull(): Boolean = preventMoreInput
        }

        object Blank : Error(R.string.stripe_blank_and_required) {
            override fun shouldShowError(hasFocus: Boolean): Boolean = false
            override fun isBlank(): Boolean = true
        }
    }
}
