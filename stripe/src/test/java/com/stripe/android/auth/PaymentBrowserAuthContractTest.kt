package com.stripe.android.auth

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.StripeBrowserLauncherActivity
import com.stripe.android.view.PaymentAuthWebViewActivity
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentBrowserAuthContractTest {

    private val defaultReturnUrl = DefaultReturnUrl(
        "com.example.app"
    )

    @Test
    fun `createIntent() when custom tabs supported and custom return_url should use PaymentAuthWebViewActivity`() {
        val intent = PaymentBrowserAuthContract(
            defaultReturnUrl,
            isCustomTabsSupported = { true }
        ).createIntent(
            ApplicationProvider.getApplicationContext(),
            ARGS.copy(
                returnUrl = "myapp://custom"
            )
        )

        assertThat(intent.component?.className)
            .isEqualTo(PaymentAuthWebViewActivity::class.java.name)
    }

    @Test
    fun `createIntent() when custom tabs supported and default return_url should use StripeBrowserLauncherActivity`() {
        val intent = PaymentBrowserAuthContract(
            defaultReturnUrl,
            isCustomTabsSupported = { true }
        ).createIntent(
            ApplicationProvider.getApplicationContext(),
            ARGS.copy(
                returnUrl = defaultReturnUrl.value
            )
        )

        assertThat(intent.component?.className)
            .isEqualTo(StripeBrowserLauncherActivity::class.java.name)
    }

    @Test
    fun `createIntent() when custom tabs not supported and default return_url should use StripeBrowserLauncherActivity`() {
        val intent = PaymentBrowserAuthContract(
            defaultReturnUrl,
            isCustomTabsSupported = { false }
        ).createIntent(
            ApplicationProvider.getApplicationContext(),
            ARGS.copy(
                returnUrl = defaultReturnUrl.value
            )
        )

        assertThat(intent.component?.className)
            .isEqualTo(PaymentAuthWebViewActivity::class.java.name)
    }

    private companion object {
        private val ARGS = PaymentBrowserAuthContract.Args(
            clientSecret = "client_secret",
            objectId = "pi_12345",
            requestCode = 5000,
            url = "https://mybank.com/auth",
            returnUrl = "myapp://custom"
        )
    }
}
