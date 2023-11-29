package com.stripe.android.uicore.elements

import android.os.Build
import android.os.Looper.getMainLooper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Blank
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Invalid
import com.stripe.android.uicore.elements.TextFieldStateConstants.Valid.Full
import com.stripe.android.uicore.elements.TextFieldStateConstants.Valid.Limitless
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import com.stripe.android.core.R as CoreR

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
internal class SimpleTextFieldControllerTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun `verify onValueChange sets the paramValue`() = runTest {
        val controller = createControllerWithState()

        controller.fieldValue.test {
            assertThat(awaitItem()).isEmpty()
            controller.onValueChange("limitless")
            assertThat(awaitItem()).isEqualTo("limitless")
        }
    }

    @Test
    fun `verify the error message is set when should be visible`() = runTest {
        val controller = createControllerWithState()

        controller.error.test {
            assertThat(awaitItem()).isNull()
            controller.onValueChange("showWhenNoFocus")
            shadowOf(getMainLooper()).idle()
            assertThat(awaitItem()).isEqualTo(ShowWhenNoFocus.getError())
        }
    }

    @Test
    fun `Verify is full set when the controller field state changes`() = runTest {
        val controller = createControllerWithState()

        controller.fieldState.test {
            assertThat(awaitItem().isFull()).isEqualTo(false)

            controller.onValueChange("full")
            assertThat(awaitItem().isFull()).isEqualTo(true)
        }
    }

    @Test
    fun `Verify is not full set when the controller field state changes`() = runTest {
        val controller = createControllerWithState()

        controller.fieldState.test {
            skipItems(1)
            controller.onValueChange("limitless")
            assertThat(awaitItem().isFull()).isEqualTo(false)
        }
    }

    @Test
    fun `Verify is not complete set when the controller field state changes`() = runTest {
        val controller = createControllerWithState()
        controller.onValueChange("full")

        controller.isComplete.test {
            assertThat(awaitItem()).isEqualTo(true)
            controller.onValueChange("invalid")
            assertThat(awaitItem()).isEqualTo(false)
        }
    }

    @Test
    fun `Verify is complete set when the controller field state changes`() = runTest {
        val controller = createControllerWithState()

        controller.isComplete.test {
            assertThat(awaitItem()).isEqualTo(false)
            controller.onValueChange("invalid")
            assertThat(awaitItem()).isEqualTo(false)

            controller.onValueChange("limitless")
            assertThat(awaitItem()).isEqualTo(true)
        }
    }

    @Test
    fun `Verify is blank optional fields are considered complete`() = runTest {
        val controller = createControllerWithState(showOptionalLabel = true)
        controller.onValueChange("invalid")

        controller.isComplete.test {
            assertThat(awaitItem()).isEqualTo(false)
            controller.onValueChange("")
            assertThat(awaitItem()).isEqualTo(true)
        }
    }

    @Test
    fun `Verify is visible error is true when onValueChange and shouldShowError returns true`() = runTest {
        val controller = createControllerWithState()
        controller.visibleError.test {
            assertThat(awaitItem()).isEqualTo(false)

            controller.onValueChange("full")
            assertThat(awaitItem()).isEqualTo(false)

            createControllerWithState()
            controller.onValueChange("invalid")
            shadowOf(getMainLooper()).idle()
            assertThat(awaitItem()).isEqualTo(true)
        }
    }

    @Test
    fun `Verify is visible error set when the controller field state changes`() = runTest {
        // We check both the visible state and the error object.  In the case of
        // an incomplete field the error object should be null when the visible error goes away.

        val controller = createControllerWithState()

        val visibleErrors = controller.visibleError.testIn(backgroundScope)

        val errors = controller.error.testIn(backgroundScope)

        assertThat(visibleErrors.awaitItem()).isEqualTo(false)
        assertThat(errors.awaitItem()).isNull()

        controller.onValueChange("invalid")
        shadowOf(getMainLooper()).idle()

        assertThat(visibleErrors.awaitItem()).isEqualTo(true)
        assertThat(errors.awaitItem()).isNotNull()

        controller.onValueChange("full")
        shadowOf(getMainLooper()).idle()

        assertThat(visibleErrors.awaitItem()).isEqualTo(false)
        assertThat(errors.awaitItem()).isNull()
    }

    @Test
    fun `Verify correct value passed to config should show error`() = runTest {
        val controller = createControllerWithState()

        // Initialize the fieldState
        controller.onValueChange("showWhenNoFocus")

        controller.visibleError.test {
            controller.onFocusChange(false)
            assertThat(awaitItem()).isEqualTo(true)

            controller.onFocusChange(true)
            shadowOf(getMainLooper()).idle()
            assertThat(awaitItem()).isEqualTo(false)
        }
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

    @Test
    fun `Verify null label`() {
        val controller = createControllerWithState(nullLabel = true)
        assertThat(controller.label.value).isNull()
    }

    @Test
    fun `Verify non-null label`() {
        val controller = createControllerWithState(nullLabel = false)
        assertThat(controller.label.value).isEqualTo(CoreR.string.stripe_address_label_full_name)
    }

    @Test
    fun `Verify null placeHolder`() {
        val controller = createControllerWithState(nullPlaceHolder = true)
        assertThat(controller.placeHolder.value).isNull()
    }

    @Test
    fun `Verify non-null placeHolder`() {
        val controller = createControllerWithState(nullPlaceHolder = false)
        assertThat(controller.placeHolder.value).isNotNull()
    }

    private fun createControllerWithState(
        showOptionalLabel: Boolean = false,
        nullLabel: Boolean = false,
        nullPlaceHolder: Boolean = true
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

            if (nullLabel) {
                on { label } doReturn null
            } else {
                on { label } doReturn CoreR.string.stripe_address_label_full_name
            }

            if (!nullPlaceHolder) {
                on { placeHolder } doReturn "PlaceHolder"
            }
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
