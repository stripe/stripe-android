package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.utils.GooglePayTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestGooglePay : BasePlaygroundTest() {
    @Test
    @GooglePayTest
    fun testUnitedStates() {
        testDriver.confirmWithGooglePay(Country.US)
    }

    @Test
    @GooglePayTest
    fun testFrance() {
        testDriver.confirmWithGooglePay(Country.FR)
    }

    @Test
    @GooglePayTest
    fun testGreatBritain() {
        testDriver.confirmWithGooglePay(Country.GB)
    }

    @Test
    @GooglePayTest
    fun testAustralia() {
        testDriver.confirmWithGooglePay(Country.AU)
    }

    @Test
    @GooglePayTest
    fun testBrazil() {
        testDriver.confirmWithGooglePay(Country.BR)
    }

    @Test
    @GooglePayTest
    fun testJapan() {
        testDriver.confirmWithGooglePay(Country.JP)
    }

    @Test
    @GooglePayTest
    fun testMexico() {
        testDriver.confirmWithGooglePay(Country.MX)
    }
}
