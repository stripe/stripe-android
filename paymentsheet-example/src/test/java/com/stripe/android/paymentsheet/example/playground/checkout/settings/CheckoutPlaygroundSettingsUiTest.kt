@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckoutPlaygroundSettingsUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `opening nested configuration shows only its direct children`() = runScenario {
        page.group(CheckoutPlaygroundDefinitions.Controller.configuration).assertIsDisplayed().performClick()

        page.group(CheckoutPlaygroundDefinitions.Controller.currencySelector.configuration).assertIsDisplayed()
        page.value(CheckoutPlaygroundDefinitions.Controller.currencySelector.shouldSetConfiguration)
            .assertDoesNotExist()
        page.group(CheckoutPlaygroundDefinitions.session.configuration).assertDoesNotExist()
        page.group(CheckoutPlaygroundDefinitions.Controller.payment.appearance.configuration).assertDoesNotExist()

        page.group(CheckoutPlaygroundDefinitions.Controller.currencySelector.configuration).performClick()

        page.value(CheckoutPlaygroundDefinitions.Controller.currencySelector.shouldSetConfiguration)
            .assertIsDisplayed()
        page.group(CheckoutPlaygroundDefinitions.Controller.payment.configuration).assertDoesNotExist()

        openConfiguration(CheckoutPlaygroundDefinitions.Controller.configuration)
        page.group(CheckoutPlaygroundDefinitions.Controller.payment.configuration).performClick()

        page.value(CheckoutPlaygroundDefinitions.Controller.payment.shouldSetConfiguration)
            .assertIsDisplayed()
        page.group(CheckoutPlaygroundDefinitions.Controller.payment.appearance.configuration)
            .performScrollTo()
            .assertIsDisplayed()
        page.value(CheckoutPlaygroundDefinitions.Controller.currencySelector.shouldSetConfiguration)
            .assertDoesNotExist()

        openConfiguration(CheckoutPlaygroundDefinitions.Controller.express.configuration)

        page.value(CheckoutPlaygroundDefinitions.Controller.express.shouldSetConfiguration)
            .performScrollTo()
            .assertIsDisplayed()
        page.value(CheckoutPlaygroundDefinitions.Controller.payment.shouldSetConfiguration)
            .assertDoesNotExist()
    }

    @Test
    fun `search finds settings globally and restores the current configuration when cleared`() = runScenario(
        initialConfiguration = CheckoutPlaygroundDefinitions.Controller.express.configuration,
    ) {
        val paymentCornerRadius = CheckoutPlaygroundDefinitions.Controller.payment.appearance.primaryButton.shape
            .cornerRadius
        val currencyCornerRadius = CheckoutPlaygroundDefinitions.Controller.currencySelector.appearance.cornerRadius

        page.value(CheckoutPlaygroundDefinitions.Controller.express.shouldSetConfiguration).assertIsDisplayed()

        setSearchQuery("CoRn Ra")

        page.value(paymentCornerRadius).performScrollTo().assertIsDisplayed()
        page.breadcrumb(paymentCornerRadius).assertTextContains(
            "CheckoutController.Configuration › Payment Element › Appearance › Primary button › Shape"
        )
        page.value(currencyCornerRadius).performScrollTo().assertIsDisplayed()
        page.breadcrumb(currencyCornerRadius).assertTextContains(
            "CheckoutController.Configuration › Currency Selector Element › Appearance"
        )
        page.value(CheckoutPlaygroundDefinitions.Controller.express.shouldSetConfiguration).assertDoesNotExist()

        setSearchQuery("")

        page.value(CheckoutPlaygroundDefinitions.Controller.express.shouldSetConfiguration).assertIsDisplayed()
        page.value(paymentCornerRadius).assertDoesNotExist()
    }

    @Test
    fun `search displays an empty state when there are no matches`() = runScenario {
        setSearchQuery("not a setting")

        composeRule.onNodeWithText("No matching settings found").assertIsDisplayed()
    }

    @Test
    fun `search displays non-applicable choice settings as disabled`() = runScenario {
        val definition = CheckoutPlaygroundDefinitions.session.paymentMethodSave
        setSearchQuery("save payment")

        page.value(definition).performScrollTo().assertIsDisplayed().assertIsNotEnabled()
        assertThat(settings[definition]).isTrue()

        settings.update(CheckoutPlaygroundDefinitions.session.customer, "new")
        composeRule.waitForIdle()

        page.value(definition).assertIsEnabled()
        composeRule.onNodeWithText("Off").performClick()
        assertThat(settings[definition]).isFalse()
    }

    @Test
    fun `search displays non-applicable text settings as disabled`() = runScenario {
        val definition = CheckoutPlaygroundDefinitions.session.paymentMethodTypes
        setSearchQuery("payment method types")

        page.value(definition).performScrollTo().assertIsDisplayed().assertIsNotEnabled()

        settings.update(CheckoutPlaygroundDefinitions.session.automaticPaymentMethods, false)
        composeRule.waitForIdle()

        page.value(definition).assertIsEnabled().performTextReplacement("card, cashapp")
        assertThat(settings[definition]).containsExactly("card", "cashapp").inOrder()
    }

    @Test
    fun `color setting uses color picker`() = runScenario(
        initialConfiguration = CheckoutPlaygroundDefinitions.Controller.payment.appearance.lightColors.configuration,
    ) {
        val definition = CheckoutPlaygroundDefinitions.Controller.payment.appearance.lightColors.primary
        settings.updateSerialized(definition, "")
        composeRule.waitForIdle()
        page.value(definition).performClick()

        composeRule.onNodeWithText("Pick color").assertIsDisplayed().performClick()

        assertThat(settings.serializedValue(definition)).isEqualTo("#FF000000")
    }

    @Test
    fun `customer setting toggles between guest and new`() = runScenario(
        initialConfiguration = CheckoutPlaygroundDefinitions.session.configuration,
    ) {
        val definition = CheckoutPlaygroundDefinitions.session.customer
        composeRule.onNodeWithText("Guest").assertIsDisplayed()
        composeRule.onNodeWithText("New").assertIsDisplayed().performClick()

        assertThat(settings[definition]).isEqualTo("new")
    }

    @Test
    fun `returning customer displays stored customer ID`() = runScenario(
        initialConfiguration = CheckoutPlaygroundDefinitions.session.configuration,
        configureSettings = { saveReturningCustomer("cus_123") },
    ) {
        page.value(CheckoutPlaygroundDefinitions.session.customerId)
            .assertIsDisplayed()
            .assertTextContains("Customer ID")
            .assertTextContains("cus_123")
    }

    @Test
    fun `returning customer without stored ID displays empty customer ID setting`() = runScenario(
        initialConfiguration = CheckoutPlaygroundDefinitions.session.configuration,
        configureSettings = {
            update(CheckoutPlaygroundDefinitions.session.customer, "returning")
        },
    ) {
        page.value(CheckoutPlaygroundDefinitions.session.customerId)
            .assertIsDisplayed()
            .assertTextContains("Customer ID")
    }

    @Test
    fun `returning customer can enter arbitrary customer ID`() = runScenario(
        initialConfiguration = CheckoutPlaygroundDefinitions.session.configuration,
        configureSettings = {
            update(CheckoutPlaygroundDefinitions.session.customer, "returning")
        },
    ) {
        page.value(CheckoutPlaygroundDefinitions.session.customerId)
            .performTextReplacement("cus_custom")

        assertThat(settings[CheckoutPlaygroundDefinitions.session.customerId]).isEqualTo("cus_custom")
    }

    @Test
    fun `customer ID is hidden for guest and new customers`() = runScenario(
        initialConfiguration = CheckoutPlaygroundDefinitions.session.configuration,
    ) {
        page.value(CheckoutPlaygroundDefinitions.session.customerId).assertDoesNotExist()

        settings.update(CheckoutPlaygroundDefinitions.session.customer, "new")
        composeRule.waitForIdle()

        page.value(CheckoutPlaygroundDefinitions.session.customerId).assertDoesNotExist()
    }

    @Test
    fun `payment method saving is displayed only for customers`() = runScenario(
        initialConfiguration = CheckoutPlaygroundDefinitions.session.configuration,
    ) {
        val definition = CheckoutPlaygroundDefinitions.session.paymentMethodSave
        page.value(definition).assertDoesNotExist()

        settings.update(CheckoutPlaygroundDefinitions.session.customer, "new")
        composeRule.waitForIdle()
        page.value(definition).assertIsDisplayed()

        settings.update(CheckoutPlaygroundDefinitions.session.customer, "returning")
        composeRule.waitForIdle()
        page.value(definition).assertIsDisplayed()
    }

    @Test
    fun `payment method types are only displayed for manual payment methods`() = runScenario(
        initialConfiguration = CheckoutPlaygroundDefinitions.session.configuration,
    ) {
        page.value(CheckoutPlaygroundDefinitions.session.paymentMethodTypes).assertDoesNotExist()

        settings.update(CheckoutPlaygroundDefinitions.session.automaticPaymentMethods, false)
        composeRule.waitForIdle()

        page.value(CheckoutPlaygroundDefinitions.session.paymentMethodTypes).assertIsDisplayed()
    }

    private fun runScenario(
        initialConfiguration: CheckoutPlaygroundSettingDefinition.Configuration = CheckoutPlaygroundDefinitions.root,
        configureSettings: CheckoutPlaygroundSettings.() -> Unit = {},
        block: Scenario.() -> Unit,
    ) {
        val settings = CheckoutPlaygroundSettings.createInMemory().apply(configureSettings)
        var current by mutableStateOf(initialConfiguration)
        var searchQuery by mutableStateOf("")
        composeRule.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                CheckoutPlaygroundSettingsUi(
                    configuration = current,
                    searchQuery = searchQuery,
                    settings = settings,
                    onOpenConfiguration = { current = it },
                )
            }
        }
        block(
            Scenario(
                page = Page(composeRule),
                settings = settings,
                openConfiguration = { current = it },
                setSearchQuery = { query ->
                    composeRule.runOnIdle {
                        searchQuery = query
                    }
                },
            )
        )
    }

    private data class Scenario(
        val page: Page,
        val settings: CheckoutPlaygroundSettings,
        val openConfiguration: (CheckoutPlaygroundSettingDefinition.Configuration) -> Unit,
        val setSearchQuery: (String) -> Unit,
    )

    private class Page(private val rule: ComposeContentTestRule) {
        fun group(definition: CheckoutPlaygroundSettingDefinition.Configuration) =
            rule.onNodeWithTag(checkoutSettingGroupTestTag(definition))

        fun value(definition: CheckoutPlaygroundSettingDefinition.Value<*>) =
            rule.onNodeWithTag(checkoutSettingValueTestTag(definition))

        fun breadcrumb(definition: CheckoutPlaygroundSettingDefinition.Value<*>) =
            rule.onNodeWithTag(checkoutSettingBreadcrumbTestTag(definition))
    }
}
