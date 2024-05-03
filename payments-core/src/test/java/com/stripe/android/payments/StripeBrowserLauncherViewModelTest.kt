package com.stripe.android.payments

import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.browser.BrowserCapabilities
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class StripeBrowserLauncherViewModelTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val analyticsRequests = mutableListOf<AnalyticsRequest>()
    private val analyticsRequestExecutor = AnalyticsRequestExecutor {
        analyticsRequests.add(it)
    }
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        application,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )

    private val savedStateHandle = SavedStateHandle()

    @Test
    fun `createLaunchIntent() should create an intent and wrap in a Chooser Intent`() {
        val viewModel = createViewModel()
        val launchIntent = viewModel.createLaunchIntent(ARGS)

        val browserIntent = requireNotNull(launchIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))

        assertThat(browserIntent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(browserIntent.data).isEqualTo(Uri.parse("https://bank.com"))
        assertThat(launchIntent.getStringExtra(Intent.EXTRA_TITLE)).isEqualTo("Verify your payment")
    }

    @Test
    fun `logCapabilities() should log expected event when using Chrome Custom Tabs`() {
        val viewModel = createViewModel(browserCapabilities = BrowserCapabilities.CustomTabs)

        viewModel.createLaunchIntent(ARGS)
        assertThat(analyticsRequests).hasSize(1)

        assertThat(analyticsRequests.first().params["event"])
            .isEqualTo("stripe_android.auth_with_customtabs")
    }

    @Test
    fun `logCapabilities() should log expected event when not using Chrome Custom Tabs`() {
        val viewModel = createViewModel(browserCapabilities = BrowserCapabilities.Unknown)

        viewModel.createLaunchIntent(ARGS)
        assertThat(analyticsRequests).hasSize(1)

        assertThat(analyticsRequests.first().params["event"])
            .isEqualTo("stripe_android.auth_with_defaultbrowser")
    }

    @Test
    fun `getResultIntent() with shouldCancelSource=true should include expected PaymentFlowResult`() {
        val viewModel = createViewModel()
        val intent = viewModel.getResultIntent(
            ARGS.copy(shouldCancelSource = true)
        )
        val result = intent.getParcelableExtra<PaymentFlowResult.Unvalidated>("extra_args")
        assertThat(result)
            .isEqualTo(
                PaymentFlowResult.Unvalidated(
                    clientSecret = "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
                    canCancelSource = true,
                    sourceId = ""
                )
            )
    }

    @Test
    fun `hasLaunched should set entry on savedStateHandle`() {
        val viewModel = createViewModel()
        assertThat(
            savedStateHandle.contains(StripeBrowserLauncherViewModel.KEY_HAS_LAUNCHED)
        ).isFalse()

        viewModel.hasLaunched = true

        assertThat(
            savedStateHandle.contains(StripeBrowserLauncherViewModel.KEY_HAS_LAUNCHED)
        ).isTrue()
    }

    private fun createViewModel(
        browserCapabilities: BrowserCapabilities = BrowserCapabilities.CustomTabs,
    ): StripeBrowserLauncherViewModel {
        return StripeBrowserLauncherViewModel(
            analyticsRequestExecutor = analyticsRequestExecutor,
            paymentAnalyticsRequestFactory = analyticsRequestFactory,
            browserCapabilities = browserCapabilities,
            intentChooserTitle = "Verify your payment",
            resolveErrorMessage = "Unable to resolve things",
            savedStateHandle = savedStateHandle,
        )
    }

    private companion object {
        private val ARGS = PaymentBrowserAuthContract.Args(
            objectId = "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
            requestCode = 50000,
            clientSecret = "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
            url = "https://bank.com",
            statusBarColor = Color.RED,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            isInstantApp = false
        )
    }
}
