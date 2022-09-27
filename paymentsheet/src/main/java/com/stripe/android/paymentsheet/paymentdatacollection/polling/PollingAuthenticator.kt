package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Singleton

@Singleton
internal class PollingAuthenticator : PaymentAuthenticator<StripeIntent> {

    private var pollingLauncher: ActivityResultLauncher<PollingContract.Args>? = null

    override suspend fun authenticate(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val args = PollingContract.Args(
            clientSecret = requireNotNull(authenticatable.clientSecret)
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
}
