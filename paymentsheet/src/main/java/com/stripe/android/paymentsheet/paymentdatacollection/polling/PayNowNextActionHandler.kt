package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.payments.core.authentication.WebIntentNextActionHandler
import com.stripe.android.paymentsheet.R
import com.stripe.android.view.AuthActivityStarterHost

// TODO: find correct vals. 60 min?
private const val PAY_NOW_TIME_LIMIT_IN_SECONDS = 60 * 60
private const val PAY_NOW_INITIAL_DELAY_IN_SECONDS = 5
// TODO: probably more attempts here.
// TODO: how does our polling logic behave when we have such a long timeout?
private const val PAY_NOW_MAX_ATTEMPTS = 12

internal class PayNowNextActionHandler : PaymentNextActionHandler<StripeIntent>() {

    private var pollingLauncher: ActivityResultLauncher<PollingContract.Args>? = null
    private var webIntentAuthenticator: WebIntentNextActionHandler? = null

    override suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val args = when (actionable.paymentMethod?.type) {
            PaymentMethod.Type.PayNow ->
                PollingContract.Args(
                    clientSecret = requireNotNull(actionable.clientSecret),
                    statusBarColor = host.statusBarColor,
                    timeLimitInSeconds = PAY_NOW_TIME_LIMIT_IN_SECONDS,
                    initialDelayInSeconds = PAY_NOW_INITIAL_DELAY_IN_SECONDS,
                    maxAttempts = PAY_NOW_MAX_ATTEMPTS,
                    // TODO: add correct CTA text.
                    ctaText = R.string.stripe_blik_confirm_payment,
                    stripeAccountId = requestOptions.stripeAccount,
                )
            else ->
                error(
                    "Received invalid payment method type " +
                        "${actionable.paymentMethod?.type?.code} " +
                        "in PollingAuthenticator"
                )
        }

        PollingUtils.launchPollingAuthenticator(
            pollingLauncher,
            host,
            args,
        )

        webIntentAuthenticator?.performNextAction(
            host,
            actionable,
            requestOptions,
        )
    }

    // TODO: potentially this should implement polling next action handler so that all this is shared.
    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>,
        webIntentNextActionHandler: WebIntentNextActionHandler?
    ) {
        pollingLauncher = activityResultCaller.registerForActivityResult(
            PollingContract(),
            activityResultCallback
        )

        webIntentAuthenticator = webIntentNextActionHandler
    }

    override fun onLauncherInvalidated() {
        pollingLauncher?.unregister()
        pollingLauncher = null
    }

}