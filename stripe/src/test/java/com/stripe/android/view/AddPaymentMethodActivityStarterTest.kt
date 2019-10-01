package com.stripe.android.view

import android.content.Intent
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
    fun testParceling() {
        val args = AddPaymentMethodActivityStarter.Args.Builder()
            .setPaymentMethodType(PaymentMethod.Type.Fpx)
            .setIsPaymentSessionActive(true)
            .setShouldRequirePostalCode(true)
            .build()

        val createdArgs =
            ParcelUtils.create<AddPaymentMethodActivityStarter.Args>(
                args,
                AddPaymentMethodActivityStarter.Args.CREATOR
            )
        assertEquals(args, createdArgs)
    }

    @Test
    fun testResultParceling() {
        val bundle =
            AddPaymentMethodActivityStarter.Result(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                .toBundle()
        val result =
            AddPaymentMethodActivityStarter.Result.fromIntent(Intent().putExtras(bundle))
        assertEquals(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            result?.paymentMethod
        )
    }
}
