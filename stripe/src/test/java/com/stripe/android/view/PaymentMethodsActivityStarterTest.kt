package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import com.stripe.android.utils.ParcelUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.android.synthetic.main.card_input_widget.view.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodsActivityStarterTest {

    @Test
    fun testParceling() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        val args = PaymentMethodsActivityStarter.Args.Builder()
            .setInitialPaymentMethodId("pm_12345")
            .setIsPaymentSessionActive(true)
            .setShouldRequirePostalCode(true)
            .setPaymentMethodTypes(listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Fpx))
            .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
            .setAddPaymentMethodFooter(R.layout.activity_payment_methods)
            .build()

        assertEquals(args, ParcelUtils.create(args))
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
}
