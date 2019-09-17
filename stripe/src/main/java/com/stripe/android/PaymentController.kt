package com.stripe.android

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.VisibleForTesting
import com.stripe.android.exception.StripeException
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.Stripe3ds2Fingerprint
import com.stripe.android.model.Stripe3dsRedirect
import com.stripe.android.model.StripeIntent
import com.stripe.android.stripe3ds2.init.StripeConfigParameters
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.service.StripeThreeDs2ServiceImpl
import com.stripe.android.stripe3ds2.transaction.CompletionEvent
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent
import com.stripe.android.stripe3ds2.transaction.StripeChallengeParameters
import com.stripe.android.stripe3ds2.transaction.StripeChallengeStatusReceiver
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.stripe3ds2.views.ChallengeProgressDialogActivity
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.StripeIntentResultExtras
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit

/**
 * A controller responsible for confirming and authenticating payment (typically through resolving
 * any required customer action). The payment authentication mechanism (e.g. 3DS) will be determined
 * by the [PaymentIntent] or [SetupIntent] object.
 */
internal open class PaymentController @VisibleForTesting constructor(
    context: Context,
    private val stripeRepository: StripeRepository,
    private val threeDs2Service: StripeThreeDs2Service =
        StripeThreeDs2ServiceImpl(context, StripeSSLSocketFactory()),
    private val messageVersionRegistry: MessageVersionRegistry =
        MessageVersionRegistry(),
    private val config: PaymentAuthConfig =
        PaymentAuthConfig.get(),
    private val analyticsRequestExecutor: FireAndForgetRequestExecutor =
        StripeFireAndForgetRequestExecutor(),
    private val analyticsDataFactory: AnalyticsDataFactory =
        AnalyticsDataFactory.create(context.applicationContext),
    private val challengeFlowStarter: ChallengeFlowStarter =
        ChallengeFlowStarterImpl()
) {
    init {
        threeDs2Service.initialize(
            context,
            StripeConfigParameters(),
            null,
            config.stripe3ds2Config.uiCustomization.uiCustomization
        )
    }

    /**
     * Confirm the Stripe Intent and resolve any next actions
     */
    open fun startConfirmAndAuth(
        host: AuthActivityStarter.Host,
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        requestOptions: ApiRequest.Options
    ) {
        ConfirmStripeIntentTask(stripeRepository, confirmStripeIntentParams, requestOptions,
            ConfirmStripeIntentCallback(host, requestOptions, this,
                getRequestCode(confirmStripeIntentParams)))
            .execute()
    }

    open fun startAuth(
        host: AuthActivityStarter.Host,
        clientSecret: String,
        requestOptions: ApiRequest.Options
    ) {
        RetrieveIntentTask(stripeRepository,
            clientSecret,
            requestOptions,
            object : ApiResultCallback<StripeIntent> {
                override fun onSuccess(stripeIntent: StripeIntent) {
                    handleNextAction(host, stripeIntent, requestOptions)
                }

                override fun onError(e: Exception) {
                    handleError(host, PAYMENT_REQUEST_CODE, e)
                }
            })
            .execute()
    }

    /**
     * Decide whether [.handlePaymentResult]
     * should be called.
     */
    open fun shouldHandlePaymentResult(requestCode: Int, data: Intent?): Boolean {
        return requestCode == PAYMENT_REQUEST_CODE && data != null
    }

    /**
     * Decide whether [.handleSetupResult]
     * should be called.
     */
    open fun shouldHandleSetupResult(requestCode: Int, data: Intent?): Boolean {
        return requestCode == SETUP_REQUEST_CODE && data != null
    }

    /**
     * If payment authentication triggered an exception, get the exception object and pass to
     * [ApiResultCallback.onError].
     *
     * Otherwise, get the PaymentIntent's client_secret from {@param data} and use to retrieve
     * the PaymentIntent object with updated status.
     *
     * @param data the result Intent
     */
    open fun handlePaymentResult(
        data: Intent,
        requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<PaymentIntentResult>
    ) {
        val authException =
            data.getSerializableExtra(StripeIntentResultExtras.AUTH_EXCEPTION)
        if (authException is Exception) {
            callback.onError(authException)
            return
        }

        @StripeIntentResult.Outcome val flowOutcome = data
            .getIntExtra(StripeIntentResultExtras.FLOW_OUTCOME, StripeIntentResult.Outcome.UNKNOWN)
        RetrieveIntentTask(stripeRepository, getClientSecret(data), requestOptions,
            object : ApiResultCallback<StripeIntent> {
                override fun onSuccess(stripeIntent: StripeIntent) {
                    if (stripeIntent is PaymentIntent) {
                        callback.onSuccess(PaymentIntentResult.Builder()
                            .setPaymentIntent(stripeIntent)
                            .setOutcome(flowOutcome)
                            .build())
                    }
                }

                override fun onError(e: Exception) {
                    callback.onError(e)
                }
            })
            .execute()
    }

    /**
     * If setup authentication triggered an exception, get the exception object and pass to
     * [ApiResultCallback.onError].
     *
     * Otherwise, get the SetupIntent's client_secret from {@param data} and use to retrieve the
     * SetupIntent object with updated status.
     *
     * @param data the result Intent
     */
    open fun handleSetupResult(
        data: Intent,
        requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<SetupIntentResult>
    ) {
        val authException =
            data.getSerializableExtra(StripeIntentResultExtras.AUTH_EXCEPTION)
        if (authException is Exception) {
            callback.onError(authException)
            return
        }

        @StripeIntentResult.Outcome val flowOutcome = data
            .getIntExtra(StripeIntentResultExtras.FLOW_OUTCOME, StripeIntentResult.Outcome.UNKNOWN)

        RetrieveIntentTask(stripeRepository, getClientSecret(data), requestOptions,
            object : ApiResultCallback<StripeIntent> {
                override fun onSuccess(stripeIntent: StripeIntent) {
                    if (stripeIntent is SetupIntent) {
                        callback.onSuccess(SetupIntentResult.Builder()
                            .setSetupIntent(stripeIntent)
                            .setOutcome(flowOutcome)
                            .build())
                    }
                }

                override fun onError(e: Exception) {
                    callback.onError(e)
                }
            })
            .execute()
    }

    @VisibleForTesting
    fun getClientSecret(data: Intent): String {
        return data.getStringExtra(StripeIntentResultExtras.CLIENT_SECRET)
    }

    /**
     * Determine which authentication mechanism should be used, or bypass authentication
     * if it is not needed.
     */
    @VisibleForTesting
    fun handleNextAction(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        if (stripeIntent.requiresAction()) {
            when (stripeIntent.nextActionType) {
                StripeIntent.NextActionType.UseStripeSdk -> {
                    val sdkData = stripeIntent.stripeSdkData
                    when {
                        sdkData?.is3ds2 == true -> {
                            analyticsRequestExecutor.executeAsync(
                                AnalyticsRequest.create(
                                    analyticsDataFactory.createAuthParams(
                                        AnalyticsDataFactory.EventName.AUTH_3DS2_FINGERPRINT,
                                        stripeIntent.id.orEmpty(),
                                        requestOptions.apiKey
                                    ),
                                    requestOptions
                                )
                            )
                            try {
                                begin3ds2Auth(host, stripeIntent,
                                    Stripe3ds2Fingerprint.create(sdkData),
                                    requestOptions)
                            } catch (e: CertificateException) {
                                handleError(host, getRequestCode(stripeIntent), e)
                            }
                        }
                        sdkData?.is3ds1 == true -> beginWebAuth(host, getRequestCode(stripeIntent),
                            stripeIntent.clientSecret ?: "",
                            Stripe3dsRedirect.create(sdkData).url, null)
                        else -> // authentication type is not supported
                            bypassAuth(host, stripeIntent)
                    }
                }
                StripeIntent.NextActionType.RedirectToUrl -> {
                    analyticsRequestExecutor.executeAsync(
                        AnalyticsRequest.create(
                            analyticsDataFactory.createAuthParams(
                                AnalyticsDataFactory.EventName.AUTH_REDIRECT,
                                stripeIntent.id.orEmpty(),
                                requestOptions.apiKey),
                            requestOptions
                        )
                    )

                    val redirectData = stripeIntent.redirectData
                    beginWebAuth(host, getRequestCode(stripeIntent),
                        stripeIntent.clientSecret ?: "",
                        redirectData?.url.toString(),
                        redirectData?.returnUrl)
                }
                else -> // next action type is not supported, so bypass authentication
                    bypassAuth(host, stripeIntent)
            }
        } else {
            // no action required, so bypass authentication
            bypassAuth(host, stripeIntent)
        }
    }

    private fun bypassAuth(host: AuthActivityStarter.Host, stripeIntent: StripeIntent) {
        PaymentRelayStarter(host, getRequestCode(stripeIntent))
            .start(PaymentRelayStarter.Data.create(stripeIntent))
    }

    private fun begin3ds2Auth(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        stripe3ds2Fingerprint: Stripe3ds2Fingerprint,
        requestOptions: ApiRequest.Options
    ) {
        val activity = host.activity ?: return

        val transaction = threeDs2Service.createTransaction(
            stripe3ds2Fingerprint.directoryServer.id,
            messageVersionRegistry.current, stripeIntent.isLiveMode,
            stripe3ds2Fingerprint.directoryServer.networkName,
            stripe3ds2Fingerprint.directoryServerEncryption.rootCerts,
            stripe3ds2Fingerprint.directoryServerEncryption.directoryServerPublicKey,
            stripe3ds2Fingerprint.directoryServerEncryption.keyId
        )

        ChallengeProgressDialogActivity.show(
            activity,
            stripe3ds2Fingerprint.directoryServer.networkName,
            false,
            config.stripe3ds2Config.uiCustomization.uiCustomization
        )

        val redirectData = stripeIntent.redirectData
        val returnUrl = redirectData?.returnUrl

        val areqParams = transaction.authenticationRequestParameters
        val timeout = config.stripe3ds2Config.timeout
        val authParams = Stripe3ds2AuthParams(
            stripe3ds2Fingerprint.source,
            areqParams.sdkAppID,
            areqParams.sdkReferenceNumber,
            areqParams.sdkTransactionID,
            areqParams.deviceData,
            areqParams.sdkEphemeralPublicKey,
            areqParams.messageVersion,
            timeout,
            returnUrl
        )
        stripeRepository.start3ds2Auth(
            authParams,
            stripeIntent.id.orEmpty(),
            requestOptions,
            Stripe3ds2AuthCallback(host, stripeRepository, transaction, timeout,
                stripeIntent, stripe3ds2Fingerprint.source, requestOptions,
                analyticsRequestExecutor, analyticsDataFactory,
                challengeFlowStarter)
        )
    }

    private class RetrieveIntentTask constructor(
        private val stripeRepository: StripeRepository,
        private val clientSecret: String,
        private val requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<StripeIntent>
    ) : ApiOperation<StripeIntent>(callback) {

        @Throws(StripeException::class)
        internal override fun getResult(): StripeIntent? {
            if (clientSecret.startsWith("pi_")) {
                return stripeRepository.retrievePaymentIntent(clientSecret, requestOptions)
            } else if (clientSecret.startsWith("seti_")) {
                return stripeRepository.retrieveSetupIntent(clientSecret, requestOptions)
            }
            return null
        }
    }

    private class ConfirmStripeIntentTask constructor(
        private val stripeRepository: StripeRepository,
        params: ConfirmStripeIntentParams,
        private val requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<StripeIntent>
    ) : ApiOperation<StripeIntent>(callback) {
        // mark this request as `use_stripe_sdk=true`
        private val params: ConfirmStripeIntentParams = params.withShouldUseStripeSdk(true)

        @Throws(StripeException::class)
        internal override fun getResult(): StripeIntent? {
            if (params is ConfirmPaymentIntentParams) {
                return stripeRepository.confirmPaymentIntent(
                    params,
                    requestOptions
                )
            } else if (params is ConfirmSetupIntentParams) {
                return stripeRepository.confirmSetupIntent(
                    params,
                    requestOptions
                )
            }
            return null
        }
    }

    private class ConfirmStripeIntentCallback constructor(
        private val host: AuthActivityStarter.Host,
        private val requestOptions: ApiRequest.Options,
        private val paymentController: PaymentController,
        private val requestCode: Int
    ) : ApiResultCallback<StripeIntent> {

        override fun onSuccess(stripeIntent: StripeIntent) {
            paymentController.handleNextAction(host, stripeIntent, requestOptions)
        }

        override fun onError(e: Exception) {
            handleError(host, requestCode, e)
        }
    }

    internal class Stripe3ds2AuthCallback @VisibleForTesting constructor(
        private val host: AuthActivityStarter.Host,
        private val stripeRepository1: StripeRepository,
        private val transaction: Transaction,
        private val maxTimeout: Int,
        private val stripeIntent: StripeIntent,
        private val sourceId: String,
        private val requestOptions: ApiRequest.Options,
        private val paymentRelayStarter: PaymentRelayStarter,
        private val analyticsRequestExecutor: FireAndForgetRequestExecutor,
        private val analyticsDataFactory: AnalyticsDataFactory,
        private val challengeFlowStarter: ChallengeFlowStarter
    ) : ApiResultCallback<Stripe3ds2AuthResult> {

        constructor(
            host: AuthActivityStarter.Host,
            stripeRepository: StripeRepository,
            transaction: Transaction,
            maxTimeout: Int,
            stripeIntent: StripeIntent,
            sourceId: String,
            requestOptions: ApiRequest.Options,
            analyticsRequestExecutor: FireAndForgetRequestExecutor,
            analyticsDataFactory: AnalyticsDataFactory,
            challengeFlowStarter: ChallengeFlowStarter
        ) :
            this(
                host, stripeRepository, transaction, maxTimeout, stripeIntent,
                sourceId, requestOptions,
                PaymentRelayStarter(host, getRequestCode(stripeIntent)),
                analyticsRequestExecutor,
                analyticsDataFactory,
                challengeFlowStarter
            )

        override fun onSuccess(result: Stripe3ds2AuthResult) {
            val ares = result.ares
            if (ares != null) {
                if (ares.shouldChallenge()) {
                    startChallengeFlow(ares)
                } else {
                    startFrictionlessFlow()
                }
            } else if (result.fallbackRedirectUrl != null) {
                beginWebAuth(host, getRequestCode(stripeIntent),
                    stripeIntent.clientSecret ?: "",
                    result.fallbackRedirectUrl, null)
            } else {
                val error = result.error
                val errorMessage: String
                errorMessage = if (error != null) {
                    "Code: ${error.errorCode}, " +
                        "Detail: ${error.errorDetail}, " +
                        "Description: ${error.errorDescription}, " +
                        "Component: ${error.errorComponent}"
                } else {
                    "Invalid 3DS2 authentication response"
                }

                onError(RuntimeException(
                    "Error encountered during 3DS2 authentication request. $errorMessage"))
            }
        }

        override fun onError(e: Exception) {
            paymentRelayStarter.start(PaymentRelayStarter.Data.create(e))
        }

        private fun startFrictionlessFlow() {
            analyticsRequestExecutor.executeAsync(
                AnalyticsRequest.create(
                    analyticsDataFactory.createAuthParams(
                        AnalyticsDataFactory.EventName.AUTH_3DS2_FRICTIONLESS,
                        stripeIntent.id.orEmpty(),
                        requestOptions.apiKey),
                    requestOptions
                )
            )
            paymentRelayStarter.start(PaymentRelayStarter.Data.create(stripeIntent))
        }

        private fun startChallengeFlow(ares: Stripe3ds2AuthResult.Ares) {
            val challengeParameters = StripeChallengeParameters()
            challengeParameters.acsSignedContent = ares.acsSignedContent
            challengeParameters.set3DSServerTransactionID(ares.threeDSServerTransId)
            challengeParameters.acsTransactionID = ares.acsTransId

            challengeFlowStarter.start(Runnable {
                val activity = host.activity ?: return@Runnable
                transaction.doChallenge(activity,
                    challengeParameters,
                    PaymentAuth3ds2ChallengeStatusReceiver.create(host, stripeRepository1,
                        stripeIntent, sourceId, requestOptions,
                        analyticsRequestExecutor, analyticsDataFactory,
                        transaction),
                    maxTimeout)
            })
        }
    }

    internal class PaymentAuth3ds2ChallengeStatusReceiver(
        private val stripeRepository: StripeRepository,
        private val stripeIntent: StripeIntent,
        private val sourceId: String,
        private val requestOptions: ApiRequest.Options,
        private val analyticsRequestExecutor: FireAndForgetRequestExecutor,
        private val analyticsDataFactory: AnalyticsDataFactory,
        private val transaction: Transaction,
        private val complete3ds2AuthCallbackFactory: Complete3ds2AuthCallbackFactory
    ) : StripeChallengeStatusReceiver() {

        override fun completed(completionEvent: CompletionEvent, uiTypeCode: String) {
            super.completed(completionEvent, uiTypeCode)
            analyticsRequestExecutor.executeAsync(
                AnalyticsRequest.create(
                    analyticsDataFactory.create3ds2ChallengeParams(
                        AnalyticsDataFactory.EventName.AUTH_3DS2_CHALLENGE_COMPLETED,
                        stripeIntent.id.orEmpty(),
                        uiTypeCode,
                        requestOptions.apiKey
                    ),
                    requestOptions
                )
            )
            notifyCompletion(Stripe3ds2CompletionStarter.StartData(stripeIntent,
                if (VALUE_YES == completionEvent.transactionStatus)
                    Stripe3ds2CompletionStarter.ChallengeFlowOutcome.COMPLETE_SUCCESSFUL
                else
                    Stripe3ds2CompletionStarter.ChallengeFlowOutcome.COMPLETE_UNSUCCESSFUL
            ))
        }

        override fun cancelled(uiTypeCode: String) {
            super.cancelled(uiTypeCode)
            analyticsRequestExecutor.executeAsync(
                AnalyticsRequest.create(
                    analyticsDataFactory.create3ds2ChallengeParams(
                        AnalyticsDataFactory.EventName.AUTH_3DS2_CHALLENGE_CANCELED,
                        stripeIntent.id.orEmpty(),
                        uiTypeCode,
                        requestOptions.apiKey
                    ),
                    requestOptions
                )
            )
            notifyCompletion(Stripe3ds2CompletionStarter.StartData(stripeIntent,
                Stripe3ds2CompletionStarter.ChallengeFlowOutcome.CANCEL))
        }

        override fun timedout(uiTypeCode: String) {
            super.timedout(uiTypeCode)
            analyticsRequestExecutor.executeAsync(
                AnalyticsRequest.create(
                    analyticsDataFactory.create3ds2ChallengeParams(
                        AnalyticsDataFactory.EventName.AUTH_3DS2_CHALLENGE_TIMEDOUT,
                        stripeIntent.id.orEmpty(),
                        uiTypeCode,
                        requestOptions.apiKey
                    ),
                    requestOptions
                )
            )
            notifyCompletion(Stripe3ds2CompletionStarter.StartData(stripeIntent,
                Stripe3ds2CompletionStarter.ChallengeFlowOutcome.TIMEOUT))
        }

        override fun protocolError(protocolErrorEvent: ProtocolErrorEvent) {
            super.protocolError(protocolErrorEvent)
            analyticsRequestExecutor.executeAsync(
                AnalyticsRequest.create(
                    analyticsDataFactory.create3ds2ChallengeErrorParams(
                        stripeIntent.id.orEmpty(),
                        protocolErrorEvent,
                        requestOptions.apiKey
                    ),
                    requestOptions
                )
            )
            notifyCompletion(Stripe3ds2CompletionStarter.StartData(stripeIntent,
                Stripe3ds2CompletionStarter.ChallengeFlowOutcome.PROTOCOL_ERROR))
        }

        override fun runtimeError(runtimeErrorEvent: RuntimeErrorEvent) {
            super.runtimeError(runtimeErrorEvent)
            analyticsRequestExecutor.executeAsync(
                AnalyticsRequest.create(
                    analyticsDataFactory.create3ds2ChallengeErrorParams(
                        stripeIntent.id.orEmpty(),
                        runtimeErrorEvent,
                        requestOptions.apiKey
                    ),
                    requestOptions
                )
            )
            notifyCompletion(Stripe3ds2CompletionStarter.StartData(stripeIntent,
                Stripe3ds2CompletionStarter.ChallengeFlowOutcome.RUNTIME_ERROR))
        }

        private fun notifyCompletion(startData: Stripe3ds2CompletionStarter.StartData) {
            analyticsRequestExecutor.executeAsync(
                AnalyticsRequest.create(
                    analyticsDataFactory.create3ds2ChallengeParams(
                        AnalyticsDataFactory.EventName.AUTH_3DS2_CHALLENGE_PRESENTED,
                        stripeIntent.id.orEmpty(),
                        transaction.initialChallengeUiType.orEmpty(),
                        requestOptions.apiKey
                    ),
                    requestOptions
                )
            )

            stripeRepository.complete3ds2Auth(sourceId, requestOptions,
                complete3ds2AuthCallbackFactory.create(startData))
        }

        internal interface Complete3ds2AuthCallbackFactory :
            Factory<Stripe3ds2CompletionStarter.StartData, ApiResultCallback<Boolean>>

        companion object {
            private const val VALUE_YES = "Y"

            fun create(
                host: AuthActivityStarter.Host,
                stripeRepository: StripeRepository,
                stripeIntent: StripeIntent,
                sourceId: String,
                requestOptions: ApiRequest.Options,
                analyticsRequestExecutor: FireAndForgetRequestExecutor,
                analyticsDataFactory: AnalyticsDataFactory,
                transaction: Transaction
            ): PaymentAuth3ds2ChallengeStatusReceiver {
                return PaymentAuth3ds2ChallengeStatusReceiver(
                    stripeRepository,
                    stripeIntent,
                    sourceId,
                    requestOptions,
                    analyticsRequestExecutor,
                    analyticsDataFactory,
                    transaction,
                    createComplete3ds2AuthCallbackFactory(
                        Stripe3ds2CompletionStarter(host, getRequestCode(stripeIntent)),
                        host,
                        stripeIntent
                    )
                )
            }

            private fun createComplete3ds2AuthCallbackFactory(
                starter: Stripe3ds2CompletionStarter,
                host: AuthActivityStarter.Host,
                stripeIntent: StripeIntent
            ): Complete3ds2AuthCallbackFactory {
                return object : Complete3ds2AuthCallbackFactory {
                    override fun create(arg: Stripe3ds2CompletionStarter.StartData):
                        ApiResultCallback<Boolean> {
                        return object : ApiResultCallback<Boolean> {
                            override fun onSuccess(result: Boolean) {
                                starter.start(arg)
                            }

                            override fun onError(e: Exception) {
                                handleError(host, getRequestCode(stripeIntent), e)
                            }
                        }
                    }
                }
            }
        }
    }

    private class ChallengeFlowStarterImpl : ChallengeFlowStarter {
        private val handlerThread = HandlerThread(Stripe3ds2AuthCallback::class.java.simpleName)
        // create Handler to notifyCompletion challenge flow on background thread
        private val handler: Handler = createHandler(handlerThread)

        override fun start(runnable: Runnable) {
            handler.postDelayed({
                runnable.run()
                handlerThread.quit()
            }, TimeUnit.SECONDS.toMillis(DELAY_SECONDS))
        }

        companion object {
            private const val DELAY_SECONDS = 2L

            fun createHandler(handlerThread: HandlerThread): Handler {
                handlerThread.start()
                return Handler(handlerThread.looper)
            }
        }
    }

    internal interface ChallengeFlowStarter {
        fun start(runnable: Runnable)
    }

    companion object {
        const val PAYMENT_REQUEST_CODE = 50000
        const val SETUP_REQUEST_CODE = 50001

        /**
         * Get the appropriate request code for the given stripe intent type
         *
         * @param intent the [StripeIntent] to get the request code for
         * @return PAYMENT_REQUEST_CODE or SETUP_REQUEST_CODE
         */
        @JvmStatic
        fun getRequestCode(intent: StripeIntent): Int {
            return if (intent is PaymentIntent) {
                PAYMENT_REQUEST_CODE
            } else SETUP_REQUEST_CODE
        }

        /**
         * Get the appropriate request code for the given stripe intent params type
         *
         * @param params the [ConfirmStripeIntentParams] to get the request code for
         * @return PAYMENT_REQUEST_CODE or SETUP_REQUEST_CODE
         */
        @JvmStatic
        fun getRequestCode(params: ConfirmStripeIntentParams): Int {
            return if (params is ConfirmPaymentIntentParams) {
                PAYMENT_REQUEST_CODE
            } else SETUP_REQUEST_CODE
        }

        /**
         * Start in-app WebView activity.
         *
         * @param host the payment authentication result will be returned as a result to this view host
         */
        private fun beginWebAuth(
            host: AuthActivityStarter.Host,
            requestCode: Int,
            clientSecret: String,
            authUrl: String,
            returnUrl: String?
        ) {
            PaymentAuthWebViewStarter(host, requestCode).start(
                PaymentAuthWebViewStarter.Data(clientSecret, authUrl, returnUrl))
        }

        private fun handleError(
            host: AuthActivityStarter.Host,
            requestCode: Int,
            exception: Exception
        ) {
            PaymentRelayStarter(host, requestCode)
                .start(PaymentRelayStarter.Data.create(exception))
        }

        @JvmStatic
        fun create(context: Context, stripeRepository: StripeRepository): PaymentController {
            return PaymentController(context.applicationContext, stripeRepository)
        }
    }
}
