package com.stripe.android.lpm

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.EnableInstantDebitsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import com.stripe.android.test.core.ui.ComposeButton
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestInstantDebits : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = "link",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.US
        settings[CurrencySettingsDefinition] = Currency.USD
        settings[AutomaticPaymentMethodsSettingsDefinition] = false
        settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
        settings[LinkSettingsDefinition] = true
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf("card", "link")
        settings[EnableInstantDebitsSettingsDefinition] = true
    }

    @get:Rule
    val instantDebitsFeatureRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.instantDebits,
        isEnabled = true,
    )

    @Test
    @Ignore
    fun testInstantDebitsSuccess() {
        testDriver.confirmInstantDebits(
            testParameters = testParameters.copy(authorizationAction = null),
            afterAuthorization = {
                rules.compose
                    .onNodeWithText("STRIPE TEST BANK", substring = true)
                    .assertIsDisplayed()
            }
        )
    }

    @Test
    fun testInstantDebitsCancelAllowsUserToContinue() {
        testDriver.confirmInstantDebits(
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel,
            ),
            afterAuthorization = {
                ComposeButton(rules.compose, hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
                    .waitFor(isEnabled())
            }
        )
    }

    @Test
    fun testInstantDebitsCancelAllowsUserToContinueInCustomFlow() {
        testDriver.confirmInstantDebitsInCustomFlow(
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel,
            ),
            afterAuthorization = {
                ComposeButton(rules.compose, hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
                    .waitFor(isEnabled())
            }
        )
    }
}
