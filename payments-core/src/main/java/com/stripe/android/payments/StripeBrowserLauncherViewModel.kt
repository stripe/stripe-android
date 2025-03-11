package com.stripe.android.payments

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.browser.BrowserCapabilities
import com.stripe.android.core.browser.BrowserCapabilitiesSupplier
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory

internal class StripeBrowserLauncherViewModel(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    private val browserCapabilities: BrowserCapabilities,
    private val intentChooserTitle: String,
    private val resolveErrorMessage: String,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    var hasLaunched: Boolean
        get() = savedStateHandle[KEY_HAS_LAUNCHED] ?: false
        set(value) {
            savedStateHandle[KEY_HAS_LAUNCHED] = value
        }

    fun createLaunchIntent(
        args: PaymentBrowserAuthContract.Args
    ): Intent {
        val url = Uri.parse(args.url)
        logBrowserCapabilities()

        val intent = when (browserCapabilities) {
            BrowserCapabilities.CustomTabs -> {
                val customTabsIntent = createCustomTabsIntent(args, url)
                customTabsIntent.intent
            }
            BrowserCapabilities.Unknown -> {
                Intent(Intent.ACTION_VIEW, url)
            }
        }

        return Intent.createChooser(intent, intentChooserTitle)
    }

    private fun createCustomTabsIntent(
        args: PaymentBrowserAuthContract.Args,
        url: Uri,
    ): CustomTabsIntent {
        val customTabColorSchemeParams = args.statusBarColor?.let { statusBarColor ->
            CustomTabColorSchemeParams.Builder()
                .setToolbarColor(statusBarColor)
                .build()
        }

        return CustomTabsIntent.Builder()
            .setSendToExternalDefaultHandlerEnabled(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .also {
                if (customTabColorSchemeParams != null) {
                    it.setDefaultColorSchemeParams(customTabColorSchemeParams)
                }
            }
            .build()
            .apply {
                intent.data = url
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

    fun getFailureIntent(args: PaymentBrowserAuthContract.Args): Intent {
        val url = Uri.parse(args.url)
        val exception = LocalStripeException(
            displayMessage = resolveErrorMessage,
            analyticsValue = "failedBrowserLaunchError",
        )

        return Intent().putExtras(
            PaymentFlowResult.Unvalidated(
                clientSecret = args.clientSecret,
                sourceId = url.lastPathSegment.orEmpty(),
                stripeAccountId = args.stripeAccountId,
                canCancelSource = args.shouldCancelSource,
                flowOutcome = StripeIntentResult.Outcome.FAILED,
                exception = exception,
            ).toBundle()
        )
    }

    private fun logBrowserCapabilities() {
        val event = when (browserCapabilities) {
            BrowserCapabilities.CustomTabs -> PaymentAnalyticsEvent.AuthWithCustomTabs
            BrowserCapabilities.Unknown -> PaymentAnalyticsEvent.AuthWithDefaultBrowser
        }
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(event)
        )
    }

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            val config = PaymentConfiguration.getInstance(application)
            val browserCapabilitiesSupplier = BrowserCapabilitiesSupplier(application)

            return StripeBrowserLauncherViewModel(
                analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(),
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = application,
                    publishableKey = config.publishableKey,
                ),
                browserCapabilities = browserCapabilitiesSupplier.get(),
                intentChooserTitle = application.getString(R.string.stripe_verify_your_payment),
                resolveErrorMessage = application.getString(R.string.stripe_failure_reason_authentication),
                savedStateHandle = savedStateHandle,
            ) as T
        }
    }

    internal companion object {
        const val KEY_HAS_LAUNCHED = "has_launched"
    }
}
