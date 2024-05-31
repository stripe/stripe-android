package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Country
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestGooglePay : BasePlaygroundTest() {
    @Test
    fun testUnitedStates() {
        testDriver.confirmWithGooglePay(Country.US)
    }

    @Test
    fun testFrance() {
        testDriver.confirmWithGooglePay(Country.FR)
    }

    @Test
    fun testGreatBritain() {
        testDriver.confirmWithGooglePay(Country.GB)
    }

    @Test
    fun testAustralia() {
        testDriver.confirmWithGooglePay(Country.AU)
    }

    @Test
    fun testBrazil() {
        testDriver.confirmWithGooglePay(Country.BR)
    }
}
