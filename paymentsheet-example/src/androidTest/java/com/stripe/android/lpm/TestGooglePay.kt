package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestGooglePay : BasePlaygroundTest(
    block = {
        around(FeatureFlagTestRule(FeatureFlags.allowNoExistingPaymentMethodForGooglePay, true))
    }
) {
    @Test
    fun testUnitedStates() {
        testDriver.confirmWithGooglePay(Merchant.US)
    }

    @Test
    fun testFrance() {
        testDriver.confirmWithGooglePay(Merchant.FR)
    }

    @Test
    fun testGreatBritain() {
        testDriver.confirmWithGooglePay(Merchant.GB)
    }

    @Test
    fun testAustralia() {
        testDriver.confirmWithGooglePay(Merchant.AU)
    }

    @Test
    fun testBrazil() {
        testDriver.confirmWithGooglePay(Merchant.BR)
    }

    @Test
    fun testJapan() {
        testDriver.confirmWithGooglePay(Merchant.JP)
    }

    @Test
    fun testMexico() {
        testDriver.confirmWithGooglePay(Merchant.MX)
    }

    @Test
    fun testSetupIntentInEEA() {
        testDriver.confirmWithGooglePay(
            merchant = Merchant.FR,
            checkoutMode = CheckoutMode.SETUP
        )
    }

    @Test
    fun testSetupIntentInUS() {
        testDriver.confirmWithGooglePay(
            merchant = Merchant.US,
            checkoutMode = CheckoutMode.SETUP
        )
    }

    @Test
    fun testCheckoutSessionWithFlowController() {
        testDriver.confirmGooglePayWithCheckoutSession(
            merchant = Merchant.US,
            customerEmail = "test@example.com",
        )
    }
}
