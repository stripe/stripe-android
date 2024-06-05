package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSheetPaymentMethodModeDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodMode
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.FieldPopulator
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestCardInCustomerSheet : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "card",
        executeInNightlyRun = true,
    ).copyPlaygroundSettings { settings ->
        settings[CustomerSettingsDefinition] = CustomerType.NEW
        settings[CustomerSheetPaymentMethodModeDefinition] = PaymentMethodMode.CreateAndAttach
    }

    @Test
    fun testCard() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CountrySettingsDefinition] = Country.US
            },
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    @Test
    fun testCardWith3ds2() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CountrySettingsDefinition] = Country.US
            }.copy(
                authorizationAction = AuthorizeAction.Authorize3ds2,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000000000003220")
        )
    }

    @Test
    fun testCardWithNonUsMerchant() {
        testDriver.savePaymentMethodInCustomerSheet(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CountrySettingsDefinition] = Country.FR
            },
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }
}
