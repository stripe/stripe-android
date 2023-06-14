package com.stripe.android.link

import android.os.Bundle
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.model.StripeIntentFixtures
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class LinkActivityContractTest {

    @Before
    fun before() {
        PaymentConfiguration.init(
            context = ApplicationProvider.getApplicationContext(),
            publishableKey = "pk_test_abcdefg",
        )
    }

    @After
    fun after() {
        PaymentConfiguration.clearInstance()
    }

    @Test
    fun `LinkActivityContract Args parcelizes correctly`() {
        val config = LinkConfiguration(
            stripeIntent = StripeIntentFixtures.PI_SUCCEEDED,
            merchantName = "Merchant, Inc",
            merchantCountryCode = "US",
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

    @Test
    fun `LinkActivityContract creates intent with URL`() {
        val config = LinkConfiguration(
            stripeIntent = StripeIntentFixtures.PI_SUCCEEDED,
            merchantName = "Merchant, Inc",
            merchantCountryCode = "US",
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
        val contract = LinkActivityContract()
        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)
        assertThat(intent.component?.className).isEqualTo(LinkForegroundActivity::class.java.name)
        assertThat(intent.extras?.getString(LinkForegroundActivity.EXTRA_POPUP_URL)).isEqualTo(
            "https://checkout.link.com/link-popup.html#" +
                "eyJwdWJsaXNoYWJsZUtleSI6InBrX3Rlc3RfYWJjZGVmZyIsInN0cmlwZUFjY291bnQiOm51bGws" +
                "Im1lcmNoYW50SW5mbyI6eyJidXNpbmVzc05hbWUiOiJNZXJjaGFudCwgSW5jIiwiY291bnRyeSI6" +
                "IlVTIn0sImN1c3RvbWVySW5mbyI6eyJlbWFpbCI6ImN1c3RvbWVyQGVtYWlsLmNvbSIsImNvdW50" +
                "cnkiOiJVUyJ9LCJwYXltZW50SW5mbyI6eyJjdXJyZW5jeSI6InVzZCIsImFtb3VudCI6MTA5OX0s" +
                "InJldHVyblVybCI6InN0cmlwZXNkazovL2xpbmtfcmV0dXJuX3VybC9jb20uc3RyaXBlLmFuZHJv" +
                "aWQubGluay50ZXN0IiwibG9jYWxlIjoiVVMifQ=="
        )
    }
}
