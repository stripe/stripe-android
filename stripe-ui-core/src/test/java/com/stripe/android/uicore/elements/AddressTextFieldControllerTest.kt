package com.stripe.android.uicore.elements

import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AddressTextFieldControllerTest {
    @Test
    fun `on raw field change, should not update value when reportsFormValue is false`() = runTest {
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
    fun `on raw field change, should update value when reportsFormValue is true`() = runTest {
        val controller = createAddressController(
            inlineAutocompleteHandler = FakeInlineAutocompleteHandler(),
            reportsFormValue = true,
        )

        turbineScope {
            val rawFieldValueTurbine = controller.rawFieldValue.testIn(this)

            assertThat(rawFieldValueTurbine.awaitItem()).isEqualTo("")

            controller.onRawValueChange("123 Main St")

            assertThat(rawFieldValueTurbine.awaitItem()).isEqualTo("123 Main St")

            rawFieldValueTurbine.cancelAndIgnoreRemainingEvents()
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
    fun `reportsFormValue mode - no error when query is not blank`() = runTest {
        val controller = createAddressController(
            inlineAutocompleteHandler = FakeInlineAutocompleteHandler(),
            reportsFormValue = true,
            initialQuery = "123 Main St",
        )

        turbineScope {
            val errorTurbine = controller.validationMessage.testIn(this)

            assertThat(errorTurbine.awaitItem()).isNull()

            controller.onValidationStateChanged(true)

            errorTurbine.expectNoEvents()

            errorTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `non-inline mode - is not editable`() = runTest {
        val controller = createAddressController(inlineAutocompleteHandler = null)

        assertThat(controller.isEditable).isFalse()
    }

    @Test
    fun `inline mode - is editable and tracks query`() = runTest {
        val controller = createAddressController(
            inlineAutocompleteHandler = FakeInlineAutocompleteHandler(),
        )

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
    fun `non-inline mode - query ignored when handler is null`() = runTest {
        val controller = createAddressController(inlineAutocompleteHandler = null)

        turbineScope {
            val queryTurbine = controller.inlineQuery.testIn(this)

            queryTurbine.awaitItem() // initial ""

            controller.onInlineQueryChanged("should be ignored")

            queryTurbine.expectNoEvents()

            queryTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initialQuery sets the starting value`() = runTest {
        val controller = createAddressController(
            inlineAutocompleteHandler = FakeInlineAutocompleteHandler(),
            initialQuery = "pre-filled value",
        )

        turbineScope {
            val queryTurbine = controller.inlineQuery.testIn(this)

            assertThat(queryTurbine.awaitItem()).isEqualTo("pre-filled value")

            queryTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isComplete reflects query state when reportsFormValue is true`() = runTest {
        val controller = createAddressController(
            inlineAutocompleteHandler = FakeInlineAutocompleteHandler(),
            reportsFormValue = true,
        )

        turbineScope {
            val completeTurbine = controller.isComplete.testIn(this)

            assertThat(completeTurbine.awaitItem()).isFalse()

            controller.onInlineQueryChanged("123 Main")

            assertThat(completeTurbine.awaitItem()).isTrue()

            controller.onInlineQueryChanged("")

            assertThat(completeTurbine.awaitItem()).isFalse()

            completeTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `predictions dropdown is hidden when state is idle`() {
        assertThat(
            shouldShowPredictionsDropdown(
                AutocompleteAddressInteractor.InlinePredictionsState.Idle
            )
        ).isFalse()
    }

    @Test
    fun `predictions dropdown remains shown when results are empty`() {
        assertThat(
            shouldShowPredictionsDropdown(
                AutocompleteAddressInteractor.InlinePredictionsState.Results(
                    query = "123 Main",
                    predictions = emptyList()
                )
            )
        ).isTrue()
    }

    private fun createAddressController(
        inlineAutocompleteHandler: InlineAutocompleteHandler? = null,
        reportsFormValue: Boolean = false,
        initialQuery: String = "",
    ): AddressTextFieldController {
        return AddressTextFieldController(
            label = resolvableString(value = "Name"),
            addressInputMode = AddressInputMode.NoAutocomplete(),
            inlineAutocompleteHandler = inlineAutocompleteHandler,
            reportsFormValue = reportsFormValue,
            initialQuery = initialQuery,
            showEnterManually = true,
        )
    }

    private class FakeInlineAutocompleteHandler : InlineAutocompleteHandler {
        override val predictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState> =
            MutableStateFlow(AutocompleteAddressInteractor.InlinePredictionsState.Idle)

        override fun onPredictionSelected(predictionId: String) = Unit
        override fun onDismissed() = Unit
        override fun onFocusLost() = Unit
        override fun onFocusGained() = Unit
        override fun onEnterManually() = Unit
        override fun getAttributionDrawable(isDarkTheme: Boolean): Int? = null
        override fun onSearchActivated() = Unit
    }
}
