package com.stripe.android.lpm

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutSessionSaveSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.InitializationType
import com.stripe.android.paymentsheet.example.playground.settings.InitializationTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TEST_TAG
import com.stripe.android.test.core.FieldPopulator
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E test for checkout session integration with the playground.
 *
 * Tests the checkout flow with the checkout session API
 * (/v1/payment_pages/{cs_id}/init and /confirm).
 */
@RunWith(AndroidJUnit4::class)
internal class TestCheckoutSession : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = "card",
        authorizationAction = null,
    ) { settings ->
        settings[InitializationTypeSettingsDefinition] = InitializationType.CheckoutSession
        settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT
        settings[MerchantSettingsDefinition] = Merchant.US
        settings[CurrencySettingsDefinition] = Currency.USD
    }

    /**
     * Test a successful card payment with checkout session.
     */
    @Test
    fun testCardPaymentWithCheckoutSession() {
        testDriver.confirmNewOrGuestComplete(
            testParameters,
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }

    /**
     * Test saving a payment method via checkout session and reusing it.
     *
     * Flow:
     * 1. Create a new customer with checkout session + save enabled
     * 2. Fill card details, check save checkbox, confirm payment
     * 3. Re-launch with same customer
     * 4. Verify saved card is pre-selected and can be used to pay
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSaveAndReuseSavedPaymentMethod() {
        val testParameters = testParameters.copy(
            saveForFutureUseCheckboxVisible = true,
            saveCheckboxValue = true,
        ).copyPlaygroundSettings { settings ->
            settings[CheckoutSessionSaveSettingsDefinition] = true
        }

        val state = testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
            values = FieldPopulator.Values(
                cardNumber = "4242424242424242",
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        testDriver.confirmCompleteWithDefaultSavedPaymentMethod(
            customerId = state?.customerId(),
            testParameters = testParameters,
            beforeBuyAction = { selectors ->
                selectors.composeTestRule.waitUntilExactlyOneExists(
                    matcher = hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG)
                        .and(isSelected())
                        .and(hasText("4242", substring = true)),
                    timeoutMillis = 5000L,
                )
            },
        )
    }
}
