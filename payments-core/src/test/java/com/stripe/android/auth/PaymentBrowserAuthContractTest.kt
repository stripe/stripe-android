package com.stripe.android.auth

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Parcel
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.StripeBrowserLauncherActivity
import com.stripe.android.utils.createTestActivityRule
import com.stripe.android.view.PaymentAuthWebViewActivity
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentBrowserAuthContractTest {

    private val defaultReturnUrl = DefaultReturnUrl(
        "com.stripe.android.test"
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var activity: Activity

    @get:Rule
    internal val testActivityRule = createTestActivityRule<TestActivity>()

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        ActivityScenario.launch(TestActivity::class.java).use { activityScenario ->
            activityScenario.onActivity {
                activity = it
            }
        }
    }

    @Test
    fun `createIntent() when has compatible browser and custom return_url should use PaymentAuthWebViewActivity`() {
        val intent = PaymentBrowserAuthContract().createIntent(
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
        val intent = PaymentBrowserAuthContract().createIntent(
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
        val intent = PaymentBrowserAuthContract().createIntent(
            activity,
            ARGS.copy(
                returnUrl = defaultReturnUrl.value
            )
        )

        assertThat(intent.component?.className)
            .isEqualTo(StripeBrowserLauncherActivity::class.java.name)
    }

    @Test
    fun `createIntent() when isInstantApp should use StripeBrowserLauncherActivity`() {
        val intent = PaymentBrowserAuthContract().createIntent(
            activity,
            ARGS.copy(
                isInstantApp = true
            )
        )

        assertThat(intent.component?.className)
            .isEqualTo(StripeBrowserLauncherActivity::class.java.name)
    }

    @Test
    fun `unparcel when no parameters as when started from StripeBrowserLauncherActivity`() {
        val parcel = Parcel.obtain()

        // An NullPointerException is thrown if a constructor doesn't exist that
        // takes a parcel and created the object with empty strings if they
        // don't exist.
        PaymentBrowserAuthContract.Args(parcel)
    }

    private companion object {
        private val ARGS = PaymentBrowserAuthContract.Args(
            clientSecret = "client_secret",
            objectId = "pi_12345",
            requestCode = 5000,
            url = "https://mybank.com/auth",
            returnUrl = "myapp://custom",
            statusBarColor = Color.RED,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            isInstantApp = false
        )
    }

    internal class TestActivity : Activity()
}
