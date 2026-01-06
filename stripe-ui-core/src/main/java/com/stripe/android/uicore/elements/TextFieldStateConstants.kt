package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.uicore.R

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TextFieldStateConstants {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class Valid : TextFieldState {
        override fun shouldShowValidationMessage(hasFocus: Boolean, isValidating: Boolean): Boolean = false
        override fun isValid(): Boolean = true
        override fun getValidationMessage(): FieldValidationMessage? = null
        override fun isBlank(): Boolean = false

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Full(
            private val validationMessage: FieldValidationMessage? = null
        ) : Valid() {
            override fun isFull(): Boolean = true

            override fun getValidationMessage() = validationMessage
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        object Limitless : Valid() { // no auto-advance
            override fun isFull(): Boolean = false
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class Error(
        private val validationMessage: FieldValidationMessage
    ) : TextFieldState {

        constructor(
            @StringRes errorMessageResId: Int,
            formatArgs: Array<out Any>? = null
        ) : this(FieldValidationMessage.Error(errorMessageResId, formatArgs))

        override fun isValid(): Boolean = false
        override fun isFull(): Boolean = false
        override fun getValidationMessage() = validationMessage

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Incomplete(
            validationMessage: FieldValidationMessage
        ) : Error(validationMessage) {

            constructor(
                @StringRes errorMessageResId: Int
            ) : this(FieldValidationMessage.Error(errorMessageResId))

            override fun shouldShowValidationMessage(hasFocus: Boolean, isValidating: Boolean): Boolean =
                !hasFocus || isValidating

            override fun isBlank(): Boolean = false
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Invalid(
            validationMessage: FieldValidationMessage,
            private val preventMoreInput: Boolean = false,
        ) : Error(validationMessage) {

            constructor(
                @StringRes errorMessageResId: Int,
                formatArgs: Array<out Any>? = null,
                preventMoreInput: Boolean = false,
            ) : this(
                validationMessage = FieldValidationMessage.Error(errorMessageResId, formatArgs),
                preventMoreInput = preventMoreInput
            )

            override fun shouldShowValidationMessage(hasFocus: Boolean, isValidating: Boolean): Boolean = true
            override fun isBlank(): Boolean = false
            override fun isFull(): Boolean = preventMoreInput
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        object Blank : Error(R.string.stripe_blank_and_required) {
            override fun shouldShowValidationMessage(hasFocus: Boolean, isValidating: Boolean): Boolean = isValidating
            override fun isBlank(): Boolean = true
        }
    }
}
