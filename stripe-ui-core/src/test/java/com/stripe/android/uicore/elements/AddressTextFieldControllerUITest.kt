package com.stripe.android.uicore.elements

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.cash.turbine.Turbine
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class AddressTextFieldControllerUITest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `inline mode shows enter manually with predictions`() = runScenario(reportsFormValue = false) {
        composeRule.onNodeWithText(ENTER_MANUALLY_TEXT).performClick()

        handler.enterManuallyCalls.awaitItem()
    }

    @Test
    fun `expanded mode shows enter manually with predictions`() = runScenario(reportsFormValue = true) {
        composeRule.onNodeWithText(ENTER_MANUALLY_TEXT).performClick()

        handler.enterManuallyCalls.awaitItem()
    }

    private fun runScenario(
        reportsFormValue: Boolean,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val handler = FakeInlineAutocompleteHandler(
            predictionsState = AutocompleteAddressInteractor.InlinePredictionsState.Results(
                query = "123 Main",
                predictions = listOf(
                    AutocompleteAddressInteractor.InlineAddressPrediction(
                        id = "1",
                        primaryText = "123 Main Street",
                        secondaryText = "San Francisco, CA, USA",
                    ),
                ),
            ),
        )
        val element = AddressTextFieldElement(
            identifier = FormFieldId.Line1,
            label = resolvableString(value = "Address"),
            inlineAutocompleteHandler = handler,
            reportsFormValue = reportsFormValue,
            initialQuery = "123 Main",
        )

        composeRule.setContent {
            element.controller.ComposeUI(
                enabled = true,
                field = element,
                modifier = Modifier,
                hiddenIdentifiers = emptySet(),
                lastTextFieldIdentifier = null,
            )
        }

        Scenario(handler).block()

        handler.enterManuallyCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val handler: FakeInlineAutocompleteHandler,
    )

    private class FakeInlineAutocompleteHandler(
        predictionsState: AutocompleteAddressInteractor.InlinePredictionsState,
    ) : InlineAutocompleteHandler {
        val enterManuallyCalls = Turbine<Unit>()

        override val predictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState> =
            MutableStateFlow(predictionsState)

        override fun onPredictionSelected(predictionId: String) = Unit
        override fun onDismissed() = Unit
        override fun onFocusLost() = Unit
        override fun onFocusGained() = Unit
        override fun onEnterManually() {
            enterManuallyCalls.add(Unit)
        }
        override fun getAttributionDrawable(isDarkTheme: Boolean): Int? = null
        override fun onSearchActivated() = Unit
    }

    private companion object {
        const val ENTER_MANUALLY_TEXT = "Enter address manually"
    }
}
