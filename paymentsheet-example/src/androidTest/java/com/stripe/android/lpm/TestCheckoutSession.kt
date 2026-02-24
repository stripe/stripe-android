package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.InitializationType
import com.stripe.android.paymentsheet.example.playground.settings.InitializationTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E test for checkout session integration with the playground.
 *
 * Tests the vanilla checkout flow with a new card payment method using
 * the checkout session API (/v1/payment_pages/{cs_id}/init and /confirm).
 */
@RunWith(AndroidJUnit4::class)
@Ignore("#ir-tallest-solar")
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
     *
     * Flow:
     * 1. Playground creates checkout session on backend
     * 2. PaymentSheet is presented with checkout session client secret
     * 3. Init API is called (/v1/payment_pages/{cs_id}/init)
     * 4. User fills out card details
     * 5. User taps "Pay"
     * 6. Payment method is created
     * 7. Confirm API is called (/v1/payment_pages/{cs_id}/confirm)
     * 8. Payment completes successfully
     */
    @Test
    fun testCardPaymentWithCheckoutSession() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                saveForFutureUseCheckboxVisible = true,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )
    }
}
