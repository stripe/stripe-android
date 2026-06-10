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
    fun `onInlineQueryChanged updates inlineQuery`() = runTest {
        val controller = createAddressController()

        turbineScope {
            val inlineQueryTurbine = controller.inlineQuery.testIn(this)

            assertThat(inlineQueryTurbine.awaitItem()).isEqualTo("")

            controller.onInlineQueryChanged("123 Main")
            assertThat(inlineQueryTurbine.awaitItem()).isEqualTo("123 Main")

            inlineQueryTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createAddressController(): AddressTextFieldController {
        return AddressTextFieldController(
            label = resolvableString(value = "Name"),
        )
    }
}
