package com.stripe.android.paymentsheet.elements

import android.os.Build
import android.os.Looper.getMainLooper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.common.TextFieldConfig
import com.stripe.android.paymentsheet.elements.common.TextFieldElement
import com.stripe.android.paymentsheet.elements.common.TextFieldElementState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class TextFieldElementTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val config = TestConfig()
    private val textFieldElement = TextFieldElement(config)

    @Test
    fun `verify onValueChange sets the paramValue`() {
        config.fakeElementState = TextFieldElementState.TextFieldElementStateError(R.string.invalid)

        var paramValue: String? = null
        textFieldElement.input
            .observeForever {
                paramValue = it
            }
        textFieldElement.onValueChange("newValue")
        assertThat(paramValue).isEqualTo("newValue")
    }

    @Test
    fun `verify the error message is set when should be visible`() {
        config.fakeElementState = TextFieldElementState.TextFieldElementStateError(R.string.invalid)
        config.fakeShouldShowError = true

        var errorMessageResId = 5
        textFieldElement.errorMessage
            .observeForever {
                errorMessageResId = it
            }

        textFieldElement.onValueChange("newValue")
        shadowOf(getMainLooper()).idle()
        assertThat(errorMessageResId).isEqualTo(R.string.invalid)
    }

    @Test
    fun `Verify is full set when the element state changes`() {
        config.fakeElementState = object : TextFieldElementState.TextFieldElementStateValid() {
            override fun isFull() = true
        }

        var isFull = false
        textFieldElement.isFull
            .observeForever {
                isFull = it
            }

        textFieldElement.onValueChange("newValue")
        assertThat(isFull).isEqualTo(true)
    }

    @Test
    fun `Verify is not full set when the element state changes`() {
        config.fakeElementState = object : TextFieldElementState.TextFieldElementStateValid() {
            override fun isFull() = false
        }

        var isFull = false
        textFieldElement.isFull
            .observeForever {
                isFull = it
            }

        textFieldElement.onValueChange("newValue")
        assertThat(isFull).isEqualTo(false)
    }

    @Test
    fun `Verify is not complete set when the element state changes`() {
        config.fakeElementState = object : TextFieldElementState.TextFieldElementStateValid() {
            override fun isValid() = false
        }

        var isComplete = true
        textFieldElement.isComplete
            .observeForever {
                isComplete = it
            }

        textFieldElement.onValueChange("newValue")
        assertThat(isComplete).isEqualTo(false)
    }

    @Test
    fun `Verify is complete set when the element state changes`() {

        var isComplete = false
        textFieldElement.isComplete
            .observeForever {
                isComplete = it
            }

        config.fakeElementState = object : TextFieldElementState.TextFieldElementStateValid() {
            override fun isValid() = false
        }
        textFieldElement.onValueChange("newValue")
        assertThat(isComplete).isEqualTo(false)

        config.fakeElementState = object : TextFieldElementState.TextFieldElementStateValid() {
            override fun isValid() = true
        }
        textFieldElement.onValueChange("newValue")
        assertThat(isComplete).isEqualTo(true)
    }

    @Test
    fun `Verify is visible error is true when onValueChange and shouldShowError returns true`() {
        var visibleError = false
        textFieldElement.visibleError
            .observeForever {
                visibleError = it
            }

        config.fakeShouldShowError = false
        assertThat(visibleError).isEqualTo(false)

        config.fakeShouldShowError = true
        textFieldElement.onValueChange("newValue")
        shadowOf(getMainLooper()).idle()
        assertThat(visibleError).isEqualTo(true)
    }

    @Test
    fun `Verify is visible error set when the element state changes`() {
        config.fakeShouldShowError = false

        var visibleError = false
        textFieldElement.visibleError
            .observeForever {
                visibleError = it
            }

        assertThat(visibleError).isEqualTo(false)

        config.fakeElementState = TextFieldElementState.TextFieldElementStateError(R.string.invalid)
        config.fakeShouldShowError = true
        textFieldElement.onValueChange("newValue")
        shadowOf(getMainLooper()).idle()
        assertThat(visibleError).isEqualTo(true)
    }

    @Test
    fun `Verify correct value passed to config should show error`() {
        val configErrorBasedOnFocus = TestConfigErrorBasedOnFocus()
        val textFieldElementErrorBasedOnFocus = TextFieldElement(configErrorBasedOnFocus)

        var visibleError = false
        textFieldElementErrorBasedOnFocus.visibleError
            .observeForever {
                visibleError = it
            }

        textFieldElementErrorBasedOnFocus.onFocusChange(false)
        assertThat(visibleError).isEqualTo(false)

        textFieldElementErrorBasedOnFocus.onFocusChange(true)
        shadowOf(getMainLooper()).idle()
        assertThat(visibleError).isEqualTo(true)
    }

    @Test
    fun `Verify filter called on value change`() {
        val configErrorBasedOnFocus = TestConfigErrorBasedOnFocus()
        val textFieldElementErrorBasedOnFocus = TextFieldElement(configErrorBasedOnFocus)

        var visibleError = false
        textFieldElementErrorBasedOnFocus.visibleError
            .observeForever {
                visibleError = it
            }

        textFieldElementErrorBasedOnFocus.onFocusChange(false)
        assertThat(visibleError).isEqualTo(false)

        textFieldElementErrorBasedOnFocus.onFocusChange(true)
        shadowOf(getMainLooper()).idle()
        assertThat(visibleError).isEqualTo(true)
    }

    @Test
    fun `Verify filter is called to set the input value`() {
        val numberConfigFilter = TestConfigFilter()
        val textFieldElement = TextFieldElement(numberConfigFilter)

        var inputValue = ""
        textFieldElement.input
            .observeForever {
                inputValue = it
            }

        textFieldElement.onValueChange("1a2b3c4d")
        shadowOf(getMainLooper()).idle()
        assertThat(inputValue).isEqualTo("1234")
    }

    private class TestConfig : TextFieldConfig {
        override val debugLabel = "debugLabel"
        override val label: Int = R.string.address_label_name
        override val keyboard: KeyboardType = KeyboardType.Ascii

        var fakeShouldShowError = false
        var fakeElementState: TextFieldElementState =
            TextFieldElementState.TextFieldElementStateValid()

        override fun determineState(paramFormatted: String?): TextFieldElementState =
            fakeElementState

        override fun shouldShowError(
            elementState: TextFieldElementState,
            hasFocus: Boolean
        ) = fakeShouldShowError

        override fun filter(userTyped: String): String = userTyped
    }

    private class TestConfigErrorBasedOnFocus : TextFieldConfig {
        override val debugLabel = "debugLabel"
        override val label: Int = R.string.address_label_name
        override val keyboard: KeyboardType = KeyboardType.Ascii

        var fakeElementState: TextFieldElementState =
            TextFieldElementState.TextFieldElementStateValid()

        override fun determineState(paramFormatted: String?): TextFieldElementState =
            fakeElementState

        override fun shouldShowError(
            elementState: TextFieldElementState,
            hasFocus: Boolean
        ) = hasFocus

        override fun filter(userTyped: String): String = userTyped
    }

    private class TestConfigFilter : TextFieldConfig {
        override val debugLabel = "debugLabel"
        override val label: Int = R.string.address_label_name
        override val keyboard: KeyboardType = KeyboardType.Ascii

        override fun determineState(paramFormatted: String?): TextFieldElementState =
            TextFieldElementState.TextFieldElementStateValid()

        override fun shouldShowError(
            elementState: TextFieldElementState,
            hasFocus: Boolean
        ) = false

        override fun filter(userTyped: String): String = userTyped.filter { Character.isDigit(it) }
    }

}