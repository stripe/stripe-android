package com.stripe.android.view

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.Stripe
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.StripeClientUserAgentHeaderFactory
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import kotlinx.coroutines.Dispatchers

internal class PaymentAuthWebViewActivityViewModel(
    private val args: PaymentBrowserAuthContract.Args,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
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
            paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.Auth3ds1ChallengeStart)
        )

        fireAnalytics(
            paymentAnalyticsRequestFactory.createRequest(
                PaymentAnalyticsEvent.AuthWithWebView
            )
        )
    }

    /**
     * Log that 3DS1 challenge completed with an error.
     */
    fun logError() {
        fireAnalytics(
            paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.Auth3ds1ChallengeError)
        )
    }

    /**
     * Log that 3DS1 challenge completed without an error.
     */
    fun logComplete() {
        fireAnalytics(
            paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.Auth3ds1ChallengeComplete)
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
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaymentAuthWebViewActivityViewModel(
                args,
                DefaultAnalyticsRequestExecutor(logger, Dispatchers.IO),
                PaymentAnalyticsRequestFactory(
                    application,
                    args.publishableKey,
                    defaultProductUsageTokens = setOf("PaymentAuthWebViewActivity")
                )
            ) as T
        }
    }
}
