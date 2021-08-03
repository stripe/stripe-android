package com.stripe.android.view

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.StripeClientUserAgentHeaderFactory
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization

internal class PaymentAuthWebViewActivityViewModel(
    private val args: PaymentBrowserAuthContract.Args,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory
) : ViewModel() {
    val extraHeaders: Map<String, String> by lazy {
        StripeClientUserAgentHeaderFactory().create(Stripe.appInfo)
    }

    @JvmSynthetic
    internal val buttonText = args.toolbarCustomization?.let { toolbarCustomization ->
        toolbarCustomization.buttonText.takeUnless { it.isNullOrBlank() }
    }

    @JvmSynthetic
    internal val toolbarTitle = args.toolbarCustomization?.let { toolbarCustomization ->
        toolbarCustomization.headerText.takeUnless { it.isNullOrBlank() }?.let {
            ToolbarTitleData(it, toolbarCustomization)
        }
    }

    @JvmSynthetic
    internal val toolbarBackgroundColor = args.toolbarCustomization?.backgroundColor

    internal val paymentResult: PaymentFlowResult.Unvalidated
        @JvmSynthetic
        get() {
            return PaymentFlowResult.Unvalidated(
                clientSecret = args.clientSecret,
                sourceId = Uri.parse(args.url).lastPathSegment.orEmpty(),
                stripeAccountId = args.stripeAccountId
            )
        }

    internal val cancellationResult: Intent
        @JvmSynthetic
        get() {
            return Intent().putExtras(
                paymentResult.copy(
                    flowOutcome = if (args.shouldCancelIntentOnUserNavigation) {
                        StripeIntentResult.Outcome.CANCELED
                    } else {
                        StripeIntentResult.Outcome.SUCCEEDED
                    },
                    canCancelSource = args.shouldCancelSource
                ).toBundle()
            )
        }

    /**
     * Log that 3DS1 challenge started.
     */
    fun logStart() {
        fireAnalytics(
            analyticsRequestFactory.createRequest(AnalyticsEvent.Auth3ds1ChallengeStart)
        )

        fireAnalytics(
            analyticsRequestFactory.createRequest(
                AnalyticsEvent.AuthWithWebView
            )
        )
    }

    /**
     * Log that 3DS1 challenge completed with an error.
     */
    fun logError() {
        fireAnalytics(
            analyticsRequestFactory.createRequest(AnalyticsEvent.Auth3ds1ChallengeError)
        )
    }

    /**
     * Log that 3DS1 challenge completed without an error.
     */
    fun logComplete() {
        fireAnalytics(
            analyticsRequestFactory.createRequest(AnalyticsEvent.Auth3ds1ChallengeComplete)
        )
    }

    private fun fireAnalytics(
        request: AnalyticsRequest
    ) {
        analyticsRequestExecutor.executeAsync(request)
    }

    internal data class ToolbarTitleData(
        internal val text: String,
        internal val toolbarCustomization: StripeToolbarCustomization
    )

    internal class Factory(
        private val application: Application,
        private val logger: Logger,
        private val args: PaymentBrowserAuthContract.Args
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val publishableKey = PaymentConfiguration.getInstance(application).publishableKey

            return PaymentAuthWebViewActivityViewModel(
                args,
                DefaultAnalyticsRequestExecutor(logger),
                AnalyticsRequestFactory(
                    application,
                    publishableKey,
                    defaultProductUsageTokens = setOf("PaymentAuthWebViewActivity")
                )
            ) as T
        }
    }
}
