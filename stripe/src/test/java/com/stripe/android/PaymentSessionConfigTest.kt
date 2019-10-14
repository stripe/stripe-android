package com.stripe.android

import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.utils.ParcelUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSessionConfigTest {

    @Test
    fun testParcel() {
        val paymentSessionConfig = PaymentSessionConfig.Builder()
            .setHiddenShippingInfoFields("field1", "field2")
            .setOptionalShippingInfoFields("field3", "field4")
            .setPrepopulatedShippingInfo(ShippingInformation(
                Address.Builder().build(), null, null))
            .setShippingInfoRequired(true)
            .setShippingMethodsRequired(true)
            .setPaymentMethodTypes(listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Fpx))
            .build()

        assertEquals(
            paymentSessionConfig,
            ParcelUtils.create(paymentSessionConfig, PaymentSessionConfig.CREATOR)
        )
    }
}
