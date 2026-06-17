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
    fun `non-inline mode - is not editable`() = runTest {
        val controller = createAddressController(isInlineAutocompleteEnabled = false)

        assertThat(controller.isEditable).isFalse()
    }

    @Test
    fun `inline mode - is editable and tracks query`() = runTest {
        val controller = createAddressController(isInlineAutocompleteEnabled = true)

        assertThat(controller.isEditable).isTrue()

        turbineScope {
            val queryTurbine = controller.inlineQuery.testIn(this)

            assertThat(queryTurbine.awaitItem()).isEqualTo("")

            controller.onInlineQueryChanged("123 Main St")

            assertThat(queryTurbine.awaitItem()).isEqualTo("123 Main St")

            queryTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `non-inline mode - query ignored when inline is disabled`() = runTest {
        val controller = createAddressController(isInlineAutocompleteEnabled = false)

        turbineScope {
            val queryTurbine = controller.inlineQuery.testIn(this)

            queryTurbine.awaitItem() // initial ""

            controller.onInlineQueryChanged("should be ignored")

            queryTurbine.expectNoEvents()

            queryTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createAddressController(isInlineAutocompleteEnabled: Boolean = false): AddressTextFieldController {
        return AddressTextFieldController(
            label = resolvableString(value = "Name"),
            addressInputMode = if (isInlineAutocompleteEnabled) {
                AddressInputMode.AutocompleteInline(
                    googleApiKey = "test-key",
                    autocompleteCountries = emptySet(),
                    phoneNumberConfig = AddressFieldConfiguration.HIDDEN,
                    nameConfig = AddressFieldConfiguration.HIDDEN,
                    emailConfig = AddressFieldConfiguration.HIDDEN,
                )
            } else {
                AddressInputMode.NoAutocomplete()
            },
        )
    }
}
