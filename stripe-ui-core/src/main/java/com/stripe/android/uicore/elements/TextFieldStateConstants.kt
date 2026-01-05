package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.uicore.R

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TextFieldStateConstants {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class Valid : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean, isValidating: Boolean): Boolean = false
        override fun isValid(): Boolean = true
        override fun getError(): FieldError? = null
        override fun isBlank(): Boolean = false

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Full(
            @StringRes private val errorMessageResId: Int? = null,
            private val formatArgs: Array<out Any>? = null,
        ) : Valid() {
            override fun isFull(): Boolean = true

            override fun shouldShowError(hasFocus: Boolean, isValidating: Boolean): Boolean {
                return errorMessageResId != null
            }

            override fun getError(): FieldError? {
                return errorMessageResId?.let {
                    FieldError(it, formatArgs, isWarning = true)
                }
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        object Limitless : Valid() { // no auto-advance
            override fun isFull(): Boolean = false
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class Error(
        @StringRes protected open val errorMessageResId: Int,
        protected open val formatArgs: Array<out Any>? = null,
        protected open val isWarning: Boolean = false
    ) : TextFieldState {
        override fun isValid(): Boolean = isWarning.or(false)
        override fun isFull(): Boolean = false
        override fun getError() = FieldError(errorMessageResId, formatArgs, isWarning)

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Incomplete(
            @StringRes override val errorMessageResId: Int
        ) : Error(errorMessageResId) {
            override fun shouldShowError(hasFocus: Boolean, isValidating: Boolean): Boolean =
                !hasFocus || isValidating
            override fun isBlank(): Boolean = false
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Invalid(
            @StringRes override val errorMessageResId: Int,
            override val formatArgs: Array<out Any>? = null,
            private val preventMoreInput: Boolean = false,
            override val isWarning: Boolean = false
        ) : Error(errorMessageResId, formatArgs) {
            override fun shouldShowError(hasFocus: Boolean, isValidating: Boolean): Boolean = true
            override fun isBlank(): Boolean = false
            override fun isFull(): Boolean = preventMoreInput
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        object Blank : Error(R.string.stripe_blank_and_required) {
            override fun shouldShowError(hasFocus: Boolean, isValidating: Boolean): Boolean = isValidating
            override fun isBlank(): Boolean = true
        }
    }
}
