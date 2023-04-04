package com.stripe.android.paymentsheet.addresselement

import android.os.Bundle
import android.os.Parcel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AddressElementActivityContractTest {

    @Test
    fun `AddressElementActivityContract args parcelize correctly`() {
        val args = AddressElementActivityContract.Args(
            "publishableKey",
            AddressLauncherFixtures.BASIC_CONFIG,
            "injectorKey"
        )

        val bundle = Bundle()
        bundle.putParcelable("args", args)

        val parcel = Parcel.obtain()
        bundle.writeToParcel(parcel, 0)

        parcel.setDataPosition(0)
        val unparceledBundle = requireNotNull(parcel.readBundle())
        @Suppress("DEPRECATION")
        val unparceledArgs = unparceledBundle.getParcelable<AddressElementActivityContract.Args>("args")

        assertEquals(unparceledArgs, args)
    }
}
