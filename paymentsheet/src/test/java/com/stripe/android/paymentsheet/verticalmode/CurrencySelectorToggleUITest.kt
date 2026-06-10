package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.Checkout
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CurrencySelectorToggleUITest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val usdOption = CurrencyOption(
        code = "USD",
        displayableText = "🇺🇸 $50.99",
        formattedAmount = "$50.99",
    )
    private val eurOption = CurrencyOption(
        code = "EUR",
        displayableText = "🇪🇺 €45.87",
        formattedAmount = "€45.87",
    )

    @Test
    fun clickingSelectedOption_doesNotCallCallback() = runScenario {
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD").performClick()

        onCurrencySelectedCalls.expectNoEvents()
    }

    @Test
    fun selectedOption_isEnabledAndClickableForAccessibility() = runScenario {
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD")
            .assertIsEnabled()
            .assertHasClickAction()
            .assertIsSelected()
            .assertRole(Role.RadioButton)
    }

    @Test
    fun options_haveAccessibleLabelsAndSelectedStateDescriptions() = runScenario {
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD")
            .assertContentDescription("$50.99")
            .assertStateDescription("Selected")

        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}EUR")
            .assertContentDescription("€45.87")
            .assertStateDescription("Not selected")
    }

    @Test
    fun exchangeRate_isNotDuplicatedInOptionAccessibleLabel() = runScenario(
        exchangeRateText = "1 USD = 0.900961 EUR",
    ) {
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD")
            .assertContentDescription("$50.99")

        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}EUR")
            .assertContentDescription("€45.87")
    }

    @Test
    fun disabledOptions_areExposedAsDisabledForAccessibility() = runScenario(isEnabled = false) {
        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}USD")
            .assertIsNotEnabled()

        composeRule.onNodeWithTag("${TEST_TAG_CURRENCY_OPTION_PREFIX}EUR")
            .assertIsNotEnabled()
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
            .assertLiveRegionMode(LiveRegionMode.Assertive)
    }

    @Test
    fun errorMessage_isExposedOnSelector() = runScenario(errorMessage = "Something went wrong.") {
        composeRule.onNodeWithTag(TEST_TAG_CURRENCY_SELECTOR)
            .assertHasErrorMessage("Something went wrong.")
    }

    @Test
    fun exchangeRate_isDisplayed() = runScenario(
        exchangeRateText = "1 USD = 0.900961 EUR",
    ) {
        composeRule.onNodeWithText("1 USD = 0.900961 EUR")
            .assertIsDisplayed()
    }

    @Test
    fun errorMessage_isNotDisplayed_whenNull() = runScenario(errorMessage = null) {
        composeRule.onNodeWithTag(TEST_TAG_CURRENCY_SELECTOR_ERROR).assertDoesNotExist()
    }

    private fun runScenario(
        selectedCode: String = "USD",
        isEnabled: Boolean = true,
        errorMessage: String? = null,
        exchangeRateText: String? = null,
        block: suspend Scenario.() -> Unit,
    ) {
        val onCurrencySelectedCalls = Turbine<CurrencyOption>()

        composeRule.setContent {
            CurrencySelectorToggle(
                options = CurrencySelectorOptions(
                    first = usdOption,
                    second = eurOption,
                    selectedCode = selectedCode,
                    exchangeRateText = exchangeRateText,
                ),
                onCurrencySelected = { onCurrencySelectedCalls.add(it) },
                isEnabled = isEnabled,
                errorMessage = errorMessage,
                appearance = Checkout.CurrencySelectorContentAppearance().build(),
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

private fun SemanticsNodeInteraction.assertContentDescription(vararg values: String): SemanticsNodeInteraction {
    assertThat(fetchSemanticsNode().config[SemanticsProperties.ContentDescription]).containsExactly(*values)
    return this
}

private fun SemanticsNodeInteraction.assertStateDescription(value: String): SemanticsNodeInteraction {
    assertThat(fetchSemanticsNode().config[SemanticsProperties.StateDescription]).isEqualTo(value)
    return this
}

private fun SemanticsNodeInteraction.assertRole(value: Role): SemanticsNodeInteraction {
    assertThat(fetchSemanticsNode().config[SemanticsProperties.Role]).isEqualTo(value)
    return this
}

private fun SemanticsNodeInteraction.assertLiveRegionMode(value: LiveRegionMode): SemanticsNodeInteraction {
    assertThat(fetchSemanticsNode().config[SemanticsProperties.LiveRegion]).isEqualTo(value)
    return this
}

private fun SemanticsNodeInteraction.assertHasErrorMessage(value: String): SemanticsNodeInteraction {
    assertThat(fetchSemanticsNode().config[SemanticsProperties.Error]).isEqualTo(value)
    return this
}
