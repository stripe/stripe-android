package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Ignore(
    "Only testable on devices with a Google Play account & payment methods " +
        "available. Run these before releasing or making changes to Google Pay flow"
)
internal class TestGooglePay : BasePlaygroundTest() {
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
}
