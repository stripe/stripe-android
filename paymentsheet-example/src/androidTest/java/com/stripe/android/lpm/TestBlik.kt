package com.stripe.android.lpm

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestBlik : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "blik",
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.FR
        settings[CurrencySettingsDefinition] = Currency.PLN
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf("card", "blik")
    }.copy(
        authorizationAction = AuthorizeAction.PollingSucceedsAfterDelay,
    )

    @Test
    fun testBlik() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
            populateCustomLpmFields = {
                rules.compose.onNodeWithText("BLIK code").apply {
                    performTextInput(
                        "123456"
                    )
                }
            },
        )
    }
}
