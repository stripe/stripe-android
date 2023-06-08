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
        val config = LinkPaymentLauncher.Configuration(
            stripeIntent = StripeIntentFixtures.PI_SUCCEEDED,
            merchantName = "Merchant, Inc",
            customerName = "Name",
            customerEmail = "customer@email.com",
            customerPhone = "1234567890",
            customerBillingCountryCode = "US",
            shippingValues = null,
        )

        val args = LinkActivityContract.Args(
            config,
            null,
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
