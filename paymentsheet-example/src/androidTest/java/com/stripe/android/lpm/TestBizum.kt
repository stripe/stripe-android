package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.FieldPopulator
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestBizum : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "bizum",
    ) { settings ->
        settings[MerchantSettingsDefinition] = Merchant.ES
        settings[CurrencySettingsDefinition] = Currency.EUR
        settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.Off
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.Bizum,
        ).joinToString(",")
    }.copy(
        authorizationAction = AuthorizeAction.PollingSucceedsAfterDelay,
    )

    @Test
    fun testBizum() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
            values = FieldPopulator.Values(
                phoneNumber = "600000001",
            ),
            populateCustomLpmFields = {
                populateCountryCodeSelector("Spain")
                populatePhoneNumber()
            },
        )
    }
}
