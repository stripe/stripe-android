package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSheetPaymentMethodModeDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodMode
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestCardInCustomerSheet : BasePlaygroundTest() {
    @Test
    fun testCard() {
        testDriver.savePaymentMethodInCustomerSheet(
            TestParameters.create(paymentMethodCode = "card").copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[CountrySettingsDefinition] = Country.US
                settings[CustomerSheetPaymentMethodModeDefinition] = PaymentMethodMode.CreateAndAttach
            },
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun testCardWithNonUsMerchant() {
        testDriver.savePaymentMethodInCustomerSheet(
            TestParameters.create(paymentMethodCode = "card").copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.NEW
                settings[CountrySettingsDefinition] = Country.FR
                settings[CustomerSheetPaymentMethodModeDefinition] = PaymentMethodMode.CreateAndAttach
            },
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }
}
