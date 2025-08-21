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
        val controller = createAddressController(optional = false)

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
        val controller = createAddressController(
            optional = false
        )

        turbineScope {
            val errorTurbine = controller.error.testIn(this)

            assertThat(errorTurbine.awaitItem()).isNull()

            controller.onValidationStateChanged(true)

            assertThat(errorTurbine.awaitItem()?.errorMessage).isEqualTo(R.string.stripe_blank_and_required)

            errorTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Verify 'onValidationStateChanged' has no visible error when optional`() = runTest {
        val controller = createAddressController(
            optional = true
        )

        turbineScope {
            val errorTurbine = controller.error.testIn(this)

            assertThat(errorTurbine.awaitItem()).isNull()

            controller.onValidationStateChanged(true)

            errorTurbine.expectNoEvents()

            errorTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createAddressController(
        optional: Boolean,
    ): AddressTextFieldController {
        return AddressTextFieldController(
            label = resolvableString(value = "Name"),
            optional = optional,
        )
    }
}
