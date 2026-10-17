package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.FieldValidationMessage
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.uicore.elements.canAcceptInput
import org.junit.Test
import com.stripe.android.R as StripeR

class TextFieldStateTest {

    @Test
    fun `Accepts input on a text field that's not full`() {
        val fieldState = TextFieldStateConstants.Error.Incomplete(
            errorMessageResId = StripeR.string.stripe_invalid_cvc
        )

        val didAccept = fieldState.canAcceptInput(
            currentValue = "12",
            proposedValue = "123"
        )

        assertThat(didAccept).isTrue()
    }

    @Test
    fun `Accepts input on a full text field if it's a deletion`() {
        val fieldState = TextFieldStateConstants.Valid.Full()

        val didAccept = fieldState.canAcceptInput(
            currentValue = "123",
            proposedValue = "12"
        )

        assertThat(didAccept).isTrue()
    }

    @Test
    fun `Does not accept input if text field is full`() {
        val fieldState = TextFieldStateConstants.Valid.Full()

        val didAccept = fieldState.canAcceptInput(
            currentValue = "123",
            proposedValue = "1234"
        )

        assertThat(didAccept).isFalse()
    }

    @Test
    fun `Valid Full with warning message is still valid`() {
        val fieldState = TextFieldStateConstants.Valid.Full(
            validationMessage = FieldValidationMessage.Warning(
                message = StripeR.string.stripe_invalid_cvc
            )
        )

        assertThat(fieldState.isValid()).isTrue()
        assertThat(fieldState.isFull()).isTrue()
    }

    @Test
    fun `Valid Full with warning message shows validation message`() {
        val warningMessage = FieldValidationMessage.Warning(
            message = StripeR.string.stripe_invalid_cvc
        )
        val fieldState = TextFieldStateConstants.Valid.Full(
            validationMessage = warningMessage
        )

        assertThat(fieldState.shouldShowValidationMessage(hasFocus = false, isValidating = false))
            .isTrue()
        assertThat(fieldState.getValidationMessage()).isEqualTo(warningMessage)
    }

    @Test
    fun `Valid Full with warning message shows validation message even when focused`() {
        val warningMessage = FieldValidationMessage.Warning(
            message = StripeR.string.stripe_invalid_cvc
        )
        val fieldState = TextFieldStateConstants.Valid.Full(
            validationMessage = warningMessage
        )

        assertThat(fieldState.shouldShowValidationMessage(hasFocus = true, isValidating = false))
            .isTrue()
    }

    @Test
    fun `Valid Full without validation message does not show validation message`() {
        val fieldState = TextFieldStateConstants.Valid.Full()

        assertThat(fieldState.shouldShowValidationMessage(hasFocus = false, isValidating = false))
            .isFalse()
        assertThat(fieldState.getValidationMessage()).isNull()
    }

    @Test
    fun `Valid Full with warning does not accept input if text field is full`() {
        val fieldState = TextFieldStateConstants.Valid.Full(
            validationMessage = FieldValidationMessage.Warning(
                message = StripeR.string.stripe_invalid_cvc
            )
        )

        val didAccept = fieldState.canAcceptInput(
            currentValue = "123",
            proposedValue = "1234"
        )

        assertThat(didAccept).isFalse()
    }

    @Test
    fun `Valid Full with warning accepts input if it's a deletion`() {
        val fieldState = TextFieldStateConstants.Valid.Full(
            validationMessage = FieldValidationMessage.Warning(
                message = StripeR.string.stripe_invalid_cvc
            )
        )

        val didAccept = fieldState.canAcceptInput(
            currentValue = "123",
            proposedValue = "12"
        )

        assertThat(didAccept).isTrue()
    }
}
