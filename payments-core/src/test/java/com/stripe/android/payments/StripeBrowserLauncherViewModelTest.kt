package com.stripe.android.payments

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.auth.PaymentBrowserAuthContract
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

    private val viewModel = StripeBrowserLauncherViewModel(
        analyticsRequestExecutor,
        analyticsRequestFactory,
        BrowserCapabilities.CustomTabs,
        "Verify your payment",
        savedStateHandle
    )

    @Test
    fun `createLaunchIntent() should create an intent and wrap in a Chooser Intent`() {
        val launchIntent = viewModel.createLaunchIntent(ARGS)

        val browserIntent =
            requireNotNull(launchIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
        assertThat(browserIntent.action)
            .isEqualTo(Intent.ACTION_VIEW)
        assertThat(browserIntent.data)
            .isEqualTo(Uri.parse("https://bank.com"))
        assertThat(launchIntent.getStringExtra(Intent.EXTRA_TITLE))
            .isEqualTo("Verify your payment")

        // createLaunchIntent() should make an analytics request
        assertThat(analyticsRequests)
            .hasSize(1)
    }

    @Test
    fun `logCapabilities() when shouldUseCustomTabs = true should log expected event`() {
        viewModel.logCapabilities(shouldUseCustomTabs = true)

        assertThat(analyticsRequests)
            .hasSize(1)

        assertThat(analyticsRequests.first().params["event"])
            .isEqualTo("stripe_android.auth_with_customtabs")
    }

    @Test
    fun `logCapabilities() when shouldUseCustomTabs = false should log expected event`() {
        viewModel.logCapabilities(shouldUseCustomTabs = false)

        assertThat(analyticsRequests)
            .hasSize(1)

        assertThat(analyticsRequests.first().params["event"])
            .isEqualTo("stripe_android.auth_with_defaultbrowser")
    }

    @Test
    fun `getResultIntent() with shouldCancelSource=true should include expected PaymentFlowResult`() {
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
        assertThat(
            savedStateHandle.contains(StripeBrowserLauncherViewModel.KEY_HAS_LAUNCHED)
        ).isFalse()

        viewModel.hasLaunched = true

        assertThat(
            savedStateHandle.contains(StripeBrowserLauncherViewModel.KEY_HAS_LAUNCHED)
        ).isTrue()
    }

    private companion object {
        private val ARGS = PaymentBrowserAuthContract.Args(
            objectId = "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
            requestCode = 50000,
            clientSecret = "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
            url = "https://bank.com",
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }
}
