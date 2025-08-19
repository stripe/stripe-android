package com.stripe.android.uicore.elements

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.R
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AddressTextFieldControllerTest {
    @Test
    fun `when optional, initial field state should be Valid & Limitless`() = runTest {
        val controller = createAddressController(optional = true)

        controller.fieldState.test {
            assertThat(awaitItem()).isEqualTo(TextFieldStateConstants.Valid.Limitless)
        }
    }

    @Test
    fun `when required, initial field state should be Error & Blank`() = runTest {
        val controller = createAddressController(optional = false)

        controller.fieldState.test {
            assertThat(awaitItem()).isEqualTo(TextFieldStateConstants.Error.Blank)
        }
    }

    @Test
    fun `Verify 'onValidationStateChanged' has visible error`() = runTest {
        val controller = createAddressController(
            optional = false
        )

        turbineScope {
            val visibleErrorTurbine = controller.visibleError.testIn(this)
            val errorTurbine = controller.error.testIn(this)

            assertThat(visibleErrorTurbine.awaitItem()).isFalse()
            assertThat(errorTurbine.awaitItem()).isNull()

            controller.onValidationStateChanged(true)

            assertThat(visibleErrorTurbine.awaitItem()).isTrue()
            assertThat(errorTurbine.awaitItem()?.errorMessage).isEqualTo(R.string.stripe_blank_and_required)

            visibleErrorTurbine.cancelAndIgnoreRemainingEvents()
            errorTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Verify 'onValidationStateChanged' has no visible error when optional`() = runTest {
        val controller = createAddressController(
            optional = true
        )

        turbineScope {
            val visibleErrorTurbine = controller.visibleError.testIn(this)
            val errorTurbine = controller.error.testIn(this)

            assertThat(visibleErrorTurbine.awaitItem()).isFalse()
            assertThat(errorTurbine.awaitItem()).isNull()

            controller.onValidationStateChanged(true)

            visibleErrorTurbine.expectNoEvents()
            errorTurbine.expectNoEvents()

            visibleErrorTurbine.cancelAndIgnoreRemainingEvents()
            errorTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createAddressController(
        optional: Boolean,
    ): AddressTextFieldController {
        return AddressTextFieldController(
            config = SimpleTextFieldConfig(
                label = resolvableString(value = "Name"),
                optional = optional,
            ),
        )
    }
}
