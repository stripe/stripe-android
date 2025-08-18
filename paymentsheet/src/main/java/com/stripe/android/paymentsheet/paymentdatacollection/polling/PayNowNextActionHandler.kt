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

// TODO: find correct vals. 60 min?
private const val PAY_NOW_TIME_LIMIT_IN_SECONDS = 60 * 60
private const val PAY_NOW_INITIAL_DELAY_IN_SECONDS = 5
// TODO: probably more attempts here.
// TODO: how does our polling logic behave when we have such a long timeout?
private const val PAY_NOW_MAX_ATTEMPTS = 12

internal class PayNowNextActionHandler : PaymentNextActionHandler<StripeIntent>() {

    private var pollingLauncher: ActivityResultLauncher<PayNowContract.Args>? = null

    override suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val args = when (actionable.paymentMethod?.type) {
            PaymentMethod.Type.PayNow ->
                PayNowContract.Args(
                    clientSecret = requireNotNull(actionable.clientSecret),
                    statusBarColor = host.statusBarColor,
                    timeLimitInSeconds = PAY_NOW_TIME_LIMIT_IN_SECONDS,
                    initialDelayInSeconds = PAY_NOW_INITIAL_DELAY_IN_SECONDS,
                    maxAttempts = PAY_NOW_MAX_ATTEMPTS,
                    // TODO: add correct CTA text.
                    ctaText = R.string.stripe_blik_confirm_payment,
                    stripeAccountId = requestOptions.stripeAccount,
                    qrCodeUrl = (actionable.nextActionData as StripeIntent.NextActionData.DisplayPayNowDetails).hostedVoucherUrl!!,
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

        val localPollingLauncher = pollingLauncher
        if (localPollingLauncher == null) {
            ErrorReporter.createFallbackInstance(host.application)
                .report(ErrorReporter.UnexpectedErrorEvent.MISSING_POLLING_AUTHENTICATOR)
        } else {
            localPollingLauncher.launch(args, options)
        }
    }

    // TODO: potentially this should implement polling next action handler so that all this is shared.
    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
        pollingLauncher = activityResultCaller.registerForActivityResult(
            PayNowContract(),
            activityResultCallback
        )
    }

    override fun onLauncherInvalidated() {
        pollingLauncher?.unregister()
        pollingLauncher = null
    }

}