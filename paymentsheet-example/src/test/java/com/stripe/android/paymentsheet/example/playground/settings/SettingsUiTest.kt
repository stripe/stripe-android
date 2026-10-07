@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.stripe.android.paymentsheet.example.playground.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
internal class SettingsUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `custom billing email can be entered`() = runScenario(
        searchQuery = "Default Billing Address",
    ) {
        val customEmailInput = composeRule.onNodeWithTag(textSettingTestTag("Custom email"))
        customEmailInput.assertDoesNotExist()

        composeRule.onNodeWithTag(settingTestTag("Default Billing Address")).performClick()
        composeRule.onNodeWithText("Custom email").performClick()

        customEmailInput
            .assertIsDisplayed()
            .performTextReplacement("custom@example.com")

        assertThat(playgroundSettings[DefaultBillingAddressSettingsDefinition].value).isEqualTo(
            DefaultBillingAddress.WithEmail(
                email = "custom@example.com",
                phone = DEFAULT_BILLING_ADDRESS_PHONE,
            )
        )
    }

    @Test
    fun `changing applicable settings does not reuse another setting's value`() = runScenario(
        searchQuery = "",
    ) {
        playgroundSettings[CustomerSettingsDefinition] = CustomerType.NEW
        composeRule.waitForIdle()

        playgroundSettings[CustomerSettingsDefinition] = CustomerType.GUEST
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Checkout Mode").assertExists()
    }

    @Test
    fun `custom billing email remains when integration type changes`() {
        val playgroundSettings = PlaygroundSettings.createFromDefaults().apply {
            this[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.WithEmail(
                email = "custom@example.com",
                phone = DEFAULT_BILLING_ADDRESS_PHONE,
            )
        }

        playgroundSettings.updateConfigurationData { configurationData ->
            configurationData.copy(
                integrationType = PlaygroundConfigurationData.IntegrationType.Embedded,
            )
        }
        assertThat(playgroundSettings[DefaultBillingAddressSettingsDefinition].value).isEqualTo(
            DefaultBillingAddress.WithEmail(
                email = "custom@example.com",
                phone = DEFAULT_BILLING_ADDRESS_PHONE,
            )
        )
    }

    @Test
    fun `custom billing email is restored`() {
        val playgroundSettings = PlaygroundSettings.createFromDefaults().apply {
            this[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.WithEmail(
                email = "custom@example.com",
                phone = DEFAULT_BILLING_ADDRESS_PHONE,
            )
        }

        val restoredSettings = PlaygroundSettings.createFromJsonString(
            playgroundSettings.snapshot().asJsonString()
        )
        assertThat(restoredSettings[DefaultBillingAddressSettingsDefinition].value).isEqualTo(
            DefaultBillingAddress.WithEmail(
                email = "custom@example.com",
                phone = DEFAULT_BILLING_ADDRESS_PHONE,
            )
        )
    }

    private fun runScenario(
        searchQuery: String,
        block: Scenario.() -> Unit,
    ) {
        val playgroundSettings = PlaygroundSettings.createFromDefaults()
        composeRule.setContent {
            SettingsUi(
                playgroundSettings = playgroundSettings,
                searchQuery = searchQuery,
            )
        }

        block(Scenario(playgroundSettings))
    }

    private data class Scenario(
        val playgroundSettings: PlaygroundSettings,
    )
}
