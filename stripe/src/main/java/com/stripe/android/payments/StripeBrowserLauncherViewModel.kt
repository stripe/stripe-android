package com.stripe.android.payments

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory

internal class StripeBrowserLauncherViewModel(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val browserCapabilities: BrowserCapabilities,
    private val intentChooserTitle: String
) : ViewModel() {

    fun createLaunchIntent(
        args: PaymentBrowserAuthContract.Args
    ): Intent {
        val shouldUseCustomTabs = browserCapabilities == BrowserCapabilities.CustomTabs
        logCapabilities(shouldUseCustomTabs)

        val url = Uri.parse(args.url)
        return if (shouldUseCustomTabs) {
            val customTabColorSchemeParams = args.statusBarColor?.let { statusBarColor ->
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(statusBarColor)
                    .build()
            }

            // use Custom Tabs
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .also {
                    if (customTabColorSchemeParams != null) {
                        it.setDefaultColorSchemeParams(customTabColorSchemeParams)
                    }
                }
                .build()
            customTabsIntent.intent.data = url

            Intent.createChooser(
                customTabsIntent.intent,
                intentChooserTitle
            )
        } else {
            // use default device browser
            Intent.createChooser(
                Intent(Intent.ACTION_VIEW, url),
                intentChooserTitle
            )
        }
    }

    fun getResultIntent(args: PaymentBrowserAuthContract.Args): Intent {
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
            val browserCapabilitiesSupplier = BrowserCapabilitiesSupplier(application)

            return StripeBrowserLauncherViewModel(
                AnalyticsRequestExecutor.Default(),
                AnalyticsRequestFactory(
                    application,
                    config.publishableKey
                ),
                browserCapabilitiesSupplier.get(),
                application.getString(R.string.stripe_verify_your_payment)
            ) as T
        }
    }
}
