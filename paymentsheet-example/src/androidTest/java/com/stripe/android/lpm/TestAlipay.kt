package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestAlipay : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = "alipay",
    ) { settings ->
        settings[MerchantSettingsDefinition] = Merchant.US
        settings[CurrencySettingsDefinition] = Currency.USD
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card.code,
            PaymentMethod.Type.Klarna.code,
            PaymentMethod.Type.Affirm.code,
            PaymentMethod.Type.Alipay.code,
        ).joinToString(",")
    }

    @Test
    fun testAlipay() {
        testDriver.confirmNewOrGuestComplete(testParameters)
    }

    // Tests for mitigating #ir-cursor-reinforce that uses the new EVO path. Merchant.CN
    // (acct_1ONGjdKULGu5EgSk) is enrolled in alipay_cn_to_alipay_plus_migration_gate, so its
    // Alipay redirects go through the pm-redirects.stripe.com trampoline unlike Merchant.US.
    @Test
    fun testAlipayEVO() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[MerchantSettingsDefinition] = Merchant.CN
                settings[CurrencySettingsDefinition] = Currency.CNY
                settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
                    PaymentMethod.Type.Card.code,
                    PaymentMethod.Type.Alipay.code,
                ).joinToString(",")
            },
        )
    }
}
