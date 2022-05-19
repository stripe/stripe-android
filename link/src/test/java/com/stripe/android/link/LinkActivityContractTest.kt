package com.stripe.android.link

import android.os.Bundle
import android.os.Parcel
import com.stripe.android.link.model.StripeIntentFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class LinkActivityContractTest {

    @Test
    fun `LinkActivityContract Args parcelizes correctly`() {
        val injectionParams = LinkActivityContract.Args.InjectionParams(
            "injectorKey",
            setOf("Product Usage"),
            true,
            "publishableKey",
            "stripeAccountId"
        )

        val args = LinkActivityContract.Args(
            StripeIntentFixtures.PI_SUCCEEDED,
            true,
            "Merchant, Inc",
            "customer@email.com",
            injectionParams
        )

        val bundle = Bundle()
        bundle.putParcelable("args", args)

        val parcel = Parcel.obtain()
        bundle.writeToParcel(parcel, 0)

        parcel.setDataPosition(0)
        val unparceledBundle = requireNotNull(parcel.readBundle())
        val unparceledArgs = unparceledBundle.getParcelable<LinkActivityContract.Args>("args")

        assertEquals(unparceledArgs, args)
    }
}
