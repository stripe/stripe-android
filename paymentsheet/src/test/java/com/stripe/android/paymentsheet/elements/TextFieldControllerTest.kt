package com.stripe.android.paymentsheet.elements

import android.os.Build
import android.os.Looper.getMainLooper
import androidx.annotation.StringRes
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.asLiveData
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.common.TextFieldConfig
import com.stripe.android.paymentsheet.elements.common.TextFieldController
import com.stripe.android.paymentsheet.elements.common.TextFieldState
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
internal class TextFieldControllerTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val config = TestConfig()

    private val textFieldElement = TextFieldController(config)

    @Test
    fun `verify onValueChange sets the paramValue`() {
        config.fakeState = Valid.Limitless

        var paramValue: String? = null
        textFieldElement.fieldValue.asLiveData()
            .observeForever {
                paramValue = it
            }
        textFieldElement.onValueChange("newValue")
        assertThat(paramValue).isEqualTo("newValue")
    }

    @Test
    fun `verify the error message is set when should be visible`() {
        config.fakeState = ShowWhenNoFocus

        var errorMessageResId: Int? = 5
        textFieldElement.errorMessage.asLiveData()
            .observeForever {
                errorMessageResId = it
            }

        textFieldElement.onValueChange("newValue")
        shadowOf(getMainLooper()).idle()
        assertThat(errorMessageResId).isEqualTo(R.string.incomplete)
    }

    @Test
    fun `Verify is full set when the element state changes`() {
        config.fakeState = Valid.Full

        var isFull = false
        textFieldElement.isFull.asLiveData()
            .observeForever {
                isFull = it
            }

        textFieldElement.onValueChange("newValue")
        assertThat(isFull).isEqualTo(true)
    }

    @Test
    fun `Verify is not full set when the element state changes`() {
        config.fakeState = Valid.Limitless

        var isFull = false
        textFieldElement.isFull.asLiveData()
            .observeForever {
                isFull = it
            }

        textFieldElement.onValueChange("newValue")
        assertThat(isFull).isEqualTo(false)
    }

    @Test
    fun `Verify is not complete set when the element state changes`() {
        config.fakeState = Error.AlwaysError

        var isComplete = true
        textFieldElement.isComplete.asLiveData()
            .observeForever {
                isComplete = it
            }

        assertThat(isComplete).isEqualTo(true)
        textFieldElement.onValueChange("newValue")
        assertThat(isComplete).isEqualTo(false)
    }


    @Test
    fun `Verify is complete set when the element state changes`() {

        var isComplete = false
        textFieldElement.isComplete.asLiveData()
            .observeForever {
                isComplete = it
            }

        config.fakeState = Error.AlwaysError
        textFieldElement.onValueChange("newValue")
        assertThat(isComplete).isEqualTo(false)

        config.fakeState = Valid.Limitless
        textFieldElement.onValueChange("newValue")
        assertThat(isComplete).isEqualTo(true)
    }

    @Test
    fun `Verify is visible error is true when onValueChange and shouldShowError returns true`() {
        var visibleError = false
        textFieldElement.visibleError.asLiveData().observeForever {
            visibleError = it
        }

        config.fakeState = Valid.Full
        textFieldElement.onValueChange("full")
        assertThat(visibleError).isEqualTo(false)

        config.fakeState = Error.AlwaysError
        textFieldElement.onValueChange("always")
        shadowOf(getMainLooper()).idle()
        assertThat(visibleError).isEqualTo(true)
    }

    @Test
    fun `Verify is visible error set when the element state changes`() {
        config.fakeState = Valid.Limitless

        var visibleError = false
        textFieldElement.visibleError.asLiveData()
            .observeForever {
                visibleError = it
            }

        assertThat(visibleError).isEqualTo(false)

        config.fakeState = Error.AlwaysError
        textFieldElement.onValueChange("newValue")
        shadowOf(getMainLooper()).idle()
        assertThat(visibleError).isEqualTo(true)
    }

    @Test
    fun `Verify correct value passed to config should show error`() {
        config.fakeState = ShowWhenNoFocus
        //Initialize the fieldState
        textFieldElement.onValueChange("1a2b3c4d")

        var visibleError = false
        textFieldElement.visibleError.asLiveData()
            .observeForever {
                visibleError = it
            }

        textFieldElement.onFocusChange(false)
        assertThat(visibleError).isEqualTo(true)

        textFieldElement.onFocusChange(true)
        shadowOf(getMainLooper()).idle()
        assertThat(visibleError).isEqualTo(false)
    }

    @Test
    fun `Verify filter is called to set the input value`() {
        val numberConfigFilter = TestConfigFilter()
        val textFieldElement = TextFieldController(numberConfigFilter)

        var inputValue = ""
        textFieldElement.fieldValue.asLiveData()
            .observeForever {
                inputValue = it
            }

        textFieldElement.onValueChange("1a2b3c4d")
        shadowOf(getMainLooper()).idle()
        assertThat(inputValue).isEqualTo("1234")
    }

    private class TestConfig : TextFieldConfig {
        override val debugLabel = "debugLabel"
        @StringRes
        override val label: Int = R.string.address_label_name
        override val keyboard: KeyboardType = KeyboardType.Ascii

        var fakeState: TextFieldState = Valid.Limitless

        override fun determineState(input: String): TextFieldState =
            fakeState

        override fun filter(userTyped: String): String = userTyped
    }

    private class TestConfigFilter : TextFieldConfig {
        override val debugLabel = "debugLabel"
        @StringRes
        override val label: Int = R.string.address_label_name
        override val keyboard: KeyboardType = KeyboardType.Ascii

        val fakeState: TextFieldState = Valid.Limitless

        override fun determineState(input: String): TextFieldState = fakeState

        override fun filter(userTyped: String): String = userTyped.filter { Character.isDigit(it) }
    }

    companion object {
        object ShowWhenNoFocus : TextFieldState {
            override fun isValid(): Boolean = false
            override fun isFull(): Boolean = false
            override fun shouldShowError(hasFocus: Boolean): Boolean = !hasFocus
            @StringRes
            override fun getErrorMessageResId(): Int = R.string.incomplete
        }
    }

}