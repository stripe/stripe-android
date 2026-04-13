package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import android.app.Application
import com.stripe.android.core.networking.ApiRequest
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
private const val PROMPTPAY_TIME_LIMIT_IN_SECONDS = 60 * 60
private const val PROMPTPAY_INITIAL_DELAY_IN_SECONDS = 5

internal class PollingNextActionHandler(
    private val errorReporterProvider: (Application) -> ErrorReporter = ErrorReporter::createFallbackInstance,
) : PaymentNextActionHandler<StripeIntent>() {

    private var pollingLauncher: ActivityResultLauncher<PollingContract.Args>? = null

    override suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val args = getPollingArgs(actionable, host, requestOptions) ?: return

        val options = ActivityOptionsCompat.makeCustomAnimation(
            host.application.applicationContext,
            AnimationConstants.FADE_IN,
            AnimationConstants.FADE_OUT,
        )

        val localPollingAuthenticator = pollingLauncher
        if (localPollingAuthenticator == null) {
            errorReporterProvider(host.application)
                .report(ErrorReporter.UnexpectedErrorEvent.MISSING_POLLING_AUTHENTICATOR)
        } else {
            localPollingAuthenticator.launch(args, options)
        }
    }

    private fun getPollingArgs(
        actionable: StripeIntent,
        host: AuthActivityStarterHost,
        requestOptions: ApiRequest.Options
    ): PollingContract.Args? = when (val nextAction = actionable.nextActionData) {
        is StripeIntent.NextActionData.UpiAwaitNotification ->
            PollingContract.Args(
                clientSecret = requireNotNull(actionable.clientSecret),
                statusBarColor = host.statusBarColor,
                timeLimitInSeconds = UPI_TIME_LIMIT_IN_SECONDS,
                initialDelayInSeconds = UPI_INITIAL_DELAY_IN_SECONDS,
                ctaText = R.string.stripe_upi_polling_message,
                stripeAccountId = requestOptions.stripeAccount,
                qrCodeUrl = null,
            )
        is StripeIntent.NextActionData.BlikAuthorize ->
            PollingContract.Args(
                clientSecret = requireNotNull(actionable.clientSecret),
                statusBarColor = host.statusBarColor,
                timeLimitInSeconds = BLIK_TIME_LIMIT_IN_SECONDS,
                initialDelayInSeconds = BLIK_INITIAL_DELAY_IN_SECONDS,
                ctaText = R.string.stripe_blik_confirm_payment,
                stripeAccountId = requestOptions.stripeAccount,
                qrCodeUrl = null,
            )
        is StripeIntent.NextActionData.DisplayPayNowDetails ->
            PollingContract.Args(
                clientSecret = requireNotNull(actionable.clientSecret),
                statusBarColor = host.statusBarColor,
                timeLimitInSeconds = PAYNOW_TIME_LIMIT_IN_SECONDS,
                initialDelayInSeconds = PAYNOW_INITIAL_DELAY_IN_SECONDS,
                ctaText = R.string.stripe_qrcode_lpm_confirm_payment,
                stripeAccountId = requestOptions.stripeAccount,
                qrCodeUrl = nextAction.qrCodeUrl,
            )
        is StripeIntent.NextActionData.DisplayPromptPayDetails ->
            PollingContract.Args(
                clientSecret = requireNotNull(actionable.clientSecret),
                statusBarColor = host.statusBarColor,
                timeLimitInSeconds = PROMPTPAY_TIME_LIMIT_IN_SECONDS,
                initialDelayInSeconds = PROMPTPAY_INITIAL_DELAY_IN_SECONDS,
                ctaText = R.string.stripe_qrcode_lpm_confirm_payment,
                stripeAccountId = requestOptions.stripeAccount,
                qrCodeUrl = nextAction.qrCodeUrl,
            )
        else -> {
            errorReporterProvider(host.application)
                .report(
                    ErrorReporter.UnexpectedErrorEvent.POLLING_NEXT_ACTION_INVALID_NEXT_ACTION_TYPE,
                    additionalNonPiiParams = mapOf(
                        "next_action_type" to (actionable.nextActionData?.let {
                            it::class.java.simpleName
                        }.orEmpty()),
                        "payment_method_type" to actionable.paymentMethod?.type?.code.orEmpty()
                    )
                )
            null
        }
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
