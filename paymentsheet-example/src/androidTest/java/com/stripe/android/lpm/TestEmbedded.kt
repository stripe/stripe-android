package com.stripe.android.lpm

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedFormSheetActionSettingDefinition
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedFormSheetActionSettingDefinition.FormSheetAction
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.FieldPopulator
import com.stripe.android.test.core.TestParameters
import com.stripe.android.utils.ForceNativeBankFlowTestRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

internal class TestEmbedded : BasePlaygroundTest() {
    @get:Rule
    val forceNativeBankFlowTestRule = ForceNativeBankFlowTestRule(
        context = ApplicationProvider.getApplicationContext()
    )

    private val parameters = TestParameters.create(
        paymentMethodCode = "card",
        executeInNightlyRun = true,
    ) { settings ->
        settings[EmbeddedFormSheetActionSettingDefinition] = FormSheetAction.Confirm
    }

    @Test
    fun testCard() {
        testDriver.confirmEmbedded(
            testParameters = parameters.copy(
                saveForFutureUseCheckboxVisible = true,
                authorizationAction = null,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun testCardWithCardBrandChoice() {
        testDriver.confirmEmbedded(
            testParameters = parameters.copy(
                saveForFutureUseCheckboxVisible = true,
                authorizationAction = null,
            ).copyPlaygroundSettings { settings ->
                settings[CountrySettingsDefinition] = Country.FR
            },
            values = FieldPopulator.Values(
                cardNumber = "4000002500001001"
            ),
            populateCustomLpmFields = {
                populateCardDetails()
                selectCardBrand("Cartes Bancaires")
            },
        )
    }

    @Test
    fun testCashAppPay() {
        testDriver.confirmEmbedded(
            testParameters = parameters.copy(
                paymentMethodCode = "cashapp",
            ).copyPlaygroundSettings { settings ->
                settings[CountrySettingsDefinition] = Country.US
                settings[CurrencySettingsDefinition] = Currency.USD
                settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
                    PaymentMethod.Type.Card,
                    PaymentMethod.Type.CashAppPay
                ).joinToString(",")
            },
            values = null, // We don't show the form for cash app.
        )
    }

    @Ignore("#ir-<tba>")
    @Test
    fun testUsBankAccount() {
        testDriver.confirmEmbeddedUsBankAccount(
            testParameters = parameters.copy(
                paymentMethodCode = "us_bank_account",
            ).copyPlaygroundSettings { settings ->
                settings[CountrySettingsDefinition] = Country.US
                settings[CurrencySettingsDefinition] = Currency.USD
                settings[DelayedPaymentMethodsSettingsDefinition] = true
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.OnWithRandomEmail
            },
        )
    }
}
