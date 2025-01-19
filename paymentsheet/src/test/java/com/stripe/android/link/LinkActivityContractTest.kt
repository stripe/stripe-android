package com.stripe.android.link

import androidx.core.os.BundleCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.networking.StripeRepository
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkActivityContractTest {

    @get:Rule
    val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.nativeLinkEnabled,
        isEnabled = false
    )

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
        featureFlagTestRule.setEnabled(false)

        val args = LinkActivityContract.Args(TestFactory.LINK_CONFIGURATION)
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
        featureFlagTestRule.setEnabled(true)

        val args = LinkActivityContract.Args(TestFactory.LINK_CONFIGURATION)
        val stripeRepository = mock<StripeRepository>()
        whenever(stripeRepository.buildPaymentUserAgent(any())).thenReturn("test")
        val contract = LinkActivityContract(stripeRepository)
        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)
        assertThat(intent.component?.className).isEqualTo(LinkActivity::class.java.name)

        val actualArg = intent.extras?.let {
            BundleCompat.getParcelable(it, LinkActivity.EXTRA_ARGS, NativeLinkArgs::class.java)
        }
        assertThat(actualArg).isEqualTo(
            NativeLinkArgs(
                configuration = TestFactory.LINK_CONFIGURATION,
                publishableKey = "pk_test_abcdefg",
                stripeAccountId = null
            )
        )
    }
}
