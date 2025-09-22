package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.utils.AnimationConstants
import com.stripe.android.view.AuthActivityStarterHost

private const val UPI_TIME_LIMIT_IN_SECONDS = 5 * 60
private const val UPI_INITIAL_DELAY_IN_SECONDS = 5
private const val BLIK_TIME_LIMIT_IN_SECONDS = 60
private const val BLIK_INITIAL_DELAY_IN_SECONDS = 5
private const val PAYNOW_TIME_LIMIT_IN_SECONDS = 60 * 60
private const val PAYNOW_INITIAL_DELAY_IN_SECONDS = 5

internal class PollingNextActionHandler : PaymentNextActionHandler<StripeIntent>() {

    private var pollingLauncher: ActivityResultLauncher<PollingContract.Args>? = null

    override suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val args = when (actionable.paymentMethod?.type) {
            PaymentMethod.Type.Upi ->
                PollingContract.Args(
                    clientSecret = requireNotNull(actionable.clientSecret),
                    statusBarColor = host.statusBarColor,
                    timeLimitInSeconds = UPI_TIME_LIMIT_IN_SECONDS,
                    initialDelayInSeconds = UPI_INITIAL_DELAY_IN_SECONDS,
                    ctaText = R.string.stripe_upi_polling_message,
                    stripeAccountId = requestOptions.stripeAccount,
                    qrCodeUrl = null,
                )
            PaymentMethod.Type.Blik ->
                PollingContract.Args(
                    clientSecret = requireNotNull(actionable.clientSecret),
                    statusBarColor = host.statusBarColor,
                    timeLimitInSeconds = BLIK_TIME_LIMIT_IN_SECONDS,
                    initialDelayInSeconds = BLIK_INITIAL_DELAY_IN_SECONDS,
                    ctaText = R.string.stripe_blik_confirm_payment,
                    stripeAccountId = requestOptions.stripeAccount,
                    qrCodeUrl = null,
                )
            PaymentMethod.Type.PayNow ->
                PollingContract.Args(
                    clientSecret = requireNotNull(actionable.clientSecret),
                    statusBarColor = host.statusBarColor,
                    timeLimitInSeconds = PAYNOW_TIME_LIMIT_IN_SECONDS,
                    initialDelayInSeconds = PAYNOW_INITIAL_DELAY_IN_SECONDS,
                    ctaText = R.string.stripe_paynow_confirm_payment,
                    stripeAccountId = requestOptions.stripeAccount,
                    qrCodeUrl = getQrCodeForPayNow(actionable),
                )
            else ->
                error(
                    "Received invalid payment method type " +
                        "${actionable.paymentMethod?.type?.code} " +
                        "in PollingAuthenticator"
                )
        }

        val options = ActivityOptionsCompat.makeCustomAnimation(
            host.application.applicationContext,
            AnimationConstants.FADE_IN,
            AnimationConstants.FADE_OUT,
        )

        val localPollingAuthenticator = pollingLauncher
        if (localPollingAuthenticator == null) {
            ErrorReporter.createFallbackInstance(host.application)
                .report(ErrorReporter.UnexpectedErrorEvent.MISSING_POLLING_AUTHENTICATOR)
        } else {
            localPollingAuthenticator.launch(args, options)
        }
    }

    private fun getQrCodeForPayNow(actionable: StripeIntent): String {
        return requireNotNull((actionable.nextActionData as StripeIntent.NextActionData.DisplayPayNowDetails).qrCodeUrl)
    }

    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
        pollingLauncher = activityResultCaller.registerForActivityResult(
            PollingContract(),
            activityResultCallback
        )
    }

    override fun onLauncherInvalidated() {
        pollingLauncher?.unregister()
        pollingLauncher = null
    }
}
