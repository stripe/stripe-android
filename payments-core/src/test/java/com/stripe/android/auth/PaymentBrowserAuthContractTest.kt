package com.stripe.android.auth

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.StripeBrowserLauncherActivity
import com.stripe.android.view.ActivityScenarioFactory
import com.stripe.android.view.PaymentAuthWebViewActivity
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentBrowserAuthContractTest {

    private val defaultReturnUrl = DefaultReturnUrl(
        "com.example.app"
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)
    private lateinit var activity: Activity

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        activityScenarioFactory.createAddPaymentMethodActivity().use { activityScenario ->
            activityScenario.onActivity {
                activity = it
            }
        }
    }

    @Test
    fun `createIntent() when has compatible browser and custom return_url should use PaymentAuthWebViewActivity`() {
        val intent = PaymentBrowserAuthContract(
            defaultReturnUrl,
            hasCompatibleBrowser = { true }
        ).createIntent(
            activity,
            ARGS.copy(
                returnUrl = "myapp://custom"
            )
        )

        assertThat(intent.component?.className)
            .isEqualTo(PaymentAuthWebViewActivity::class.java.name)
    }

    @Test
    fun `createIntent() when has compatible browser and default return_url should use StripeBrowserLauncherActivity`() {
        val intent = PaymentBrowserAuthContract(
            defaultReturnUrl,
            hasCompatibleBrowser = { true }
        ).createIntent(
            activity,
            ARGS.copy(
                returnUrl = defaultReturnUrl.value
            )
        )

        assertThat(intent.component?.className)
            .isEqualTo(StripeBrowserLauncherActivity::class.java.name)
    }

    @Test
    fun `createIntent() when no compatible browser and default return_url should use StripeBrowserLauncherActivity`() {
        val intent = PaymentBrowserAuthContract(
            defaultReturnUrl,
            hasCompatibleBrowser = { false }
        ).createIntent(
            activity,
            ARGS.copy(
                returnUrl = defaultReturnUrl.value
            )
        )

        assertThat(intent.component?.className)
            .isEqualTo(PaymentAuthWebViewActivity::class.java.name)
    }

    @Test
    fun `createIntent() should set statusBarColor from activity`() {
        val intent = PaymentBrowserAuthContract(
            defaultReturnUrl,
            hasCompatibleBrowser = { false }
        ).createIntent(
            activity,
            ARGS
        )

        val args = requireNotNull(
            intent.getParcelableExtra<PaymentBrowserAuthContract.Args>("extra_args")
        )
        assertThat(args.statusBarColor)
            .isNotNull()
        assertThat(args.statusBarColor)
            .isEqualTo(activity.window.statusBarColor)
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
