package com.stripe.android.link

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.model.StripeIntentFixtures
import com.stripe.android.link.utils.FakeAndroidKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class LinkPaymentLauncherTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val config = LinkPaymentLauncher.Configuration(
        StripeIntentFixtures.PI_SUCCEEDED,
        MERCHANT_NAME,
        CUSTOMER_NAME,
        CUSTOMER_EMAIL,
        CUSTOMER_PHONE,
        null
    )

    private var linkPaymentLauncher = LinkPaymentLauncher(
        context,
        setOf(PRODUCT_USAGE),
        { PUBLISHABLE_KEY },
        { STRIPE_ACCOUNT_ID },
        enableLogging = true,
        ioContext = Dispatchers.IO,
        uiContext = mock(),
        paymentAnalyticsRequestFactory = mock(),
        analyticsRequestExecutor = mock(),
        stripeRepository = mock(),
        addressResourceRepository = mock()
    )

    init {
        FakeAndroidKeyStore.setup()
    }

    @Test
    fun `verify component is reused for same configuration`() {
        val component = linkPaymentLauncher.component
        linkPaymentLauncher.getAccountStatusFlow(config)
        assertThat(linkPaymentLauncher.component).isEqualTo(component)
    }

    @Test
    fun `verify component is recreated for different configuration`() {
        val component = linkPaymentLauncher.component
        linkPaymentLauncher.getAccountStatusFlow(config.copy(merchantName = "anotherName"))
        assertThat(linkPaymentLauncher.component).isNotEqualTo(component)
    }

    companion object {
        const val PRODUCT_USAGE = "productUsage"
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"

        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "email"
        const val CUSTOMER_PHONE = "phone"
        const val CUSTOMER_NAME = "name"
    }
}
