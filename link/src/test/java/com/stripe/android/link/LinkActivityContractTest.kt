package com.stripe.android.link

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.model.StripeIntentFixtures
import com.stripe.android.networking.StripeRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
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
    fun `LinkActivityContract creates intent with URL with native link disabled`() {
        FeatureFlags.nativeLinkEnabled.setEnabled(false)
        val config = LinkConfiguration(
            stripeIntent = StripeIntentFixtures.PI_SUCCEEDED,
            merchantName = "Merchant, Inc",
            merchantCountryCode = "US",
            customerInfo = LinkConfiguration.CustomerInfo(
                name = "Name",
                email = "customer@email.com",
                phone = "1234567890",
                billingCountryCode = "US",
            ),
            shippingValues = null,
            passthroughModeEnabled = false,
            flags = emptyMap(),
            cardBrandChoice = null,
        )

        val args = LinkActivityContract.Args(
            config,
        )
        val stripeRepository = mock<StripeRepository>()
        whenever(stripeRepository.buildPaymentUserAgent(any())).thenReturn("test")
        val contract = LinkActivityContract(stripeRepository)
        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)
        assertThat(intent.component?.className).isEqualTo(LinkForegroundActivity::class.java.name)
        assertThat(intent.extras?.getString(LinkForegroundActivity.EXTRA_POPUP_URL)).startsWith(
            "https://checkout.link.com/#"
        )
    }

    @Test
    fun `LinkActivityContract creates intent with with NativeLinkArgs when native link is enabled`() {
        FeatureFlags.nativeLinkEnabled.setEnabled(true)
        val config = LinkConfiguration(
            stripeIntent = StripeIntentFixtures.PI_SUCCEEDED,
            merchantName = "Merchant, Inc",
            merchantCountryCode = "US",
            customerInfo = LinkConfiguration.CustomerInfo(
                name = "Name",
                email = "customer@email.com",
                phone = "1234567890",
                billingCountryCode = "US",
            ),
            shippingValues = null,
            passthroughModeEnabled = false,
            flags = emptyMap(),
            cardBrandChoice = null,
        )

        val args = LinkActivityContract.Args(
            config,
        )
        val stripeRepository = mock<StripeRepository>()
        whenever(stripeRepository.buildPaymentUserAgent(any())).thenReturn("test")
        val contract = LinkActivityContract(stripeRepository)
        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)
        assertThat(intent.component?.className).isEqualTo(LinkActivity::class.java.name)
        assertThat(intent.extras?.getParcelable<NativeLinkArgs>(LinkActivity.EXTRA_ARGS)).isEqualTo(
            NativeLinkArgs(
                configuration = config,
                publishableKey = "pk_test_abcdefg",
                stripeAccountId = null
            )
        )
    }
}
