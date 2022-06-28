package com.stripe.android.paymentsheet.addresselement

import android.os.Bundle
import android.os.Parcel
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AddressElementActivityContractTest {

    @Test
    fun `AddressElementActivityContract args parcelize correctly`() {
        val injectionParams = AddressElementActivityContract.Args.InjectionParams(
            "injectorKey",
            setOf("Product Usage"),
            true
        )

        val args = AddressElementActivityContract.Args(
            PaymentIntentFixtures.PI_SUCCEEDED,
            PaymentSheetFixtures.CONFIG_CUSTOMER,
            injectionParams
        )

        val bundle = Bundle()
        bundle.putParcelable("args", args)

        val parcel = Parcel.obtain()
        bundle.writeToParcel(parcel, 0)

        parcel.setDataPosition(0)
        val unparceledBundle = requireNotNull(parcel.readBundle())
        val unparceledArgs = unparceledBundle.getParcelable<AddressElementActivityContract.Args>("args")

        assertEquals(unparceledArgs, args)
    }
}
