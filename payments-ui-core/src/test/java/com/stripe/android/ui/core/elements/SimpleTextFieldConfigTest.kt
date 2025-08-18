package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.R
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import org.junit.Test

class SimpleTextFieldConfigTest {
    @Test
    fun `test number keyboards only accept numbers`() {
        val textConfig = SimpleTextFieldConfig(
            label = resolvableString("Phone"),
            keyboard = KeyboardType.Number
        )

        assertThat(textConfig.filter("abc123")).isEqualTo("123")
    }

    @Test
    fun `test number password keyboards only accept numbers`() {
        val textConfig = SimpleTextFieldConfig(
            label = resolvableString("Password"),
            keyboard = KeyboardType.NumberPassword
        )

        assertThat(textConfig.filter("abc123")).isEqualTo("123")
    }

    @Test
    fun `test when optional, state should be valid & have no field error`() {
        val textConfig = SimpleTextFieldConfig(
            label = resolvableString("Name (optional)"),
            keyboard = KeyboardType.Text,
            optional = true,
        )

        val state = textConfig.determineState("")

        assertThat(state.isValid()).isTrue()
        assertThat(state.getError()).isNull()
    }

    @Test
    fun `test when optional but blank, state should be invalid & have field error`() {
        val textConfig = SimpleTextFieldConfig(
            label = resolvableString("Name (optional)"),
            keyboard = KeyboardType.Text,
            optional = true,
        )

        val state = textConfig.determineState("    ")

        assertThat(state.isValid()).isFalse()
        assertThat(state.getError()?.errorMessage).isEqualTo(R.string.stripe_blank_and_required)
    }

    @Test
    fun `test when required, state should be invalid & have field error`() {
        val textConfig = SimpleTextFieldConfig(
            label = resolvableString("Name (optional)"),
            keyboard = KeyboardType.Text,
            optional = false,
        )

        val state = textConfig.determineState("")

        assertThat(state.isValid()).isFalse()
        assertThat(state.getError()?.errorMessage).isEqualTo(R.string.stripe_blank_and_required)
    }
}
