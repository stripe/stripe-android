package com.stripe.android

import android.os.Parcel
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodTest
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSessionDataTest {

    @Test
    fun updateIsPaymentReadyToCharge_noShippingRequired() {
        val config = PaymentSessionConfig.Builder()
            .setShippingInfoRequired(false)
            .setShippingMethodsRequired(false)
            .build()

        val data = PaymentSessionData()
        assertFalse(data.updateIsPaymentReadyToCharge(config))
        assertFalse(data.isPaymentReadyToCharge)

        data.paymentMethod = PAYMENT_METHOD
        assertTrue(data.updateIsPaymentReadyToCharge(config))
        assertTrue(data.isPaymentReadyToCharge)
    }

    @Test
    fun updateIsPaymentReadyToCharge_shippingRequired() {
        val config = PaymentSessionConfig.Builder()
            .setShippingInfoRequired(true)
            .setShippingMethodsRequired(true)
            .build()

        val data = PaymentSessionData()
        assertFalse(data.updateIsPaymentReadyToCharge(config))
        assertFalse(data.isPaymentReadyToCharge)

        data.paymentMethod = PAYMENT_METHOD
        assertFalse(data.updateIsPaymentReadyToCharge(config))
        assertFalse(data.isPaymentReadyToCharge)

        data.shippingInformation = ShippingInformation(null, null, null)
        assertFalse(data.updateIsPaymentReadyToCharge(config))
        assertFalse(data.isPaymentReadyToCharge)

        data.shippingMethod = ShippingMethod("label", "id", null, 0, "USD")
        assertTrue(data.updateIsPaymentReadyToCharge(config))
        assertTrue(data.isPaymentReadyToCharge)
    }

    @Test
    fun writeToParcel_withNulls_readsFromParcelCorrectly() {
        val data = PaymentSessionData()

        data.cartTotal = 100L
        data.shippingTotal = 150L
        data.isPaymentReadyToCharge = false

        val parcel = Parcel.obtain()
        data.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val parceledData = PaymentSessionData.CREATOR.createFromParcel(parcel)
        assertEquals(data, parceledData)
    }

    @Test
    fun writeToParcel_withoutNulls_readsFromParcelCorrectly() {
        val data = PaymentSessionData()

        data.cartTotal = 100L
        data.shippingTotal = 150L
        data.paymentMethod = PAYMENT_METHOD
        data.isPaymentReadyToCharge = false
        data.shippingInformation = ShippingInformation(null, null, null)
        data.shippingMethod = ShippingMethod("UPS", "SuperFast", null, 10000L, "usd")

        val parcel = Parcel.obtain()
        data.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val parceledData = PaymentSessionData.CREATOR.createFromParcel(parcel)
        assertEquals(data, parceledData)
    }

    companion object {

        private val PAYMENT_METHOD = PaymentMethod.fromJson(PaymentMethodTest.PM_CARD_JSON)
    }
}
