package com.stripe.android.payments

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.AnalyticsEvent
import com.stripe.android.PaymentConfiguration
import com.stripe.android.auth.PaymentAuthWebViewContract
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory

internal class StripeBrowserLauncherViewModel(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory
) : ViewModel() {

    fun getResultIntent(args: PaymentAuthWebViewContract.Args): Intent {
        val url = Uri.parse(args.url)
        return Intent().putExtras(
            PaymentFlowResult.Unvalidated(
                clientSecret = args.clientSecret,
                sourceId = url.lastPathSegment.orEmpty(),
                stripeAccountId = args.stripeAccountId
            ).toBundle()
        )
    }

    fun logCapabilities(
        shouldUseCustomTabs: Boolean
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                when (shouldUseCustomTabs) {
                    true -> AnalyticsEvent.AuthWithCustomTabs
                    false -> AnalyticsEvent.AuthWithDefaultBrowser
                }
            )
        )
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val config = PaymentConfiguration.getInstance(application)

            return StripeBrowserLauncherViewModel(
                AnalyticsRequestExecutor.Default(),
                AnalyticsRequestFactory(
                    application,
                    config.publishableKey
                )
            ) as T
        }
    }
}
