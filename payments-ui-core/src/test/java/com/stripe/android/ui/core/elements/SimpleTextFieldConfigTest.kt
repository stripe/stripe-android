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

    @Test
    fun `test shouldShowError returns true when error exists and isValidating is true`() {
        val textConfig = SimpleTextFieldConfig(
            label = resolvableString("Name"),
            keyboard = KeyboardType.Text,
            optional = false,
        )

        val state = textConfig.determineState("")

        assertThat(state.shouldShowError(hasFocus = true, isValidating = true)).isTrue()
        assertThat(state.shouldShowError(hasFocus = false, isValidating = true)).isTrue()
    }

    @Test
    fun `test shouldShowError returns false when error exists but isValidating is false`() {
        val textConfig = SimpleTextFieldConfig(
            label = resolvableString("Name"),
            keyboard = KeyboardType.Text,
            optional = false,
        )

        val state = textConfig.determineState("")

        assertThat(state.shouldShowError(hasFocus = true, isValidating = false)).isFalse()
        assertThat(state.shouldShowError(hasFocus = false, isValidating = false)).isFalse()
    }

    @Test
    fun `test shouldShowError returns false when no error exists regardless of isValidating`() {
        val textConfig = SimpleTextFieldConfig(
            label = resolvableString("Name"),
            keyboard = KeyboardType.Text,
            optional = false,
        )

        val state = textConfig.determineState("valid input")

        assertThat(state.shouldShowError(hasFocus = true, isValidating = true)).isFalse()
        assertThat(state.shouldShowError(hasFocus = false, isValidating = true)).isFalse()
        assertThat(state.shouldShowError(hasFocus = true, isValidating = false)).isFalse()
        assertThat(state.shouldShowError(hasFocus = false, isValidating = false)).isFalse()
    }

    @Test
    fun `test shouldShowError with optional field when empty is valid`() {
        val textConfig = SimpleTextFieldConfig(
            label = resolvableString("Name (optional)"),
            keyboard = KeyboardType.Text,
            optional = true,
        )

        val state = textConfig.determineState("")

        // Optional field with empty input is valid -> no error should be shown
        assertThat(state.shouldShowError(hasFocus = true, isValidating = true)).isFalse()
        assertThat(state.shouldShowError(hasFocus = false, isValidating = true)).isFalse()
    }
}
