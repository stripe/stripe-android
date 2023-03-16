package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Singleton

private const val UPI_TIME_LIMIT_IN_SECONDS = 5 * 60
private const val UPI_INITIAL_DELAY_IN_SECONDS = 5
private const val UPI_MAX_ATTEMPTS = 12

@Singleton
internal class PollingAuthenticator : PaymentAuthenticator<StripeIntent>() {

    private var pollingLauncher: ActivityResultLauncher<PollingContract.Args>? = null

    override suspend fun performAuthentication(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        confirmParams: ConfirmStripeIntentParams?,
        requestOptions: ApiRequest.Options,
    ) {
        val args = PollingContract.Args(
            clientSecret = requireNotNull(authenticatable.clientSecret),
            statusBarColor = host.statusBarColor,
            timeLimitInSeconds = UPI_TIME_LIMIT_IN_SECONDS,
            initialDelayInSeconds = UPI_INITIAL_DELAY_IN_SECONDS,
            maxAttempts = UPI_MAX_ATTEMPTS,
        )
        pollingLauncher?.launch(args)
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
