package com.stripe.android.screenshot

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.BuildConfig
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSaveSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPaymentSheetScreenshots : BasePlaygroundTest(disableAnimations = false) {

    private val testParams = TestParameters.create(
        paymentMethodCode = "card",
        saveForFutureUseCheckboxVisible = true,
        authorizationAction = null,
    ) { settings ->
        settings[AutomaticPaymentMethodsSettingsDefinition] = false
        settings[SupportedPaymentMethodsSettingsDefinition] = "card"
        settings[LinkSettingsDefinition] = true
    }

    @Before
    fun skipTestsIfNeeded() {
        assumeFalse(BuildConfig.IS_BROWSERSTACK_BUILD)
    }

    @Test
    fun testPaymentSheetAlongsideSfuLinkSignUp() {
        testDriver.screenshotRegression(
            testParameters = testParams.copy(
                resetCustomer = true,
            ).copyPlaygroundSettings { settings ->
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.OnWithRandomEmail
                settings[CountrySettingsDefinition] = Country.US
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
                settings[CollectAddressSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                settings[CustomerSessionSettingsDefinition] = true
                settings[CustomerSessionSaveSettingsDefinition] = true
            },
            customOperations = {
                testDriver.pressSelection()
                testDriver.scrollToBottom()
            }
        )
    }

    @Test
    fun testPaymentSheetInsteadOfSfuLinkSignUp() {
        testDriver.screenshotRegression(
            testParameters = testParams.copy(
                resetCustomer = true,
            ).copyPlaygroundSettings { settings ->
                settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.OnWithRandomEmail
                settings[CountrySettingsDefinition] = Country.US
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
                settings[CollectAddressSettingsDefinition] =
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                settings[CustomerSessionSettingsDefinition] = true
                settings[CustomerSessionSaveSettingsDefinition] = false
            },
            customOperations = {
                testDriver.pressSelection()
                testDriver.scrollToBottom()
            }
        )
    }
}
