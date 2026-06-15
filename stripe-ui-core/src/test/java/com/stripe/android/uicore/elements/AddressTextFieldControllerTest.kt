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
        val controller = createAddressController(isInlineAutocompleteEnabled = false)

        turbineScope {
            val stateTurbine = controller.textFieldState.testIn(this)

            val state = stateTurbine.awaitItem()
            assertThat(state.isEditable).isFalse()
            assertThat(state.value).isEqualTo("")

            stateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `non-inline mode - shows disabled error indicator when validating`() = runTest {
        val controller = createAddressController(isInlineAutocompleteEnabled = false)

        turbineScope {
            val stateTurbine = controller.textFieldState.testIn(this)

            stateTurbine.awaitItem() // initial state

            controller.onValidationStateChanged(true)

            val state = stateTurbine.awaitItem()
            assertThat(state.showDisabledErrorIndicator).isTrue()
            assertThat(state.fieldDisplayState).isEqualTo(FieldDisplayState.ERROR)

            stateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `inline mode - textFieldState is editable and tracks query`() = runTest {
        val controller = createAddressController(isInlineAutocompleteEnabled = true)

        turbineScope {
            val stateTurbine = controller.textFieldState.testIn(this)

            val initial = stateTurbine.awaitItem()
            assertThat(initial.isEditable).isTrue()
            assertThat(initial.value).isEqualTo("")

            controller.onInlineQueryChanged("123 Main St")

            val updated = stateTurbine.awaitItem()
            assertThat(updated.value).isEqualTo("123 Main St")

            stateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `inline mode - error does not show disabled indicator`() = runTest {
        val controller = createAddressController(isInlineAutocompleteEnabled = true)

        turbineScope {
            val stateTurbine = controller.textFieldState.testIn(this)

            stateTurbine.awaitItem() // initial state

            controller.onValidationStateChanged(true)

            val state = stateTurbine.awaitItem()
            assertThat(state.showDisabledErrorIndicator).isFalse()
            assertThat(state.fieldDisplayState).isEqualTo(FieldDisplayState.ERROR)

            stateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `non-inline mode - query ignored when inline is disabled`() = runTest {
        val controller = createAddressController(isInlineAutocompleteEnabled = false)

        turbineScope {
            val stateTurbine = controller.textFieldState.testIn(this)

            stateTurbine.awaitItem() // initial state

            controller.onInlineQueryChanged("should be ignored")

            stateTurbine.expectNoEvents()

            stateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createAddressController(isInlineAutocompleteEnabled: Boolean = false): AddressTextFieldController {
        return AddressTextFieldController(
            label = resolvableString(value = "Name"),
            isInlineAutocompleteEnabled = isInlineAutocompleteEnabled,
        )
    }
}
