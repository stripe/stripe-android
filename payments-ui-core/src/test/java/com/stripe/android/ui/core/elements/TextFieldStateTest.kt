package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.uicore.elements.canAcceptInput
import org.junit.Test

class TextFieldStateTest {

    @Test
    fun `Accepts input on a text field that's not full`() {
        val fieldState = TextFieldStateConstants.Error.Incomplete(
            errorMessageResId = R.string.invalid_cvc
        )

        val didAccept = fieldState.canAcceptInput(
            currentValue = "12",
            proposedValue = "123"
        )

        assertThat(didAccept).isTrue()
    }

    @Test
    fun `Accepts input on a full text field if it's a deletion`() {
        val fieldState = TextFieldStateConstants.Valid.Full

        val didAccept = fieldState.canAcceptInput(
            currentValue = "123",
            proposedValue = "12"
        )

        assertThat(didAccept).isTrue()
    }

    @Test
    fun `Does not accept input if text field is full`() {
        val fieldState = TextFieldStateConstants.Valid.Full

        val didAccept = fieldState.canAcceptInput(
            currentValue = "123",
            proposedValue = "1234"
        )

        assertThat(didAccept).isFalse()
    }
}
