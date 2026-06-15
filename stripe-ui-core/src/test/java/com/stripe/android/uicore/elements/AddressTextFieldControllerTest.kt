package com.stripe.android.uicore.elements

import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.R
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AddressTextFieldControllerTest {
    @Test
    fun `on raw field change, should not update value or field value states`() = runTest {
        val controller = createAddressController()

        turbineScope {
            val rawFieldValueTurbine = controller.rawFieldValue.testIn(this)
            val fieldValueTurbine = controller.fieldValue.testIn(this)

            assertThat(rawFieldValueTurbine.awaitItem()).isEqualTo("")
            assertThat(fieldValueTurbine.awaitItem()).isEqualTo("")

            controller.onRawValueChange("A new value")

            rawFieldValueTurbine.expectNoEvents()
            fieldValueTurbine.expectNoEvents()

            rawFieldValueTurbine.cancelAndIgnoreRemainingEvents()
            fieldValueTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Verify 'onValidationStateChanged' has visible error`() = runTest {
        val controller = createAddressController()

        turbineScope {
            val errorTurbine = controller.validationMessage.testIn(this)

            assertThat(errorTurbine.awaitItem()).isNull()

            controller.onValidationStateChanged(true)

            assertThat(errorTurbine.awaitItem()?.message).isEqualTo(R.string.stripe_blank_and_required)

            errorTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `non-inline mode - textFieldState is not editable with empty value`() = runTest {
        val controller = createAddressController()

        turbineScope {
            val stateTurbine = controller.textFieldState.testIn(this)
            val state = stateTurbine.awaitItem()

            assertThat(state.isEditable).isFalse()
            assertThat(state.value).isEqualTo("")
            assertThat(state.fieldDisplayState).isEqualTo(FieldDisplayState.NORMAL)
            assertThat(state.showDisabledErrorIndicator).isFalse()

            stateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `non-inline mode - shows disabled error indicator when validating`() = runTest {
        val controller = createAddressController()

        turbineScope {
            val stateTurbine = controller.textFieldState.testIn(this)
            stateTurbine.awaitItem() // initial

            controller.onValidationStateChanged(true)

            val state = stateTurbine.awaitItem()
            assertThat(state.isEditable).isFalse()
            assertThat(state.fieldDisplayState).isEqualTo(FieldDisplayState.ERROR)
            assertThat(state.showDisabledErrorIndicator).isTrue()

            stateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `inline mode - textFieldState is editable and tracks query`() = runTest {
        val controller = createAddressController(isInlineEnabled = true)

        turbineScope {
            val stateTurbine = controller.textFieldState.testIn(this)
            assertThat(stateTurbine.awaitItem().isEditable).isTrue()

            controller.onInlineQueryChanged("123 Main")

            val state = stateTurbine.awaitItem()
            assertThat(state.value).isEqualTo("123 Main")
            assertThat(state.isEditable).isTrue()

            stateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `inline mode - error does not show disabled indicator`() = runTest {
        val controller = createAddressController(isInlineEnabled = true)

        turbineScope {
            val stateTurbine = controller.textFieldState.testIn(this)
            stateTurbine.awaitItem() // initial

            controller.onValidationStateChanged(true)

            val state = stateTurbine.awaitItem()
            assertThat(state.fieldDisplayState).isEqualTo(FieldDisplayState.ERROR)
            assertThat(state.showDisabledErrorIndicator).isFalse()

            stateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `inline mode - query ignored when not in inline mode`() = runTest {
        val controller = createAddressController()

        turbineScope {
            val stateTurbine = controller.textFieldState.testIn(this)
            stateTurbine.awaitItem() // initial

            controller.onInlineQueryChanged("should be ignored")

            stateTurbine.expectNoEvents()
            stateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createAddressController(
        isInlineEnabled: Boolean = false,
    ): AddressTextFieldController {
        return AddressTextFieldController(
            label = resolvableString(value = "Name"),
            isInlineEnabled = isInlineEnabled,
        )
    }
}
