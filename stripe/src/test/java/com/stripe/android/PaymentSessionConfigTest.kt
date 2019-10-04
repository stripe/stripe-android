package com.stripe.android

import android.os.Parcel
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
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

        val parcel = Parcel.obtain()
        paymentSessionConfig.writeToParcel(parcel, paymentSessionConfig.describeContents())
        parcel.setDataPosition(0)

        assertEquals(paymentSessionConfig, PaymentSessionConfig.CREATOR.createFromParcel(parcel))
    }
}
