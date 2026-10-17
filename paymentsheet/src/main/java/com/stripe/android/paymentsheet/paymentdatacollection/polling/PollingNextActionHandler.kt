package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityOptionsCompat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.utils.AnimationConstants
import com.stripe.android.view.AuthActivityStarterHost

private const val BLIK_TIME_LIMIT_IN_SECONDS = 60
private const val BLIK_INITIAL_DELAY_IN_SECONDS = 5
private const val PAYNOW_TIME_LIMIT_IN_SECONDS = 60 * 60
private const val PAYNOW_INITIAL_DELAY_IN_SECONDS = 5
private const val PROMPTPAY_TIME_LIMIT_IN_SECONDS = 60 * 60
private const val PROMPTPAY_INITIAL_DELAY_IN_SECONDS = 5
private const val PIX_DEFAULT_TIME_LIMIT_IN_SECONDS = 24 * 60 * 60
private const val PIX_INITIAL_DELAY_IN_SECONDS = 0
private const val PIX_POLLING_INTERVAL_IN_SECONDS = 2
private const val BIZUM_TIME_LIMIT_IN_SECONDS = 70 * 60
private const val BIZUM_INITIAL_DELAY_IN_SECONDS = 5
private const val MB_WAY_TIME_LIMIT_IN_SECONDS = 4 * 60
private const val MB_WAY_INITIAL_DELAY_IN_SECONDS = 5
private const val DEFAULT_POLLING_INTERVAL_IN_SECONDS = 1
private const val MILLIS_PER_SECOND = 1000L

private data class DefaultPollingConfig(
    val timeLimitInSeconds: Int,
    val initialDelayInSeconds: Int,
    val ctaText: Int,
    val qrCodeUrl: String?,
)

internal class PollingNextActionHandler : PaymentNextActionHandler<StripeIntent>() {

    private var pollingLauncher: ActivityResultLauncher<PollingContract.Args>? = null

    override suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val args = getArgsForPaymentMethod(
            actionable = actionable,
            statusBarColor = host.statusBarColor,
            requestOptions = requestOptions,
            currentTimeMillis = System.currentTimeMillis(),
        )

        val options = ActivityOptionsCompat.makeCustomAnimation(
            host.application.applicationContext,
            AnimationConstants.FADE_IN,
            AnimationConstants.FADE_OUT,
        )

        val localPollingAuthenticator = pollingLauncher
        if (localPollingAuthenticator == null) {
            ErrorReporter.createFallbackInstance(
                context = host.application,
                apiConfigurationProvider = {
                    ApiConfiguration.State(
                        publishableKey = requestOptions.apiKey,
                        stripeAccountId = requestOptions.stripeAccount,
                    )
                },
            )
                .report(ErrorReporter.UnexpectedErrorEvent.MISSING_POLLING_AUTHENTICATOR)
        } else {
            localPollingAuthenticator.launch(args, options)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getArgsForPaymentMethod(
        actionable: StripeIntent,
        statusBarColor: Int?,
        requestOptions: ApiRequest.Options,
        currentTimeMillis: Long,
    ): PollingContract.Args {
        val paymentMethodType = requireNotNull(actionable.paymentMethod?.type) {
            "Received null payment method type in PollingAuthenticator"
        }

        return when (paymentMethodType) {
            PaymentMethod.Type.Pix -> getArgsForPix(actionable, statusBarColor, requestOptions, currentTimeMillis)
            else -> createArgsWithDefaultPollingInterval(
                actionable = actionable,
                statusBarColor = statusBarColor,
                requestOptions = requestOptions,
                paymentMethodType = paymentMethodType,
            )
        }
    }

    private fun createArgsWithDefaultPollingInterval(
        actionable: StripeIntent,
        statusBarColor: Int?,
        requestOptions: ApiRequest.Options,
        paymentMethodType: PaymentMethod.Type,
    ): PollingContract.Args {
        val config = getDefaultPollingConfig(actionable, paymentMethodType)

        return PollingContract.Args(
            clientSecret = requireNotNull(actionable.clientSecret),
            statusBarColor = statusBarColor,
            timeLimitInSeconds = config.timeLimitInSeconds,
            initialDelayInSeconds = config.initialDelayInSeconds,
            pollingIntervalInSeconds = DEFAULT_POLLING_INTERVAL_IN_SECONDS,
            ctaText = config.ctaText,
            requestOptions = requestOptions,
            qrCodeUrl = config.qrCodeUrl,
            paymentMethodType = paymentMethodType.code,
        )
    }

    private fun getDefaultPollingConfig(
        actionable: StripeIntent,
        paymentMethodType: PaymentMethod.Type,
    ): DefaultPollingConfig = when (paymentMethodType) {
        PaymentMethod.Type.Blik -> DefaultPollingConfig(
            timeLimitInSeconds = BLIK_TIME_LIMIT_IN_SECONDS,
            initialDelayInSeconds = BLIK_INITIAL_DELAY_IN_SECONDS,
            ctaText = R.string.stripe_blik_confirm_payment,
            qrCodeUrl = null,
        )
        PaymentMethod.Type.PayNow -> DefaultPollingConfig(
            timeLimitInSeconds = PAYNOW_TIME_LIMIT_IN_SECONDS,
            initialDelayInSeconds = PAYNOW_INITIAL_DELAY_IN_SECONDS,
            ctaText = R.string.stripe_qrcode_lpm_confirm_payment,
            qrCodeUrl = getQrCodeForPayNow(actionable),
        )
        PaymentMethod.Type.PromptPay -> DefaultPollingConfig(
            timeLimitInSeconds = PROMPTPAY_TIME_LIMIT_IN_SECONDS,
            initialDelayInSeconds = PROMPTPAY_INITIAL_DELAY_IN_SECONDS,
            ctaText = R.string.stripe_qrcode_lpm_confirm_payment,
            qrCodeUrl = getQrCodeForPromptPay(actionable),
        )
        PaymentMethod.Type.Bizum -> DefaultPollingConfig(
            timeLimitInSeconds = BIZUM_TIME_LIMIT_IN_SECONDS,
            initialDelayInSeconds = BIZUM_INITIAL_DELAY_IN_SECONDS,
            ctaText = R.string.stripe_bizum_confirm_payment,
            qrCodeUrl = null,
        )
        PaymentMethod.Type.MbWay -> DefaultPollingConfig(
            timeLimitInSeconds = MB_WAY_TIME_LIMIT_IN_SECONDS,
            initialDelayInSeconds = MB_WAY_INITIAL_DELAY_IN_SECONDS,
            ctaText = R.string.stripe_mb_way_confirm_payment,
            qrCodeUrl = null,
        )
        else -> error("Received invalid payment method type ${paymentMethodType.code} in PollingAuthenticator")
    }

    private fun getQrCodeForPayNow(actionable: StripeIntent): String {
        return requireNotNull((actionable.nextActionData as StripeIntent.NextActionData.DisplayPayNowDetails).qrCodeUrl)
    }

    private fun getQrCodeForPromptPay(actionable: StripeIntent): String {
        return requireNotNull(
            (actionable.nextActionData as StripeIntent.NextActionData.DisplayPromptPayDetails).qrCodeUrl
        )
    }

    private fun getArgsForPix(
        actionable: StripeIntent,
        statusBarColor: Int?,
        requestOptions: ApiRequest.Options,
        currentTimeMillis: Long,
    ): PollingContract.Args {
        val pixDetails = actionable.nextActionData as StripeIntent.NextActionData.DisplayPixDetails

        return PollingContract.Args(
            clientSecret = requireNotNull(actionable.clientSecret),
            statusBarColor = statusBarColor,
            timeLimitInSeconds = getTimeLimitForPix(pixDetails, currentTimeMillis),
            initialDelayInSeconds = PIX_INITIAL_DELAY_IN_SECONDS,
            pollingIntervalInSeconds = PIX_POLLING_INTERVAL_IN_SECONDS,
            ctaText = R.string.stripe_pix_confirm_payment,
            requestOptions = requestOptions,
            qrCodeUrl = pixDetails.hostedInstructionsUrl,
            paymentMethodType = PaymentMethod.Type.Pix.code,
        )
    }

    private fun getTimeLimitForPix(
        pixDetails: StripeIntent.NextActionData.DisplayPixDetails,
        currentTimeMillis: Long,
    ): Int {
        val expiresAt = pixDetails.expiresAt
            ?: return PIX_DEFAULT_TIME_LIMIT_IN_SECONDS
        val remainingMillis = (expiresAt * MILLIS_PER_SECOND - currentTimeMillis).coerceAtLeast(0L)

        return (remainingMillis / MILLIS_PER_SECOND).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
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
