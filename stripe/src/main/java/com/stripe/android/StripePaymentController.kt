package com.stripe.android

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import com.stripe.android.exception.APIException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.AlipayAuthResult
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.Source
import com.stripe.android.model.Stripe3ds2AuthParams
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.Stripe3ds2Fingerprint
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData.RedirectToUrl
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.service.StripeThreeDs2ServiceImpl
import com.stripe.android.stripe3ds2.transaction.ChallengeParameters
import com.stripe.android.stripe3ds2.transaction.CompletionEvent
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent
import com.stripe.android.stripe3ds2.transaction.Stripe3ds2ActivityStarterHost
import com.stripe.android.stripe3ds2.transaction.StripeChallengeStatusReceiver
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.stripe3ds2.views.ChallengeProgressActivity
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.Stripe3ds2CompletionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * A controller responsible for confirming and authenticating payment (typically through resolving
 * any required customer action). The payment authentication mechanism (e.g. 3DS) will be determined
 * by the [PaymentIntent] or [SetupIntent] object.
 */
internal class StripePaymentController internal constructor(
    context: Context,
    private val publishableKey: String,
    private val stripeRepository: StripeRepository,
    private val enableLogging: Boolean = false,
    private val messageVersionRegistry: MessageVersionRegistry =
        MessageVersionRegistry(),
    private val config: PaymentAuthConfig =
        PaymentAuthConfig.get(),
    private val threeDs2Service: StripeThreeDs2Service =
        StripeThreeDs2ServiceImpl(context, enableLogging),
    private val analyticsRequestExecutor: AnalyticsRequestExecutor =
        AnalyticsRequestExecutor.Default(Logger.getInstance(enableLogging)),
    private val analyticsDataFactory: AnalyticsDataFactory =
        AnalyticsDataFactory(context.applicationContext, publishableKey),
    private val challengeProgressActivityStarter: ChallengeProgressActivityStarter =
        ChallengeProgressActivityStarter.Default(),
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val resources: Resources = context.applicationContext.resources
) : PaymentController {
    private val logger = Logger.getInstance(enableLogging)
    private val analyticsRequestFactory = AnalyticsRequest.Factory(logger)

    init {
        threeDs2Service.initialize(
            config.stripe3ds2Config.uiCustomization.uiCustomization
        )
    }

    /**
     * Confirm the Stripe Intent and resolve any next actions
     */
    override fun startConfirmAndAuth(
        host: AuthActivityStarter.Host,
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        requestOptions: ApiRequest.Options
    ) {
        startConfirm(
            confirmStripeIntentParams,
            requestOptions,
            ConfirmStripeIntentCallback(
                host,
                requestOptions,
                this,
                getRequestCode(confirmStripeIntentParams)
            )
        )
    }

    override fun startConfirm(
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<StripeIntent>
    ) {
        CoroutineScope(workContext).launch {
            val result = runCatching {
                val intent = when (confirmStripeIntentParams) {
                    is ConfirmPaymentIntentParams ->
                        stripeRepository.confirmPaymentIntent(
                            // mark this request as `use_stripe_sdk=true`
                            confirmStripeIntentParams
                                .withShouldUseStripeSdk(shouldUseStripeSdk = true),
                            requestOptions,
                            expandFields = EXPAND_PAYMENT_METHOD
                        )
                    is ConfirmSetupIntentParams ->
                        stripeRepository.confirmSetupIntent(
                            // mark this request as `use_stripe_sdk=true`
                            confirmStripeIntentParams
                                .withShouldUseStripeSdk(shouldUseStripeSdk = true),
                            requestOptions,
                            expandFields = EXPAND_PAYMENT_METHOD
                        )
                    else -> error("Confirmation params must be ConfirmPaymentIntentParams or ConfirmSetupIntentParams")
                }
                requireNotNull(intent) { REQUIRED_ERROR }
            }

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { intent ->
                        callback.onSuccess(intent)
                    },
                    onFailure = { error ->
                        callback.onError(
                            when (error) {
                                is Exception -> error
                                else -> RuntimeException(error)
                            }
                        )
                    }
                )
            }
        }
    }

    override fun startAuth(
        host: AuthActivityStarter.Host,
        clientSecret: String,
        requestOptions: ApiRequest.Options
    ) {
        stripeRepository.retrieveIntent(
            clientSecret,
            requestOptions,
            callback = object : ApiResultCallback<StripeIntent> {
                override fun onSuccess(result: StripeIntent) {
                    handleNextAction(host, result, requestOptions)
                }

                override fun onError(e: Exception) {
                    handleError(host, PAYMENT_REQUEST_CODE, e)
                }
            }
        )
    }

    override fun startAuthenticateSource(
        host: AuthActivityStarter.Host,
        source: Source,
        requestOptions: ApiRequest.Options
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create(
                analyticsDataFactory.createAuthSourceParams(
                    AnalyticsEvent.AuthSourceStart,
                    source.id
                ),
                requestOptions
            )
        )

        stripeRepository.retrieveSource(
            sourceId = source.id.orEmpty(),
            clientSecret = source.clientSecret.orEmpty(),
            options = requestOptions,
            callback = object : ApiResultCallback<Source> {
                override fun onSuccess(result: Source) {
                    onSourceRetrieved(host, result, requestOptions)
                }

                override fun onError(e: Exception) {
                    handleError(host, SOURCE_REQUEST_CODE, e)
                }
            }
        )
    }

    private fun onSourceRetrieved(
        host: AuthActivityStarter.Host,
        source: Source,
        requestOptions: ApiRequest.Options
    ) {
        if (source.flow == Source.Flow.Redirect) {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.create(
                    analyticsDataFactory.createAuthSourceParams(
                        AnalyticsEvent.AuthSourceRedirect,
                        source.id
                    ),
                    requestOptions
                )
            )

            PaymentAuthWebViewStarter(
                host,
                SOURCE_REQUEST_CODE
            ).start(
                PaymentAuthWebViewStarter.Args(
                    clientSecret = source.clientSecret.orEmpty(),
                    url = source.redirect?.url.orEmpty(),
                    returnUrl = source.redirect?.returnUrl,
                    enableLogging = enableLogging,
                    stripeAccountId = requestOptions.stripeAccount
                )
            )
        } else {
            bypassAuth(host, source, requestOptions.stripeAccount)
        }
    }

    /**
     * Decide whether [handlePaymentResult] should be called.
     */
    override fun shouldHandlePaymentResult(requestCode: Int, data: Intent?): Boolean {
        return requestCode == PAYMENT_REQUEST_CODE && data != null
    }

    /**
     * Decide whether [handleSetupResult] should be called.
     */
    override fun shouldHandleSetupResult(requestCode: Int, data: Intent?): Boolean {
        return requestCode == SETUP_REQUEST_CODE && data != null
    }

    override fun shouldHandleSourceResult(requestCode: Int, data: Intent?): Boolean {
        return requestCode == SOURCE_REQUEST_CODE && data != null
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
    override fun handlePaymentResult(
        data: Intent,
        callback: ApiResultCallback<PaymentIntentResult>
    ) {
        val result = PaymentController.Result.fromIntent(data) ?: PaymentController.Result()
        val authException = result.exception
        if (authException is Exception) {
            callback.onError(authException)
            return
        }

        val shouldCancelSource = result.shouldCancelSource
        val sourceId = result.sourceId.orEmpty()
        @StripeIntentResult.Outcome val flowOutcome = result.flowOutcome

        val requestOptions = ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = result.stripeAccountId
        )

        stripeRepository.retrieveIntent(
            getClientSecret(data),
            requestOptions,
            expandFields = EXPAND_PAYMENT_METHOD,
            callback = createPaymentIntentCallback(
                requestOptions,
                flowOutcome,
                sourceId,
                shouldCancelSource,
                callback
            )
        )
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
    override fun handleSetupResult(
        data: Intent,
        callback: ApiResultCallback<SetupIntentResult>
    ) {
        val result = PaymentController.Result.fromIntent(data) ?: PaymentController.Result()
        val authException = result.exception
        if (authException is Exception) {
            callback.onError(authException)
            return
        }

        val shouldCancelSource = result.shouldCancelSource
        val sourceId = result.sourceId.orEmpty()
        @StripeIntentResult.Outcome val flowOutcome = result.flowOutcome

        val requestOptions = ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = result.stripeAccountId
        )

        stripeRepository.retrieveIntent(
            getClientSecret(data),
            requestOptions,
            expandFields = EXPAND_PAYMENT_METHOD,
            callback = createSetupIntentCallback(
                requestOptions,
                flowOutcome,
                sourceId,
                shouldCancelSource,
                callback
            )
        )
    }

    override fun handleSourceResult(
        data: Intent,
        callback: ApiResultCallback<Source>
    ) {
        val result = PaymentController.Result.fromIntent(data)
        val sourceId = result?.sourceId.orEmpty()
        val clientSecret = result?.clientSecret.orEmpty()

        val requestOptions = ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = result?.stripeAccountId
        )

        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create(
                analyticsDataFactory.createAuthSourceParams(
                    AnalyticsEvent.AuthSourceResult,
                    sourceId
                ),
                requestOptions
            )
        )

        stripeRepository.retrieveSource(sourceId, clientSecret, requestOptions, callback)
    }

    override fun authenticateAlipay(
        intent: StripeIntent,
        stripeAccountId: String?,
        authenticator: AlipayAuthenticator,
        callback: ApiResultCallback<PaymentIntentResult>
    ) {
        val requestOptions = ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId
        )
        AlipayAuthenticationTask(
            intent,
            authenticator,
            stripeRepository,
            requestOptions,
            object : ApiResultCallback<AlipayAuthResult> {
                override fun onSuccess(result: AlipayAuthResult) {
                    stripeRepository.retrieveIntent(
                        intent.clientSecret.orEmpty(),
                        requestOptions,
                        expandFields = EXPAND_PAYMENT_METHOD,
                        callback = createPaymentIntentCallback(
                            requestOptions,
                            result.outcome,
                            "",
                            false,
                            callback
                        )
                    )
                }

                override fun onError(e: Exception) {
                    callback.onError(e)
                }
            }
        ).execute()
    }

    internal class AlipayAuthenticationTask(
        private val intent: StripeIntent,
        private val authenticator: AlipayAuthenticator,
        private val apiRepository: StripeRepository,
        private val requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<AlipayAuthResult>
    ) : ApiOperation<AlipayAuthResult>(callback = callback) {
        override suspend fun getResult(): AlipayAuthResult {
            if (intent.paymentMethod?.liveMode == false) {
                throw IllegalArgumentException(
                    "Attempted to authenticate test mode " +
                        "PaymentIntent with the Alipay SDK.\n" +
                        "The Alipay SDK does not support test mode payments."
                )
            }

            val nextActionData = intent.nextActionData
            if (nextActionData is StripeIntent.NextActionData.AlipayRedirect) {
                val output =
                    authenticator.onAuthenticationRequest(nextActionData.data)
                return AlipayAuthResult(
                    when (output[RESULT_FIELD]) {
                        RESULT_CODE_SUCCESS -> {
                            nextActionData.authCompleteUrl?.let {
                                runCatching {
                                    apiRepository.retrieveObject(it, requestOptions)
                                }
                            }
                            StripeIntentResult.Outcome.SUCCEEDED
                        }
                        RESULT_CODE_FAILED -> StripeIntentResult.Outcome.FAILED
                        RESULT_CODE_CANCELLED -> StripeIntentResult.Outcome.CANCELED
                        else -> StripeIntentResult.Outcome.UNKNOWN
                    }
                )
            } else {
                throw RuntimeException("Unable to authenticate Payment Intent with Alipay SDK")
            }
        }

        companion object {
            private const val RESULT_FIELD = "resultStatus"

            // https://intl.alipay.com/docs/ac/3rdpartryqrcode/standard_4
            private const val RESULT_CODE_SUCCESS = "9000"
            private const val RESULT_CODE_CANCELLED = "6001"
            private const val RESULT_CODE_FAILED = "4000"
        }
    }

    private fun createPaymentIntentCallback(
        requestOptions: ApiRequest.Options,
        @StripeIntentResult.Outcome flowOutcome: Int,
        sourceId: String,
        shouldCancelSource: Boolean = false,
        callback: ApiResultCallback<PaymentIntentResult>
    ): ApiResultCallback<StripeIntent> {
        return object : ApiResultCallback<StripeIntent> {
            override fun onSuccess(result: StripeIntent) {
                if (result is PaymentIntent) {
                    if (shouldCancelSource && result.requiresAction()) {
                        logger.debug("Canceling source '$sourceId' for PaymentIntent")
                        stripeRepository.cancelIntent(
                            result,
                            sourceId,
                            requestOptions,
                            createPaymentIntentCallback(
                                requestOptions,
                                flowOutcome,
                                sourceId,
                                false, // don't attempt to cancel source again!
                                callback
                            )
                        )
                    } else {
                        logger.debug("Dispatching PaymentIntentResult for ${result.id}")
                        callback.onSuccess(
                            PaymentIntentResult(result, flowOutcome, getFailureMessage(result, flowOutcome))
                        )
                    }
                } else {
                    callback.onError(
                        IllegalArgumentException(
                            "Expected a PaymentIntent, received a ${result.javaClass.simpleName}"
                        )
                    )
                }
            }

            override fun onError(e: Exception) {
                callback.onError(e)
            }
        }
    }

    private fun getFailureMessage(intent: StripeIntent, @StripeIntentResult.Outcome outcome: Int): String? {
        return when {
            intent.status == StripeIntent.Status.RequiresPaymentMethod -> {
                when (intent) {
                    is PaymentIntent -> {
                        when {
                            intent.lastPaymentError?.code == PaymentIntent.Error.CODE_AUTHENTICATION_ERROR -> {
                                resources.getString(R.string.stripe_failure_reason_authentication)
                            }
                            intent.lastPaymentError?.type == PaymentIntent.Error.Type.CardError -> {
                                intent.lastPaymentError.message
                            }
                            else -> {
                                null
                            }
                        }
                    }
                    is SetupIntent -> {
                        when {
                            intent.lastSetupError?.code == SetupIntent.Error.CODE_AUTHENTICATION_ERROR -> {
                                resources.getString(R.string.stripe_failure_reason_authentication)
                            }
                            intent.lastSetupError?.type == SetupIntent.Error.Type.CardError -> {
                                intent.lastSetupError.message
                            }
                            else -> {
                                null
                            }
                        }
                    }
                    else -> null
                }
            }
            outcome == StripeIntentResult.Outcome.TIMEDOUT -> {
                resources.getString(R.string.stripe_failure_reason_timed_out)
            }
            else -> {
                null
            }
        }
    }

    private fun createSetupIntentCallback(
        requestOptions: ApiRequest.Options,
        @StripeIntentResult.Outcome flowOutcome: Int,
        sourceId: String,
        shouldCancelSource: Boolean = false,
        resultCallback: ApiResultCallback<SetupIntentResult>
    ): ApiResultCallback<StripeIntent> {
        return object : ApiResultCallback<StripeIntent> {
            override fun onSuccess(result: StripeIntent) {
                if (result is SetupIntent) {
                    if (shouldCancelSource && result.requiresAction()) {
                        logger.debug("Canceling source '$sourceId' for SetupIntent")
                        stripeRepository.cancelIntent(
                            result,
                            sourceId,
                            requestOptions,
                            createSetupIntentCallback(
                                requestOptions,
                                flowOutcome,
                                sourceId,
                                false, // don't attempt to cancel source again!
                                resultCallback
                            )
                        )
                    } else {
                        logger.debug("Dispatching SetupIntentResult for ${result.id}")
                        resultCallback.onSuccess(
                            SetupIntentResult(result, flowOutcome, getFailureMessage(result, flowOutcome))
                        )
                    }
                } else {
                    resultCallback.onError(
                        IllegalArgumentException(
                            "Expected a SetupIntent, received a ${result.javaClass.simpleName}"
                        )
                    )
                }
            }

            override fun onError(e: Exception) {
                resultCallback.onError(e)
            }
        }
    }

    /**
     * Determine which authentication mechanism should be used, or bypass authentication
     * if it is not needed.
     */
    @VisibleForTesting
    override fun handleNextAction(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        if (stripeIntent.requiresAction()) {
            when (val nextActionData = stripeIntent.nextActionData) {
                is StripeIntent.NextActionData.SdkData.Use3DS2 -> {
                    analyticsRequestExecutor.executeAsync(
                        analyticsRequestFactory.create(
                            analyticsDataFactory.createAuthParams(
                                AnalyticsEvent.Auth3ds2Fingerprint,
                                stripeIntent.id.orEmpty()
                            ),
                            requestOptions
                        )
                    )
                    try {
                        begin3ds2Auth(
                            host,
                            stripeIntent,
                            Stripe3ds2Fingerprint(nextActionData),
                            requestOptions
                        )
                    } catch (e: CertificateException) {
                        handleError(host, getRequestCode(stripeIntent), e)
                    }
                }
                is StripeIntent.NextActionData.SdkData.Use3DS1 -> {
                    analyticsRequestExecutor.executeAsync(
                        analyticsRequestFactory.create(
                            analyticsDataFactory.createAuthParams(
                                AnalyticsEvent.Auth3ds1Sdk,
                                stripeIntent.id.orEmpty()
                            ),
                            requestOptions
                        )
                    )
                    beginWebAuth(
                        host,
                        getRequestCode(stripeIntent),
                        stripeIntent.clientSecret.orEmpty(),
                        nextActionData.url,
                        requestOptions.stripeAccount,
                        enableLogging = enableLogging,
                        // 3D-Secure requires cancelling the source when the user cancels auth (AUTHN-47)
                        shouldCancelSource = true
                    )
                }
                is RedirectToUrl -> {
                    analyticsRequestExecutor.executeAsync(
                        analyticsRequestFactory.create(
                            analyticsDataFactory.createAuthParams(
                                AnalyticsEvent.AuthRedirect,
                                stripeIntent.id.orEmpty()
                            ),
                            requestOptions
                        )
                    )

                    beginWebAuth(
                        host,
                        getRequestCode(stripeIntent),
                        stripeIntent.clientSecret.orEmpty(),
                        nextActionData.url.toString(),
                        requestOptions.stripeAccount,
                        nextActionData.returnUrl,
                        enableLogging = enableLogging
                    )
                }
                /**
                 * If using the standard confirmation path, handle Alipay the same as
                 * a standard webview redirect.
                 * Alipay Native SDK use case is handled by [Stripe.confirmAlipayPayment]
                 * outside of the standard confirmation path.
                 */
                is StripeIntent.NextActionData.AlipayRedirect -> {
                    analyticsRequestExecutor.executeAsync(
                        analyticsRequestFactory.create(
                            analyticsDataFactory.createAuthParams(
                                AnalyticsEvent.AuthRedirect,
                                stripeIntent.id.orEmpty()
                            ),
                            requestOptions
                        )
                    )

                    beginWebAuth(
                        host,
                        getRequestCode(stripeIntent),
                        stripeIntent.clientSecret.orEmpty(),
                        nextActionData.webViewUrl.toString(),
                        requestOptions.stripeAccount,
                        nextActionData.returnUrl,
                        enableLogging = enableLogging
                    )
                }
                is StripeIntent.NextActionData.DisplayOxxoDetails -> {
                    // TODO(smaskell): add analytics event
                    if (nextActionData.hostedVoucherUrl != null) {
                        beginWebAuth(
                            host,
                            getRequestCode(stripeIntent),
                            stripeIntent.clientSecret.orEmpty(),
                            nextActionData.hostedVoucherUrl,
                            requestOptions.stripeAccount,
                            enableLogging = enableLogging,
                            shouldCancelIntentOnUserNavigation = false
                        )
                    } else {
                        // TODO(smaskell): Determine how to handle missing URL
                        bypassAuth(host, stripeIntent, requestOptions.stripeAccount)
                    }
                }
                else -> bypassAuth(host, stripeIntent, requestOptions.stripeAccount)
            }
        } else {
            // no action required, so bypass authentication
            bypassAuth(host, stripeIntent, requestOptions.stripeAccount)
        }
    }

    private fun bypassAuth(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        stripeAccountId: String?
    ) {
        PaymentRelayStarter.create(host, getRequestCode(stripeIntent))
            .start(PaymentRelayStarter.Args.create(stripeIntent, stripeAccountId))
    }

    private fun bypassAuth(
        host: AuthActivityStarter.Host,
        source: Source,
        stripeAccountId: String?
    ) {
        PaymentRelayStarter.create(host, SOURCE_REQUEST_CODE)
            .start(PaymentRelayStarter.Args.create(source, stripeAccountId))
    }

    private fun begin3ds2Auth(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        stripe3ds2Fingerprint: Stripe3ds2Fingerprint,
        requestOptions: ApiRequest.Options
    ) {
        val activity = host.activity ?: return

        val transaction = threeDs2Service.createTransaction(
            stripe3ds2Fingerprint.directoryServerEncryption.directoryServerId,
            messageVersionRegistry.current, stripeIntent.isLiveMode,
            stripe3ds2Fingerprint.directoryServerName,
            stripe3ds2Fingerprint.directoryServerEncryption.rootCerts,
            stripe3ds2Fingerprint.directoryServerEncryption.directoryServerPublicKey,
            stripe3ds2Fingerprint.directoryServerEncryption.keyId,
            challengeCompletionIntent = Intent(activity, Stripe3ds2CompletionActivity::class.java)
                .putExtra(
                    Stripe3ds2CompletionActivity.EXTRA_CLIENT_SECRET,
                    stripeIntent.clientSecret
                )
                .putExtra(
                    Stripe3ds2CompletionActivity.EXTRA_STRIPE_ACCOUNT,
                    requestOptions.stripeAccount
                )
                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT),
            challengeCompletionRequestCode = getRequestCode(stripeIntent)
        )

        challengeProgressActivityStarter.start(
            activity,
            stripe3ds2Fingerprint.directoryServerName,
            false,
            config.stripe3ds2Config.uiCustomization.uiCustomization
        )

        CoroutineScope(workContext).launch {
            val areqParams = transaction.createAuthenticationRequestParameters()

            val timeout = config.stripe3ds2Config.timeout
            val authParams = Stripe3ds2AuthParams(
                stripe3ds2Fingerprint.source,
                areqParams.sdkAppId,
                areqParams.sdkReferenceNumber,
                areqParams.sdkTransactionId,
                areqParams.deviceData,
                areqParams.sdkEphemeralPublicKey,
                areqParams.messageVersion,
                timeout,
                // We do not currently have a fallback url
                // TODO(smaskell-stripe): Investigate more robust error handling
                returnUrl = null
            )

            val start3ds2AuthResult = runCatching {
                stripeRepository.start3ds2Auth(
                    authParams,
                    stripeIntent.id.orEmpty(),
                    requestOptions
                )
            }

            withContext(Dispatchers.Main) {
                val callback = Stripe3ds2AuthCallback(
                    host,
                    stripeRepository,
                    transaction,
                    timeout,
                    stripeIntent,
                    stripe3ds2Fingerprint.source,
                    requestOptions,
                    analyticsRequestExecutor,
                    analyticsDataFactory,
                    enableLogging,
                    workContext = workContext
                )

                start3ds2AuthResult.fold(
                    onSuccess = {
                        callback.onSuccess(it)
                    },
                    onFailure = {
                        callback.onError(RuntimeException(it))
                    }
                )
            }
        }
    }

    private class ConfirmStripeIntentCallback constructor(
        private val host: AuthActivityStarter.Host,
        private val requestOptions: ApiRequest.Options,
        private val paymentController: PaymentController,
        private val requestCode: Int
    ) : ApiResultCallback<StripeIntent> {

        override fun onSuccess(result: StripeIntent) {
            paymentController.handleNextAction(host, result, requestOptions)
        }

        override fun onError(e: Exception) {
            handleError(host, requestCode, e)
        }
    }

    internal class Stripe3ds2AuthCallback @VisibleForTesting internal constructor(
        private val host: AuthActivityStarter.Host,
        private val stripeRepository: StripeRepository,
        private val transaction: Transaction,
        private val maxTimeout: Int,
        private val stripeIntent: StripeIntent,
        private val sourceId: String,
        private val requestOptions: ApiRequest.Options,
        private val analyticsRequestExecutor: AnalyticsRequestExecutor,
        private val analyticsDataFactory: AnalyticsDataFactory,
        private val enableLogging: Boolean = false,
        private val paymentRelayStarter: PaymentRelayStarter =
            PaymentRelayStarter.create(host, getRequestCode(stripeIntent)),
        private val workContext: CoroutineContext
    ) : ApiResultCallback<Stripe3ds2AuthResult> {

        private val analyticsRequestFactory = AnalyticsRequest.Factory(
            Logger.getInstance(enableLogging)
        )

        override fun onSuccess(result: Stripe3ds2AuthResult) {
            val ares = result.ares
            if (ares != null) {
                if (ares.isChallenge) {
                    startChallengeFlow(ares)
                } else {
                    startFrictionlessFlow()
                }
            } else if (result.fallbackRedirectUrl != null) {
                analyticsRequestExecutor.executeAsync(
                    analyticsRequestFactory.create(
                        analyticsDataFactory.createAuthParams(
                            AnalyticsEvent.Auth3ds2Fallback,
                            stripeIntent.id.orEmpty()
                        ),
                        requestOptions
                    )
                )
                beginWebAuth(
                    host,
                    getRequestCode(stripeIntent),
                    stripeIntent.clientSecret.orEmpty(),
                    result.fallbackRedirectUrl,
                    requestOptions.stripeAccount,
                    enableLogging = enableLogging,
                    // 3D-Secure requires cancelling the source when the user cancels auth (AUTHN-47)
                    shouldCancelSource = true
                )
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

                onError(
                    RuntimeException(
                        "Error encountered during 3DS2 authentication request. $errorMessage"
                    )
                )
            }
        }

        override fun onError(e: Exception) {
            paymentRelayStarter.start(
                PaymentRelayStarter.Args.create(
                    when (e) {
                        is StripeException -> e
                        else -> APIException(e)
                    }
                )
            )
        }

        private fun startFrictionlessFlow() {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.create(
                    analyticsDataFactory.createAuthParams(
                        AnalyticsEvent.Auth3ds2Frictionless,
                        stripeIntent.id.orEmpty()
                    ),
                    requestOptions
                )
            )
            paymentRelayStarter.start(PaymentRelayStarter.Args.create(stripeIntent))
        }

        private fun startChallengeFlow(ares: Stripe3ds2AuthResult.Ares) {
            val challengeParameters = ChallengeParameters(
                acsSignedContent = ares.acsSignedContent,
                threeDsServerTransactionId = ares.threeDSServerTransId,
                acsTransactionId = ares.acsTransId
            )

            val host = host.fragment?.let { fragment ->
                Stripe3ds2ActivityStarterHost(fragment)
            } ?: host.activity?.let { activity ->
                Stripe3ds2ActivityStarterHost(activity)
            } ?: return

            CoroutineScope(workContext).launch {
                delay(CHALLENGE_DELAY)

                transaction.doChallenge(
                    host,
                    challengeParameters,
                    PaymentAuth3ds2ChallengeStatusReceiver.create(
                        stripeRepository,
                        stripeIntent,
                        sourceId,
                        requestOptions,
                        analyticsRequestExecutor,
                        analyticsDataFactory,
                        transaction,
                        analyticsRequestFactory,
                        workContext
                    ),
                    maxTimeout
                )
            }
        }
    }

    internal class PaymentAuth3ds2ChallengeStatusReceiver internal constructor(
        private val stripeRepository: StripeRepository,
        private val stripeIntent: StripeIntent,
        private val sourceId: String,
        private val requestOptions: ApiRequest.Options,
        private val analyticsRequestExecutor: AnalyticsRequestExecutor,
        private val analyticsDataFactory: AnalyticsDataFactory,
        private val transaction: Transaction,
        private val analyticsRequestFactory: AnalyticsRequest.Factory,
        private val workContext: CoroutineContext
    ) : StripeChallengeStatusReceiver() {

        override fun completed(
            completionEvent: CompletionEvent,
            uiTypeCode: String,
            onReceiverCompleted: () -> Unit
        ) {
            super.completed(completionEvent, uiTypeCode, onReceiverCompleted)
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.create(
                    analyticsDataFactory.create3ds2ChallengeParams(
                        AnalyticsEvent.Auth3ds2ChallengeCompleted,
                        stripeIntent.id.orEmpty(),
                        uiTypeCode
                    ),
                    requestOptions
                )
            )
            notifyCompletion(onReceiverCompleted)
        }

        override fun cancelled(
            uiTypeCode: String,
            onReceiverCompleted: () -> Unit
        ) {
            super.cancelled(uiTypeCode, onReceiverCompleted)
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.create(
                    analyticsDataFactory.create3ds2ChallengeParams(
                        AnalyticsEvent.Auth3ds2ChallengeCanceled,
                        stripeIntent.id.orEmpty(),
                        uiTypeCode
                    ),
                    requestOptions
                )
            )
            notifyCompletion(onReceiverCompleted)
        }

        override fun timedout(
            uiTypeCode: String,
            onReceiverCompleted: () -> Unit
        ) {
            super.timedout(uiTypeCode, onReceiverCompleted)
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.create(
                    analyticsDataFactory.create3ds2ChallengeParams(
                        AnalyticsEvent.Auth3ds2ChallengeTimedOut,
                        stripeIntent.id.orEmpty(),
                        uiTypeCode
                    ),
                    requestOptions
                )
            )
            notifyCompletion(onReceiverCompleted)
        }

        override fun protocolError(
            protocolErrorEvent: ProtocolErrorEvent,
            onReceiverCompleted: () -> Unit
        ) {
            super.protocolError(protocolErrorEvent, onReceiverCompleted)
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.create(
                    analyticsDataFactory.create3ds2ChallengeErrorParams(
                        stripeIntent.id.orEmpty(),
                        protocolErrorEvent
                    ),
                    requestOptions
                )
            )
            notifyCompletion(onReceiverCompleted)
        }

        override fun runtimeError(
            runtimeErrorEvent: RuntimeErrorEvent,
            onReceiverCompleted: () -> Unit
        ) {
            super.runtimeError(runtimeErrorEvent, onReceiverCompleted)
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.create(
                    analyticsDataFactory.create3ds2ChallengeErrorParams(
                        stripeIntent.id.orEmpty(),
                        runtimeErrorEvent
                    ),
                    requestOptions
                )
            )
            notifyCompletion(onReceiverCompleted)
        }

        private fun notifyCompletion(completed3ds2Callback: () -> Unit) {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.create(
                    analyticsDataFactory.create3ds2ChallengeParams(
                        AnalyticsEvent.Auth3ds2ChallengePresented,
                        stripeIntent.id.orEmpty(),
                        transaction.initialChallengeUiType.orEmpty()
                    ),
                    requestOptions
                )
            )

            CoroutineScope(workContext).launch {
                val complete3ds2AuthResult = runCatching {
                    stripeRepository.complete3ds2Auth(
                        sourceId,
                        requestOptions
                    )
                }

                withContext(Dispatchers.Main) {
                    completed3ds2Callback()
                }
            }
        }

        internal companion object {
            internal fun create(
                stripeRepository: StripeRepository,
                stripeIntent: StripeIntent,
                sourceId: String,
                requestOptions: ApiRequest.Options,
                analyticsRequestExecutor: AnalyticsRequestExecutor,
                analyticsDataFactory: AnalyticsDataFactory,
                transaction: Transaction,
                analyticsRequestFactory: AnalyticsRequest.Factory,
                workContext: CoroutineContext
            ): PaymentAuth3ds2ChallengeStatusReceiver {
                return PaymentAuth3ds2ChallengeStatusReceiver(
                    stripeRepository,
                    stripeIntent,
                    sourceId,
                    requestOptions,
                    analyticsRequestExecutor,
                    analyticsDataFactory,
                    transaction,
                    analyticsRequestFactory,
                    workContext
                )
            }
        }
    }

    internal interface ChallengeProgressActivityStarter {
        fun start(
            context: Context,
            directoryServerName: String,
            cancelable: Boolean,
            uiCustomization: StripeUiCustomization
        )

        class Default : ChallengeProgressActivityStarter {
            override fun start(
                context: Context,
                directoryServerName: String,
                cancelable: Boolean,
                uiCustomization: StripeUiCustomization
            ) {
                ChallengeProgressActivity.show(
                    context,
                    directoryServerName,
                    cancelable,
                    uiCustomization
                )
            }
        }
    }

    internal companion object {
        internal const val PAYMENT_REQUEST_CODE = 50000
        internal const val SETUP_REQUEST_CODE = 50001
        internal const val SOURCE_REQUEST_CODE = 50002

        /**
         * Get the appropriate request code for the given stripe intent type
         *
         * @param intent the [StripeIntent] to get the request code for
         * @return PAYMENT_REQUEST_CODE or SETUP_REQUEST_CODE
         */
        @JvmSynthetic
        internal fun getRequestCode(intent: StripeIntent): Int {
            return if (intent is PaymentIntent) {
                PAYMENT_REQUEST_CODE
            } else {
                SETUP_REQUEST_CODE
            }
        }

        /**
         * Get the appropriate request code for the given stripe intent params type
         *
         * @param params the [ConfirmStripeIntentParams] to get the request code for
         * @return PAYMENT_REQUEST_CODE or SETUP_REQUEST_CODE
         */
        @JvmSynthetic
        internal fun getRequestCode(params: ConfirmStripeIntentParams): Int {
            return if (params is ConfirmPaymentIntentParams) {
                PAYMENT_REQUEST_CODE
            } else {
                SETUP_REQUEST_CODE
            }
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
            stripeAccount: String?,
            returnUrl: String? = null,
            enableLogging: Boolean = false,
            shouldCancelSource: Boolean = false,
            shouldCancelIntentOnUserNavigation: Boolean = true
        ) {
            Logger.getInstance(enableLogging).debug("PaymentAuthWebViewStarter#start()")
            val starter = PaymentAuthWebViewStarter(host, requestCode)
            starter.start(
                PaymentAuthWebViewStarter.Args(
                    clientSecret,
                    authUrl,
                    returnUrl,
                    enableLogging,
                    stripeAccountId = stripeAccount,
                    shouldCancelSource = shouldCancelSource,
                    shouldCancelIntentOnUserNavigation = shouldCancelIntentOnUserNavigation
                )
            )
        }

        private fun handleError(
            host: AuthActivityStarter.Host,
            requestCode: Int,
            exception: Exception
        ) {
            PaymentRelayStarter.create(host, requestCode)
                .start(
                    PaymentRelayStarter.Args.create(
                        when (exception) {
                            is StripeException -> exception
                            else -> APIException(exception)
                        }
                    )
                )
        }

        @JvmStatic
        @JvmOverloads
        fun create(
            context: Context,
            publishableKey: String,
            stripeRepository: StripeRepository,
            enableLogging: Boolean = false
        ): PaymentController {
            return StripePaymentController(
                context.applicationContext,
                publishableKey,
                stripeRepository,
                enableLogging
            )
        }

        @JvmSynthetic
        internal fun getClientSecret(data: Intent): String {
            return requireNotNull(PaymentController.Result.fromIntent(data)?.clientSecret)
        }

        private val EXPAND_PAYMENT_METHOD = listOf("payment_method")
        internal val CHALLENGE_DELAY = TimeUnit.SECONDS.toMillis(2L)

        private const val REQUIRED_ERROR = "API request returned an invalid response."
    }
}
