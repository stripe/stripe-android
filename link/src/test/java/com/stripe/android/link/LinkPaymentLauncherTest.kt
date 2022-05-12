package com.stripe.android.link

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.link.model.StripeIntentFixtures
import com.stripe.android.link.utils.FakeAndroidKeyStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class LinkPaymentLauncherTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mockHostActivityLauncher = mock<ActivityResultLauncher<LinkActivityContract.Args>>()

    private var linkPaymentLauncher = LinkPaymentLauncher(
        MERCHANT_NAME,
        null,
        context,
        setOf(PRODUCT_USAGE),
        { PUBLISHABLE_KEY },
        { STRIPE_ACCOUNT_ID },
        enableLogging = true,
        ioContext = mock(),
        uiContext = mock(),
        paymentAnalyticsRequestFactory = mock(),
        analyticsRequestExecutor = mock(),
        stripeRepository = mock(),
        resourceRepository = mock()
    )

    init {
        FakeAndroidKeyStore.setup()
    }

    @Test
    fun `verify present() launches LinkActivity with correct arguments`() = runTest {
        val stripeIntent = StripeIntentFixtures.PI_SUCCEEDED
        linkPaymentLauncher.setup(stripeIntent, true, this)
        linkPaymentLauncher.present(mockHostActivityLauncher)

        verify(mockHostActivityLauncher).launch(
            argWhere { arg ->
                arg.stripeIntent == stripeIntent &&
                    arg.injectionParams != null &&
                    arg.injectionParams.productUsage == setOf(PRODUCT_USAGE) &&
                    arg.injectionParams.injectorKey == LinkPaymentLauncher::class.simpleName + WeakMapInjectorRegistry.CURRENT_REGISTER_KEY.get() &&
                    arg.injectionParams.enableLogging &&
                    arg.injectionParams.publishableKey == PUBLISHABLE_KEY &&
                    arg.injectionParams.stripeAccountId.equals(STRIPE_ACCOUNT_ID)
            }
        )
    }

    companion object {
        const val PRODUCT_USAGE = "productUsage"
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"

        const val MERCHANT_NAME = "merchantName"
    }
}
