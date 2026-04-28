package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CurrencySelectorToggleUITest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val usdOption = CurrencyOption(code = "USD", displayableText = "\uD83C\uDDFA\uD83C\uDDF8 $50.99")
    private val eurOption = CurrencyOption(code = "EUR", displayableText = "\uD83C\uDDEA\uD83C\uDDFA €45.87")

    @Test
    fun clickingSelectedOption_doesNotCallCallback() = runScenario {
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD").performClick()

        onCurrencySelectedCalls.expectNoEvents()
    }

    @Test
    fun clickingUnselectedOption_callsCallback() = runScenario {
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}EUR").performClick()

        assertThat(onCurrencySelectedCalls.awaitItem()).isEqualTo(eurOption)
    }

    @Test
    fun clickingOption_whenDisabled_doesNotCallCallback() = runScenario(isEnabled = false) {
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}EUR").performClick()

        onCurrencySelectedCalls.expectNoEvents()
    }

    @Test
    fun errorMessage_isDisplayed() = runScenario(errorMessage = "Something went wrong.") {
        composeRule.onNodeWithTag(TEST_TAG_CURRENCY_SELECTOR_ERROR)
            .assertIsDisplayed()
            .assertTextEquals("Something went wrong.")
    }

    @Test
    fun errorMessage_isNotDisplayed_whenNull() = runScenario(errorMessage = null) {
        composeRule.onNodeWithTag(TEST_TAG_CURRENCY_SELECTOR_ERROR).assertDoesNotExist()
    }

    private fun runScenario(
        selectedCode: String = "USD",
        isEnabled: Boolean = true,
        errorMessage: String? = null,
        block: suspend Scenario.() -> Unit,
    ) {
        val onCurrencySelectedCalls = Turbine<CurrencyOption>()

        composeRule.setContent {
            CurrencySelectorToggle(
                options = CurrencySelectorOptions(
                    first = usdOption,
                    second = eurOption,
                    selectedCode = selectedCode,
                ),
                onCurrencySelected = { onCurrencySelectedCalls.add(it) },
                isEnabled = isEnabled,
                errorMessage = errorMessage,
            )
        }

        val scenario = Scenario(onCurrencySelectedCalls)
        runTest {
            scenario.block()
        }

        onCurrencySelectedCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val onCurrencySelectedCalls: Turbine<CurrencyOption>,
    )
}
