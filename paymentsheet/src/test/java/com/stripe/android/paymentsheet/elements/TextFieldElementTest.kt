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
internal class TextFieldElementTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val config = TestConfig()
    private val shouldShowErrorDebug: (TextFieldElementState, Boolean) -> Boolean =
        { state, hasFocus ->
            when (state) {
                is TextFieldElement.Companion.Valid.Full -> false
                is TextFieldElement.Companion.Error.ShowInFocus -> !hasFocus
                is TextFieldElement.Companion.Error.ShowAlways -> true
                else -> config.shouldShowError(state, hasFocus)
            }
        }

    private val determineStateDebug: (String) -> TextFieldElementState = { str ->
        when {
            str.contains("full") -> TextFieldElement.Companion.Valid.Full
            str.contains("focus") -> TextFieldElement.Companion.Error.ShowInFocus
            str.contains("always") -> TextFieldElement.Companion.Error.ShowAlways
            else -> config.determineState(str)
        }
    }

    private val textFieldElement =
        TextFieldElement(config, shouldShowErrorDebug, determineStateDebug)


    @Test
    fun `verify onValueChange sets the paramValue`() {
        config.fakeElementState = Error.Incomplete

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
        config.fakeElementState = Error.Incomplete
        config.fakeShouldShowError = true

        var errorMessageResId = 5
        textFieldElement.errorMessage
            .observeForever {
                errorMessageResId = it
            }

        textFieldElement.onValueChange("newValue")
        shadowOf(getMainLooper()).idle()
        assertThat(errorMessageResId).isEqualTo(R.string.incomplete)
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

        config.fakeElementState = Error.Incomplete
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
        var fakeElementState: TextFieldElementState = Valid.Limitless

        override fun determineState(input: String): TextFieldElementState =
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

        var fakeElementState: TextFieldElementState = Valid.Limitless

        override fun determineState(input: String): TextFieldElementState =
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

        override fun determineState(input: String): TextFieldElementState = Valid.Limitless

        override fun shouldShowError(
            elementState: TextFieldElementState,
            hasFocus: Boolean
        ) = false

        override fun filter(userTyped: String): String = userTyped.filter { Character.isDigit(it) }
    }

    companion object {

        sealed class Valid : TextFieldElementState.TextFieldElementStateValid() {
            object Limitless : Valid() // no auto-advance
        }

        sealed class Error(stringResId: Int) :
            TextFieldElementState.TextFieldElementStateError(stringResId) {
            object Incomplete : Error(R.string.incomplete)
        }
    }

}