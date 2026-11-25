package com.stripe.android.paymentsheet.paymentdatacollection.upi

import android.content.Context
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.view.AuthActivityStarterHost

internal class UpiNextActionHandler(
    private val errorReporterFactory: (Context, ApiConfiguration.State) -> ErrorReporter,
) : PaymentNextActionHandler<StripeIntent>() {
    private var launcher: ActivityResultLauncher<UpiAppChooserContract.Args>? = null

    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>,
    ) {
        launcher?.unregister()
        launcher = activityResultCaller.registerForActivityResult(UpiAppChooserContract(), activityResultCallback)
    }

    override fun onLauncherInvalidated() {
        launcher?.unregister()
        launcher = null
    }

    override suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        requestOptions: ApiRequest.Options,
    ) {
        val apiConfiguration = ApiConfiguration.State(
            publishableKey = requestOptions.apiKey,
            stripeAccountId = requestOptions.stripeAccount,
        )
        val registeredLauncher = launcher
        if (registeredLauncher == null) {
            errorReporterFactory(host.application, apiConfiguration).report(UpiAppChooserError.MissingLauncher)
            return
        }
        registeredLauncher.launch(
            UpiAppChooserContract.Args(
                clientSecret = actionable.clientSecret.orEmpty(),
                mobileAuthUrl = (actionable.nextActionData as? StripeIntent.NextActionData.UpiRedirect)?.mobileAuthUrl,
                apiConfiguration = apiConfiguration,
            )
        )
    }
}
