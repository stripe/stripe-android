package com.stripe.android.payments

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import kotlin.properties.Delegates

internal class StripeBrowserLauncherViewModel(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    private val browserCapabilities: BrowserCapabilities,
    private val intentChooserTitle: String,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var hasLaunched: Boolean by Delegates.observable(
        savedStateHandle.contains(KEY_HAS_LAUNCHED)
    ) { _, _, newValue ->
        savedStateHandle.set(KEY_HAS_LAUNCHED, true)
    }

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
                stripeAccountId = args.stripeAccountId,
                canCancelSource = args.shouldCancelSource
            ).toBundle()
        )
    }

    fun logCapabilities(
        shouldUseCustomTabs: Boolean
    ) {
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(
                when (shouldUseCustomTabs) {
                    true -> PaymentAnalyticsEvent.AuthWithCustomTabs
                    false -> PaymentAnalyticsEvent.AuthWithDefaultBrowser
                }
            )
        )
    }

    class Factory(
        private val application: Application,
        owner: SavedStateRegistryOwner
    ) : AbstractSavedStateViewModelFactory(owner, null) {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            val config = PaymentConfiguration.getInstance(application)
            val browserCapabilitiesSupplier = BrowserCapabilitiesSupplier(application)

            return StripeBrowserLauncherViewModel(
                DefaultAnalyticsRequestExecutor(),
                PaymentAnalyticsRequestFactory(
                    application,
                    config.publishableKey
                ),
                browserCapabilitiesSupplier.get(),
                application.getString(R.string.stripe_verify_your_payment),
                handle
            ) as T
        }
    }

    internal companion object {
        const val KEY_HAS_LAUNCHED = "has_launched"
    }
}
