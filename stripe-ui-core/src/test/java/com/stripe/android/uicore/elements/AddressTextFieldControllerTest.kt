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
    fun `onValueChange updates fieldState when inline`() = runTest {
        val controller = AddressTextFieldController(
            label = resolvableString(value = "Address"),
            inlinePredictionsState = AutocompleteAddressInteractor.InlinePredictionsState,
        )

        turbineScope {
            val fieldStateTurbine = controller.fieldState.testIn(this)

            assertThat(fieldStateTurbine.awaitItem().value).isEqualTo("")

            controller.onValueChange("123 Main")
            assertThat(fieldStateTurbine.awaitItem().value).isEqualTo("123 Main")

            fieldStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onValueChange is no-op when not inline`() = runTest {
        val controller = createAddressController()

        turbineScope {
            val fieldStateTurbine = controller.fieldState.testIn(this)

            assertThat(fieldStateTurbine.awaitItem().value).isEqualTo("")

            controller.onValueChange("123 Main")
            fieldStateTurbine.expectNoEvents()

            fieldStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createAddressController(): AddressTextFieldController {
        return AddressTextFieldController(
            label = resolvableString(value = "Name"),
        )
    }
}
