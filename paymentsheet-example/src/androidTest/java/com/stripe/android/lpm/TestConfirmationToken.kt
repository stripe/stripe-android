package com.stripe.android.lpm

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.ConfirmationTokenSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSaveSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedFormSheetActionSettingDefinition
import com.stripe.android.paymentsheet.example.playground.settings.InitializationType
import com.stripe.android.paymentsheet.example.playground.settings.InitializationTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TEST_TAG
import com.stripe.android.test.core.FieldPopulator
import com.stripe.android.test.core.TestParameters
import com.stripe.android.utils.ForceNativeBankFlowTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for Confirmation Token flow with Deferred Server-Side Confirmation.
 */
@RunWith(AndroidJUnit4::class)
internal class TestConfirmationToken : BasePlaygroundTest() {

    @get:Rule
    val forceNativeBankFlowTestRule = ForceNativeBankFlowTestRule(
        context = ApplicationProvider.getApplicationContext()
    )

    private fun createConfirmationTokenParams(
        paymentMethodCode: String,
        additionalSettings: (PlaygroundSettings) -> Unit = {}
    ) = TestParameters.create(
        paymentMethodCode = paymentMethodCode,
        executeInNightlyRun = true,
    ) { settings ->
        settings[CustomerSessionSettingsDefinition] = true
        settings[CustomerSessionSaveSettingsDefinition] = true
        settings[InitializationTypeSettingsDefinition] = InitializationType.DeferredServerSideConfirmation
        settings[ConfirmationTokenSettingsDefinition] = true
        additionalSettings(settings)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSavedCardWithConfirmationToken() {
        val cardNumber = "6011111111111117"

        val testParameters = createConfirmationTokenParams("card") { settings ->
            settings[CustomerSettingsDefinition] = CustomerType.NEW
            settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT
        }.copy(
            authorizationAction = null,
            saveForFutureUseCheckboxVisible = true,
            saveCheckboxValue = true,
        )

        val state = testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
            values = FieldPopulator.Values(
                cardNumber = cardNumber,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        testDriver.confirmCompleteWithDefaultSavedPaymentMethod(
            customerId = state?.asPaymentState()?.customerConfig?.id,
            testParameters = testParameters,
            beforeBuyAction = { selectors ->
                selectors.composeTestRule.waitUntilExactlyOneExists(
                    matcher = hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG)
                        .and(isSelected())
                        .and(hasText(cardNumber.takeLast(4), substring = true)),
                    timeoutMillis = 5000L
                )
            },
        )
    }

    @Test
    fun testBankAccountWithConfirmationTokenInCustomFlow() {
        val testParameters = createConfirmationTokenParams("us_bank_account") { settings ->
            settings[CountrySettingsDefinition] = Country.US
            settings[CurrencySettingsDefinition] = Currency.USD
            settings[DelayedPaymentMethodsSettingsDefinition] = true
        }

        testDriver.confirmCustomUSBankAccountAndBuy(
            testParameters = testParameters,
        )
    }

    @Test
    fun testCashAppWithConfirmationTokenInEmbedded() {
        val testParameters = createConfirmationTokenParams("cashapp") { settings ->
            settings[CountrySettingsDefinition] = Country.US
            settings[CurrencySettingsDefinition] = Currency.USD
            settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
                PaymentMethod.Type.Card,
                PaymentMethod.Type.CashAppPay
            ).joinToString(",")
            settings[EmbeddedFormSheetActionSettingDefinition] =
                EmbeddedFormSheetActionSettingDefinition.FormSheetAction.Confirm
        }

        testDriver.confirmEmbedded(
            testParameters = testParameters,
            values = null, // CashApp doesn't show form
        )
    }
}
