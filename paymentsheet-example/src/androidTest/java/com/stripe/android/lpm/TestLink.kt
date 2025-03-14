package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Layout
import com.stripe.android.paymentsheet.example.playground.settings.LayoutSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkType
import com.stripe.android.paymentsheet.example.playground.settings.LinkTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestLink : BasePlaygroundTest() {
    private val linkNewUser = TestParameters.create(
        paymentMethodCode = "card",
    ) { settings ->
        settings[LinkSettingsDefinition] = true
    }

    @Test
    fun testLinkInlineCustom() {
        val testParameters = TestParameters.create(
            paymentMethodCode = "card",
        ) { settings ->
            settings[CustomerSettingsDefinition] = CustomerType.GUEST
            settings[CountrySettingsDefinition] = Country.US
            settings[LinkTypeSettingsDefinition] = LinkType.Native
            settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.Off
            settings[LayoutSettingsDefinition] = Layout.HORIZONTAL
        }
        testDriver.testLinkCustom(
            testParameters = testParameters,
            populateCustomLpmFields = { populateCardDetails() },
        )
    }

    @Test
    fun testLinkPaymentWithBankAccountInPaymentMethodMode() {
        val testParameters = makeLinkTestParameters(passthroughMode = false)
        testDriver.confirmWithBankAccountInLink(testParameters)
    }

    @Test
    fun testLinkPaymentWithBankAccountInPassthroughMode() {
        val testParameters = makeLinkTestParameters(passthroughMode = true)
        testDriver.confirmWithBankAccountInLink(testParameters)
    }

    private fun makeLinkTestParameters(passthroughMode: Boolean): TestParameters {
        return TestParameters.create(
            paymentMethodCode = "card",
        ) { settings ->
            settings[SupportedPaymentMethodsSettingsDefinition] = if (passthroughMode) "card" else "card,link"
            settings[CountrySettingsDefinition] = Country.US
            settings[LinkTypeSettingsDefinition] = LinkType.Native
            settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
        }
    }
}
