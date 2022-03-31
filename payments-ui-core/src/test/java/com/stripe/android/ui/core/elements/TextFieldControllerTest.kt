package com.stripe.android.ui.core.elements

import android.os.Build
import android.os.Looper.getMainLooper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.asLiveData
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.TextFieldStateConstants.Error.Blank
import com.stripe.android.ui.core.elements.TextFieldStateConstants.Error.Invalid
import com.stripe.android.ui.core.elements.TextFieldStateConstants.Valid.Full
import com.stripe.android.ui.core.elements.TextFieldStateConstants.Valid.Limitless
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
internal class TextFieldControllerTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun `verify onValueChange sets the paramValue`() {
        val controller = createControllerWithState()

        var paramValue: String? = null
        controller.fieldValue.asLiveData()
            .observeForever {
                paramValue = it
            }
        controller.onValueChange("limitless")
        assertThat(paramValue).isEqualTo("limitless")
    }

    @Test
    fun `verify the error message is set when should be visible`() {
        val controller = createControllerWithState()

        var fieldError: FieldError? = FieldError(5)
        controller.error.asLiveData()
            .observeForever {
                fieldError = it
            }

        controller.onValueChange("showWhenNoFocus")
        shadowOf(getMainLooper()).idle()
        assertThat(fieldError).isEqualTo(ShowWhenNoFocus.getError())
    }

    @Test
    fun `Verify is full set when the controller field state changes`() {
        val controller = createControllerWithState()

        var isFull = false
        controller.fieldState.asLiveData()
            .observeForever {
                isFull = it.isFull()
            }

        controller.onValueChange("full")
        assertThat(isFull).isEqualTo(true)
    }

    @Test
    fun `Verify is not full set when the controller field state changes`() {
        val controller = createControllerWithState()

        var isFull = false
        controller.fieldState.asLiveData()
            .observeForever {
                isFull = it.isFull()
            }

        controller.onValueChange("limitless")
        assertThat(isFull).isEqualTo(false)
    }

    @Test
    fun `Verify is not complete set when the controller field state changes`() {
        val controller = createControllerWithState()
        controller.onValueChange("full")

        var isComplete = true
        controller.isComplete.asLiveData()
            .observeForever {
                isComplete = it
            }

        assertThat(isComplete).isEqualTo(true)
        controller.onValueChange("invalid")
        assertThat(isComplete).isEqualTo(false)
    }

    @Test
    fun `Verify is complete set when the controller field state changes`() {
        val controller = createControllerWithState()

        var isComplete = false
        controller.isComplete.asLiveData()
            .observeForever {
                isComplete = it
            }

        controller.onValueChange("invalid")
        assertThat(isComplete).isEqualTo(false)

        controller.onValueChange("limitless")
        assertThat(isComplete).isEqualTo(true)
    }

    @Test
    fun `Verify is blank optional fields are considered complete`() {
        val controller = createControllerWithState(showOptionalLabel = true)
        controller.onValueChange("invalid")

        var isComplete = true
        controller.isComplete.asLiveData()
            .observeForever {
                isComplete = it
            }

        assertThat(isComplete).isEqualTo(false)
        controller.onValueChange("")
        assertThat(isComplete).isEqualTo(true)
    }

    @Test
    fun `Verify is visible error is true when onValueChange and shouldShowError returns true`() {
        val controller = createControllerWithState()
        var visibleError = false
        controller.visibleError.asLiveData().observeForever {
            visibleError = it
        }

        controller.onValueChange("full")
        assertThat(visibleError).isEqualTo(false)

        createControllerWithState()
        controller.onValueChange("invalid")
        shadowOf(getMainLooper()).idle()
        assertThat(visibleError).isEqualTo(true)
    }

    @Test
    fun `Verify is visible error set when the controller field state changes`() {
        // We check both the visible state and the error object.  In the case of
        // an incomplete field the error object should be null when the visible error goes away.

        val controller = createControllerWithState()

        var visibleError = false
        controller.visibleError.asLiveData()
            .observeForever {
                visibleError = it
            }

        val error = mutableListOf<FieldError?>()
        controller.error.asLiveData()
            .observeForever {
                error.add(it)
            }

        assertThat(visibleError).isEqualTo(false)
        assertThat(error.size).isEqualTo(1)
        assertThat(error[0]).isNull()

        controller.onValueChange("invalid")
        shadowOf(getMainLooper()).idle()

        assertThat(visibleError).isEqualTo(true)
        assertThat(error.size).isEqualTo(2)
        assertThat(error[1]).isNotNull()

        controller.onValueChange("full")
        shadowOf(getMainLooper()).idle()

        assertThat(visibleError).isEqualTo(false)
        assertThat(error.size).isEqualTo(3)
        assertThat(error[2]).isNull()
    }

    @Test
    fun `Verify correct value passed to config should show error`() {
        val controller = createControllerWithState()

        // Initialize the fieldState
        controller.onValueChange("showWhenNoFocus")

        var visibleError = false
        controller.visibleError.asLiveData()
            .observeForever {
                visibleError = it
            }

        controller.onFocusChange(false)
        assertThat(visibleError).isEqualTo(true)

        controller.onFocusChange(true)
        shadowOf(getMainLooper()).idle()
        assertThat(visibleError).isEqualTo(false)
    }

    @Test
    fun `Verify filter is called to set the input value`() {
        val config: TextFieldConfig = mock {
            on { determineState("1234") } doReturn Limitless
            on { filter("1a2b3c4d") } doReturn "1234"
        }

        val controller = SimpleTextFieldController(config)

        controller.onValueChange("1a2b3c4d")

        verify(config).filter("1a2b3c4d")
    }

    private fun createControllerWithState(
        showOptionalLabel: Boolean = false
    ): SimpleTextFieldController {
        val config: TextFieldConfig = mock {
            on { determineState("full") } doReturn Full
            on { filter("full") } doReturn "full"

            on { determineState("limitless") } doReturn Limitless
            on { filter("limitless") } doReturn "limitless"

            on { determineState("invalid") } doReturn Invalid(-1)
            on { filter("invalid") } doReturn "invalid"

            on { determineState("blank") } doReturn Blank
            on { filter("blank") } doReturn "blank"

            on { determineState("showWhenNoFocus") } doReturn ShowWhenNoFocus
            on { filter("showWhenNoFocus") } doReturn "showWhenNoFocus"

            // These are for the initial call to onValueChange("")
            on { determineState("") } doReturn Blank
            on { filter("") } doReturn ""

            on { label } doReturn R.string.address_label_name
        }

        return SimpleTextFieldController(config, showOptionalLabel)
    }

    companion object {
        val fieldError = FieldError(-1)

        object ShowWhenNoFocus : TextFieldState {
            override fun isValid(): Boolean = false
            override fun isFull(): Boolean = false
            override fun isBlank(): Boolean = false

            override fun shouldShowError(hasFocus: Boolean): Boolean = !hasFocus
            override fun getError() = fieldError
        }
    }
}
