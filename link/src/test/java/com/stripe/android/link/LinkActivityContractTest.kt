package com.stripe.android.link

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.model.StripeIntentFixtures
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
        assertThat(intent.extras?.getString(LinkForegroundActivity.EXTRA_POPUP_URL)).startsWith(
            "https://checkout.link.com/#"
        )
    }
}
