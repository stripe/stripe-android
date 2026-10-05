package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkDisplaySetting
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkType
import com.stripe.android.paymentsheet.example.playground.settings.LinkTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestLink : BasePlaygroundTest() {

    @Test
    fun testLinkPaymentWithBankAccountInPaymentMethodMode() {
        testDriver.confirmWithBankAccountInLink(
            makeLinkTestParameters(passthroughMode = false)
        )
    }

    @Test
    fun testLinkPaymentWithBankAccountInPassthroughMode() {
        testDriver.confirmWithBankAccountInLink(
            makeLinkTestParameters(passthroughMode = true)
        )
    }

    private fun makeLinkTestParameters(passthroughMode: Boolean): TestParameters {
        return TestParameters.create(
            paymentMethodCode = "card",
            authorizationAction = null,
        ) { settings ->
            settings[SupportedPaymentMethodsSettingsDefinition] = if (passthroughMode) "card" else "card,link"
            settings[MerchantSettingsDefinition] = Merchant.US
            settings[LinkSettingsDefinition] = LinkDisplaySetting.Automatic
            settings[LinkTypeSettingsDefinition] = LinkType.Native
            settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
        }
    }
}
