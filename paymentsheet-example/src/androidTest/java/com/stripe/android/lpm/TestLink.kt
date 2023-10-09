package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.AttachDefaultsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestLink : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "card",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.US
        settings[CurrencySettingsDefinition] = Currency.USD
        settings[LinkSettingsDefinition] = true
        settings[CustomerSettingsDefinition] = CustomerType.GUEST
        settings[DefaultBillingAddressSettingsDefinition] = false
        settings[AttachDefaultsSettingsDefinition] = false
    }.copy(
        authorizationAction = null,
    )

    @Test
    fun testLinkInlineCustom() {
        testDriver.testLinkCustom(testParameters)
    }

    @Test
    fun testLinkInlineComplete() {
        // TODO:
        testDriver.testLinkCustom(testParameters)
    }

    @Test
    fun testLinkInlineCustom_passthrough() {
        testDriver.testLinkCustom(testParameters.linkPassthroughMode())
    }

    @Test
    fun testLinkInlineComplete_passthrough() {
        // TODO:
        testDriver.testLinkCustom(testParameters.linkPassthroughMode())
    }

    @Test
    fun testLink() {
        testDriver.confirmLink(testParameters)
    }

    @Test
    fun testLinkWithSfu() {
        testDriver.confirmLink(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
            }
        )
    }

    @Test
    fun testLinkWithSetupIntent() {
        testDriver.confirmLink(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            }
        )
    }

    @Test
    fun testLink_passthroughMode() {
        testDriver.confirmLink(
            testParameters.linkPassthroughMode()
        )
    }

    @Test
    fun testLinkWithSfu_passthroughMode() {
        testDriver.confirmLink(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
            }.linkPassthroughMode()
        )
    }

    @Test
    fun testLinkWithSetupIntent_passthroughMode() {
        testDriver.confirmLink(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            }.linkPassthroughMode()
        )
    }
}

private fun TestParameters.linkPassthroughMode(): TestParameters =
    copyPlaygroundSettings { settings ->
        settings[LinkSettingsDefinition] = false
        settings[AutomaticPaymentMethodsSettingsDefinition] = false
    }
