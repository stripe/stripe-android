package com.stripe.android.lpm

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.GooglePaySettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestSepaDebit : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = "sepa_debit",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.FR
        settings[DelayedPaymentMethodsSettingsDefinition] = true
        settings[GooglePaySettingsDefinition] = false
    }.copy(
        authorizationAction = null,
    )

    @Test
    fun testSepaDebit() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        ) {
            rules.compose.onNodeWithText("IBAN").apply {
                performTextInput(
                    "DE89370400440532013000"
                )
            }
        }
    }

    @Test
    fun testSepaDebitSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[AutomaticPaymentMethodsSettingsDefinition] = true
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
            }
        ) {
            rules.compose.onNodeWithText("IBAN").apply {
                performTextInput(
                    "DE89370400440532013000"
                )
            }
        }
    }

    @Test
    fun testSepaDebitSetup() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[AutomaticPaymentMethodsSettingsDefinition] = true
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            }
        ) {
            rules.compose.onNodeWithText("IBAN").apply {
                performTextInput(
                    "DE89370400440532013000"
                )
            }
        }
    }

    @Test
    fun testSepaDebitInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
            populateCustomLpmFields = {
                rules.compose.onNodeWithText("IBAN").apply {
                    performTextInput(
                        "DE89370400440532013000"
                    )
                }
            },
            verifyCustomLpmFields = {
                rules.compose.onNodeWithText("IBAN").apply {
                    assertContentDescriptionEquals(
                        "DE89370400440532013000"
                    )
                }
            }
        )
    }
}
