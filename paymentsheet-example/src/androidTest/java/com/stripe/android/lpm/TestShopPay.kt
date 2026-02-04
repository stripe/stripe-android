package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.ShopPaySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.WalletButtonsPlaygroundType
import com.stripe.android.paymentsheet.example.playground.settings.WalletButtonsSettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestShopPay : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "shop_pay",
        requiresBrowser = false,
        authorizationAction = AuthorizeAction.ShopPay
    ) { settings ->
        settings[MerchantSettingsDefinition] = Merchant.US
        settings[CurrencySettingsDefinition] = Currency.USD
        settings[CustomerSessionSettingsDefinition] = true
        settings[CustomerSettingsDefinition] = CustomerType.NEW
        settings[AutomaticPaymentMethodsSettingsDefinition] = false
        settings[SupportedPaymentMethodsSettingsDefinition] = "card,shop_pay,klarna,amazon_pay"
        settings[ShopPaySettingsDefinition] = true
        settings[WalletButtonsSettingsDefinition] = WalletButtonsPlaygroundType.GPayAlwaysLinkAutoNeverShopPayAuto
    }

    // PaymentSheet merchant is not gated into Shop Pay, so we're testing with SPT only atm.
    @Test
    fun testShopPayWithSpt() {
        testDriver.confirmNewOrGuestCompleteWithSpt(
            testParameters = testParameters,
            buttonTag = "ShopPayButton"
        )
    }
}
