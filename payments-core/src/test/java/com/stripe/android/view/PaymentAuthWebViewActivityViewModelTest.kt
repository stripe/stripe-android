package com.stripe.android.view

import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentAuthWebViewActivityViewModelTest {
    private val analyticsRequests = mutableListOf<AnalyticsRequest>()
    private val analyticsRequestExecutor = AnalyticsRequestExecutor { analyticsRequests.add(it) }
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        ApplicationProvider.getApplicationContext(),
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )

    @Test
    fun cancellationResult() {
        val viewModel = createViewModel(
            ARGS.copy(
                shouldCancelSource = true
            )
        )

        val intent = viewModel.cancellationResult
        val resultIntent = PaymentFlowResult.Unvalidated.fromIntent(intent)
        assertThat(resultIntent.flowOutcome)
            .isEqualTo(StripeIntentResult.Outcome.CANCELED)
        assertThat(resultIntent.canCancelSource)
            .isTrue()
    }

    @Test
    fun `cancellationResult should set correct outcome when user nav is allowed`() {
        val viewModel = createViewModel(
            ARGS.copy(
                shouldCancelSource = true,
                shouldCancelIntentOnUserNavigation = false
            )
        )

        val intent = viewModel.cancellationResult
        val resultIntent = PaymentFlowResult.Unvalidated.fromIntent(intent)
        assertThat(resultIntent.flowOutcome)
            .isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        assertThat(resultIntent.canCancelSource)
            .isTrue()
    }

    @Test
    fun toolbarBackgroundColor_returnsCorrectValue() {
        val viewModel = createViewModel(
            ARGS.copy(
                toolbarCustomization = StripeToolbarCustomization().apply {
                    setBackgroundColor("#ffffff")
                }
            )
        )
        assertThat(viewModel.toolbarBackgroundColor)
            .isEqualTo("#ffffff")
    }

    @Test
    fun buttonText_returnsCorrectValue() {
        val viewModel = createViewModel(
            ARGS.copy(
                toolbarCustomization = StripeToolbarCustomization().apply {
                    setButtonText("close")
                }
            )
        )
        assertThat(viewModel.buttonText)
            .isEqualTo("close")
    }

    @Test
    fun toolbarTitle_returnsCorrectValue() {
        val toolbarCustomization = StripeToolbarCustomization().apply {
            setHeaderText("auth webview")
        }
        val viewModel = createViewModel(
            ARGS.copy(
                toolbarCustomization = toolbarCustomization
            )
        )
        assertThat(
            viewModel.toolbarTitle
        ).isEqualTo(
            PaymentAuthWebViewActivityViewModel.ToolbarTitleData(
                "auth webview",
                toolbarCustomization
            )
        )
    }

    @Test
    fun `logError() should fire expected event`() {
        val viewModel = createViewModel(ARGS)

        viewModel.logError()

        val params = analyticsRequests.first().params
        assertThat(params["event"])
            .isEqualTo("stripe_android.3ds1_challenge_error")
    }

    @Test
    fun `logComplete() should fire expected event`() {
        val viewModel = createViewModel(ARGS)

        viewModel.logComplete()

        val params = analyticsRequests.first().params
        assertThat(params["event"])
            .isEqualTo("stripe_android.3ds1_challenge_complete")
    }

    @Test
    fun `logComplete() with uri=null should fire expected event`() {
        val viewModel = createViewModel(ARGS)

        viewModel.logComplete()

        val params = analyticsRequests.first().params
        assertThat(params["event"])
            .isEqualTo("stripe_android.3ds1_challenge_complete")
    }

    private fun createViewModel(
        args: PaymentBrowserAuthContract.Args
    ): PaymentAuthWebViewActivityViewModel {
        return PaymentAuthWebViewActivityViewModel(
            args,
            analyticsRequestExecutor,
            analyticsRequestFactory
        )
    }

    private companion object {
        val ARGS = PaymentBrowserAuthContract.Args(
            objectId = "pi_1EceMnCRMbs6FrXfCXdF8dnx",
            requestCode = 100,
            clientSecret = "client_secret",
            url = "https://example.com",
            statusBarColor = Color.RED,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            isInstantApp = false
        )
    }
}
