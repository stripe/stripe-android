package com.stripe.android

import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import com.stripe.android.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PaymentSessionDataTest {

    @Test
    fun updateIsPaymentReadyToCharge_noShippingRequired() {
        val config = PaymentSessionConfig.Builder()
            .setShippingInfoRequired(false)
            .setShippingMethodsRequired(false)
            .build()

        val data = PaymentSessionData(config)
        assertFalse(data.isPaymentReadyToCharge)
        assertTrue(
            data.copy(paymentMethod = PAYMENT_METHOD).isPaymentReadyToCharge
        )
    }

    @Test
    fun updateIsPaymentReadyToCharge_shippingRequired() {
        val config = PaymentSessionFixtures.CONFIG

        assertFalse(PaymentSessionData(config).isPaymentReadyToCharge)

        assertFalse(
            PaymentSessionData(
                isShippingInfoRequired = config.isShippingInfoRequired,
                isShippingMethodRequired = config.isShippingMethodRequired,
                paymentMethod = PAYMENT_METHOD
            ).isPaymentReadyToCharge
        )

        assertFalse(
            PaymentSessionData(
                isShippingInfoRequired = config.isShippingInfoRequired,
                isShippingMethodRequired = config.isShippingMethodRequired,
                paymentMethod = PAYMENT_METHOD,
                shippingInformation = ShippingInformation()
            ).isPaymentReadyToCharge
        )

        assertTrue(
            PaymentSessionData(
                isShippingInfoRequired = config.isShippingInfoRequired,
                isShippingMethodRequired = config.isShippingMethodRequired,
                paymentMethod = PAYMENT_METHOD,
                shippingInformation = ShippingInformation(),
                shippingMethod = ShippingMethod("label", "id", 0, "USD")
            ).isPaymentReadyToCharge
        )
    }

    @Test
    fun writeToParcel_withNulls_readsFromParcelCorrectly() {
        val data = PaymentSessionData(
            isShippingInfoRequired = true,
            isShippingMethodRequired = true,
            cartTotal = 100L,
            shippingTotal = 150L
        )

        assertEquals(data, ParcelUtils.create(data))
    }

    @Test
    fun writeToParcel_withoutNulls_readsFromParcelCorrectly() {
        val data = PaymentSessionData(
            isShippingInfoRequired = true,
            isShippingMethodRequired = true,
            cartTotal = 100L,
            shippingTotal = 150L,
            paymentMethod = PAYMENT_METHOD,
            shippingInformation = ShippingInformation(),
            shippingMethod = ShippingMethod("UPS", "SuperFast", 10000L, "USD")
        )

        assertEquals(data, ParcelUtils.create(data))
    }

    private companion object {
        private val PAYMENT_METHOD = PaymentMethodFixtures.CARD_PAYMENT_METHOD
    }
}
