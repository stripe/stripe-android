package com.stripe.android.lpm

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSheetPaymentMethodModeDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.FeatureFlagSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodMode
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import com.stripe.android.utils.ForceNativeBankFlowTestRule
import org.junit.Rule
import org.junit.Test

internal class TestUsBankAccountInCustomerSheet : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "us_bank_account",
        executeInNightlyRun = true,
        authorizationAction = null
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.US
        settings[CustomerSheetPaymentMethodModeDefinition] = PaymentMethodMode.SetupIntent
    }

    @get:Rule
    val forceNativeBankFlowTestRule = ForceNativeBankFlowTestRule(
        context = ApplicationProvider.getApplicationContext()
    )

    @Test
    fun testUSBankAccount() {
        testDriver.saveUsBankAccountInCustomerSheet(
            financialConnectionsLiteEnabled = false,
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel,
            )
        )
    }

    @Test
    fun testUSBankAccountLite() {
        testDriver.saveUsBankAccountInCustomerSheet(
            financialConnectionsLiteEnabled = true,
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel,
            ).copyPlaygroundSettings {
                it[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.OnWithRandomEmail
                it[FeatureFlagSettingsDefinition(FeatureFlags.financialConnectionsLiteEnabled)] = true
                it[FeatureFlagSettingsDefinition(FeatureFlags.financialConnectionsFullSdkUnavailable)] = true
            }
        )
    }

    @Test
    fun testUSBankAccountWithCustomerSession() {
        testDriver.saveUsBankAccountInCustomerSheet(
            financialConnectionsLiteEnabled = false,
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel,
            ).copyPlaygroundSettings { settings ->
                settings[CustomerSessionSettingsDefinition] = true
            }
        )
    }

    @Test
    fun testUSBankAccountLiteWithCustomerSession() {
        testDriver.saveUsBankAccountInCustomerSheet(
            financialConnectionsLiteEnabled = true,
            testParameters = testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel,
            ).copyPlaygroundSettings { settings ->
                settings[CustomerSessionSettingsDefinition] = true
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.OnWithRandomEmail
                settings[FeatureFlagSettingsDefinition(FeatureFlags.financialConnectionsLiteEnabled)] = true
                settings[FeatureFlagSettingsDefinition(FeatureFlags.financialConnectionsFullSdkUnavailable)] = true
            }
        )
    }
}
