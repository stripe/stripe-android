package com.stripe.android.link

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkPaymentLauncherTest {
    private val mockHostActivityLauncher =
        mock<ActivityResultLauncher<LinkActivityContract.Args>>()

    private val linkPaymentLauncher = LinkPaymentLauncher(
        mockHostActivityLauncher,
        mock(),
        setOf(PRODUCT_USAGE),
        { PUBLISHABLE_KEY },
        { STRIPE_ACCOUNT_ID },
        enableLogging = true,
        ioContext = mock(),
        paymentAnalyticsRequestFactory = mock(),
        analyticsRequestExecutor = mock(),
        stripeRepository = mock()
    )

    @Test
    fun `verify present() launches LinkActivity with correct arguments`() {
        linkPaymentLauncher.present(MERCHANT_NAME)

        verify(mockHostActivityLauncher).launch(
            argWhere { arg ->
                arg.merchantName.equals(MERCHANT_NAME) &&
                    arg.injectionParams != null &&
                    arg.injectionParams.productUsage.equals(setOf(PRODUCT_USAGE)) &&
                    arg.injectionParams.injectorKey == LinkPaymentLauncher::class.simpleName + WeakMapInjectorRegistry.CURRENT_REGISTER_KEY.get() &&
                    arg.injectionParams.enableLogging &&
                    arg.injectionParams.publishableKey.equals(PUBLISHABLE_KEY) &&
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
