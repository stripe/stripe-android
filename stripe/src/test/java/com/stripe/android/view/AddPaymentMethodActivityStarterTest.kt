package com.stripe.android.view

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.utils.ParcelUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddPaymentMethodActivityStarterTest {

    @Test
    fun testArgsParceling() {
        val args = AddPaymentMethodActivityStarter.Args.Builder()
            .setPaymentMethodType(PaymentMethod.Type.Fpx)
            .setIsPaymentSessionActive(true)
            .setShouldRequirePostalCode(true)
            .build()

        assertEquals(args, ParcelUtils.create(args))
    }

    @Test
    fun testResultParceling() {
        val result =
            AddPaymentMethodActivityStarter.Result(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        assertEquals(result, ParcelUtils.create(result))
    }
}
