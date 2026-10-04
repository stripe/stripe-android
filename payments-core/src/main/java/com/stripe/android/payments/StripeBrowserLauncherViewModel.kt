package com.stripe.android.payments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
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
    private val customTabsPackage: String?,
    private val resolveErrorMessage: String,
    private val savedStateHandle: SavedStateHandle,
    private val canResolveActivity: (Intent) -> Boolean,
    private val appChooserTitle: String,
    private val noCompatibleAppMessage: String,
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
        if (args.shouldUseAppChooser) {
            require(url.scheme == "upi" && url.host == "pay") { "Invalid UPI mobile_auth_url" }
            val target = Intent(Intent.ACTION_VIEW, url)
            if (!canResolveActivity(target)) {
                throw ActivityNotFoundException("No compatible app available")
            }
            return Intent.createChooser(target, appChooserTitle)
        }
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

        return intent
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
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .also {
                if (customTabColorSchemeParams != null) {
                    it.setDefaultColorSchemeParams(customTabColorSchemeParams)
                }
            }
            .build()
            .apply {
                intent.data = url
                customTabsPackage?.let { intent.setPackage(it) }
            }
    }

    fun getResultIntent(args: PaymentBrowserAuthContract.Args): Intent {
        val url = Uri.parse(args.url)
        return Intent().putExtras(
            PaymentFlowResult.Unvalidated(
                clientSecret = args.clientSecret,
                sourceId = url.lastPathSegment.orEmpty(),
                stripeAccountId = args.apiConfiguration.stripeAccountId,
                canCancelSource = args.shouldCancelSource
            ).toBundle()
        )
    }

    fun getFailureIntent(args: PaymentBrowserAuthContract.Args, cause: Exception): Intent {
        val url = Uri.parse(args.url)
        val noCompatibleApp = args.shouldUseAppChooser && cause is ActivityNotFoundException
        val exception = LocalStripeException(
            displayMessage = if (noCompatibleApp) noCompatibleAppMessage else resolveErrorMessage,
            analyticsValue = if (args.shouldUseAppChooser) "failedUpiAppLaunchError" else "failedBrowserLaunchError",
        )

        return Intent().putExtras(
            PaymentFlowResult.Unvalidated(
                clientSecret = args.clientSecret,
                sourceId = url.lastPathSegment.orEmpty(),
                stripeAccountId = args.apiConfiguration.stripeAccountId,
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

    class Factory(
        private val args: PaymentBrowserAuthContract.Args,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            return StripeBrowserLauncherViewModel(
                analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(),
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = application,
                    publishableKeyProvider = { args.apiConfiguration.publishableKey },
                ),
                browserCapabilities = if (args.shouldUseAppChooser) {
                    BrowserCapabilities.Unknown
                } else {
                    BrowserCapabilitiesSupplier(application).get()
                },
                customTabsPackage = if (args.shouldUseAppChooser) {
                    null
                } else {
                    CustomTabsClient.getPackageName(application, null)
                },
                resolveErrorMessage = application.getString(R.string.stripe_failure_reason_authentication),
                savedStateHandle = savedStateHandle,
                canResolveActivity = { intent ->
                    application.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                        .isNotEmpty()
                },
                appChooserTitle = application.getString(R.string.stripe_upi_choose_app),
                noCompatibleAppMessage = application.getString(R.string.stripe_upi_no_compatible_apps),
            ) as T
        }
    }

    internal companion object {
        const val KEY_HAS_LAUNCHED = "has_launched"
    }
}
