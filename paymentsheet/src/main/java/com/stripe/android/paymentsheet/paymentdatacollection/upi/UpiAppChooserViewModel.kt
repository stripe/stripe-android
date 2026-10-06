package com.stripe.android.paymentsheet.paymentdatacollection.upi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.R

internal class UpiAppChooserViewModel(
    private val args: UpiAppChooserContract.Args,
    private val savedStateHandle: SavedStateHandle,
    private val canResolveActivity: (Intent) -> Boolean,
    private val chooserTitle: String,
    private val noCompatibleAppMessage: String,
    private val launchFailedMessage: String,
    private val errorReporter: ErrorReporter,
) : ViewModel() {
    var hasLaunched: Boolean
        get() = savedStateHandle[KEY_HAS_LAUNCHED] ?: false
        set(value) { savedStateHandle[KEY_HAS_LAUNCHED] = value }

    val hasLeftForExternalActivity: Boolean
        get() = savedStateHandle[KEY_HAS_LEFT] ?: false

    init {
        // A new ViewModel with a persisted launch means the process was recreated. Older Android
        // versions may save state before onStop records departure. Verify when the activity resumes;
        // ordinary configuration changes retain this ViewModel and must not count as returning.
        if (hasLaunched) savedStateHandle[KEY_HAS_LEFT] = true
    }

    fun onStop(isChangingConfigurations: Boolean) {
        if (hasLaunched && !isChangingConfigurations) {
            savedStateHandle[KEY_HAS_LEFT] = true
        }
    }

    fun createLaunchIntent(): Intent {
        require(args.clientSecret.isNotBlank()) { "Missing UPI client secret" }
        val uri = Uri.parse(requireNotNull(args.mobileAuthUrl) { "Missing UPI mobile_auth_url" })
        // Demo branch only: fake bank apps must never claim the real UPI scheme.
        val isDemoUri = BuildConfig.DEBUG && uri.scheme == "stripe-upi-demo"
        require((uri.scheme == "upi" || isDemoUri) && uri.host == "pay") { "Invalid UPI mobile_auth_url" }
        val target = Intent(Intent.ACTION_VIEW, uri)
        if (!canResolveActivity(target)) {
            throw ActivityNotFoundException("No compatible UPI app available")
        }
        return Intent.createChooser(target, chooserTitle)
    }

    fun getResult(): PaymentFlowResult.Unvalidated {
        // Returning from an app is only a signal to verify the PaymentIntent, not payment success.
        return PaymentFlowResult.Unvalidated(
            clientSecret = args.clientSecret,
            flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
            stripeAccountId = args.apiConfiguration.stripeAccountId,
        )
    }

    fun getFailure(cause: Exception): PaymentFlowResult.Unvalidated {
        val event = when (cause) {
            is ActivityNotFoundException -> UpiAppChooserError.NoCompatibleApp
            is IllegalArgumentException -> UpiAppChooserError.InvalidArgs
            else -> UpiAppChooserError.LaunchFailed
        }
        val exception = LocalStripeException(
            displayMessage = if (cause is ActivityNotFoundException) noCompatibleAppMessage else launchFailedMessage,
            analyticsValue = "failedUpiAppLaunchError",
        )
        // Do not include the URI or any of its payment details in diagnostics.
        errorReporter.report(event, exception)
        return PaymentFlowResult.Unvalidated(
            clientSecret = args.clientSecret,
            flowOutcome = StripeIntentResult.Outcome.FAILED,
            exception = exception,
            stripeAccountId = args.apiConfiguration.stripeAccountId,
        )
    }

    class Factory(private val args: UpiAppChooserContract.Args) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            return UpiAppChooserViewModel(
                args = args,
                savedStateHandle = extras.createSavedStateHandle(),
                canResolveActivity = { intent ->
                    application.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                        .isNotEmpty()
                },
                chooserTitle = application.getString(R.string.stripe_upi_choose_app),
                noCompatibleAppMessage = application.getString(R.string.stripe_upi_no_compatible_apps),
                launchFailedMessage = application.getString(R.string.stripe_upi_launch_failed),
                errorReporter = ErrorReporter.createFallbackInstance(application, { args.apiConfiguration }),
            ) as T
        }
    }

    private companion object {
        const val KEY_HAS_LAUNCHED = "upi_has_launched"
        const val KEY_HAS_LEFT = "upi_has_left"
    }
}
