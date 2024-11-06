package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
class PaymentMethodsActivityStarterTest {

    @Test
    fun testArgsParceling() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        val args = PaymentMethodsActivityStarter.Args.Builder()
            .setInitialPaymentMethodId("pm_12345")
            .setIsPaymentSessionActive(true)
            .setPaymentMethodTypes(listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Fpx))
            .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
            .setAddPaymentMethodFooter(R.layout.stripe_payment_methods_activity)
            .setBillingAddressFields(BillingAddressFields.Full)
            .build()

        assertEquals(args, ParcelUtils.create(args))
    }

    @Test
    fun testResultParceling() {
        val result = PaymentMethodsActivityStarter.Result(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            true
        )
        assertEquals(result, ParcelUtils.create(result))
    }

    @Test
    fun testDefaultPaymentMethodTypes_isCard() {
        assertEquals(
            listOf(PaymentMethod.Type.Card),
            PaymentMethodsActivityStarter.Args.Builder()
                .build()
                .paymentMethodTypes
        )
    }

    @Test
    fun testDisableDeletePaymentMethods() {
        assertFalse(
            PaymentMethodsActivityStarter.Args.Builder()
                .setCanDeletePaymentMethods(false)
                .build()
                .canDeletePaymentMethods
        )
    }
}
