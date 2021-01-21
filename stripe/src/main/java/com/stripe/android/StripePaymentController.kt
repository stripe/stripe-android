package com.stripe.android

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import com.stripe.android.auth.PaymentAuthWebViewContract
import com.stripe.android.exception.StripeException
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
import com.stripe.android.networking.AlipayRepository
import com.stripe.android.networking.AnalyticsDataFactory
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.DefaultAlipayRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.DefaultPaymentFlowResultProcessor
import com.stripe.android.payments.PaymentFlowFailureMessageFactory
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.service.StripeThreeDs2ServiceImpl
import com.stripe.android.stripe3ds2.transaction.ChallengeParameters
import com.stripe.android.stripe3ds2.transaction.CompletionEvent
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
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
    private val alipayRepository: AlipayRepository = DefaultAlipayRepository(stripeRepository),
    private val paymentRelayLauncher: ActivityResultLauncher<PaymentRelayStarter.Args>? = null,
    private val paymentAuthWebViewLauncher: ActivityResultLauncher<PaymentAuthWebViewContract.Args>? = null,
    private val workContext: CoroutineContext = Dispatchers.IO
) : PaymentController {
    private val failureMessageFactory = PaymentFlowFailureMessageFactory(context)
    private val paymentFlowResultProcessor = DefaultPaymentFlowResultProcessor(
        context,
        publishableKey,
        stripeRepository,
        enableLogging,
        workContext
    )

    private val logger = Logger.getInstance(enableLogging)
    private val analyticsRequestFactory = AnalyticsRequest.Factory(logger)

    private val paymentRelayStarterFactory = { host: AuthActivityStarter.Host ->
        paymentRelayLauncher?.let {
            PaymentRelayStarter.Modern(it)
        } ?: PaymentRelayStarter.Legacy(host)
    }

    private val paymentAuthWebViewStarterFactory = { host: AuthActivityStarter.Host ->
        paymentAuthWebViewLauncher?.let {
            PaymentAuthWebViewStarter.Modern(it)
        } ?: PaymentAuthWebViewStarter.Legacy(host)
    }

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
                paymentRelayStarterFactory(host),
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
                    onFailure = {
                        dispatchError(it, callback)
                    }
                )
            }
        }
    }

    override fun startAuth(
        host: AuthActivityStarter.Host,
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        type: PaymentController.StripeIntentType
    ) {
        CoroutineScope(workContext).launch {
            val stripeIntentResult = runCatching {
                val stripeIntent = when (type) {
                    PaymentController.StripeIntentType.PaymentIntent -> {
                        stripeRepository.retrievePaymentIntent(
                            clientSecret,
                            requestOptions
                        )
                    }
                    PaymentController.StripeIntentType.SetupIntent -> {
                        stripeRepository.retrieveSetupIntent(
                            clientSecret,
                            requestOptions
                        )
                    }
                }
                requireNotNull(stripeIntent)
            }

            withContext(Dispatchers.Main) {
                stripeIntentResult.fold(
                    onSuccess = { stripeIntent ->
                        handleNextAction(host, stripeIntent, requestOptions)
                    },
                    onFailure = {
                        handleError(
                            paymentRelayStarterFactory(host),
                            when (type) {
                                PaymentController.StripeIntentType.PaymentIntent -> {
                                    PAYMENT_REQUEST_CODE
                                }
                                PaymentController.StripeIntentType.SetupIntent -> {
                                    SETUP_REQUEST_CODE
                                }
                            },
                            it
                        )
                    }
                )
            }
        }
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
                )
            )
        )

        CoroutineScope(workContext).launch {
            val sourceResult = runCatching {
                requireNotNull(
                    stripeRepository.retrieveSource(
                        sourceId = source.id.orEmpty(),
                        clientSecret = source.clientSecret.orEmpty(),
                        options = requestOptions
                    )
                )
            }

            withContext(Dispatchers.Main) {
                sourceResult.fold(
                    onSuccess = { retrievedSourced ->
                        onSourceRetrieved(host, retrievedSourced, requestOptions)
                    },
                    onFailure = {
                        handleError(
                            paymentRelayStarterFactory(host),
                            SOURCE_REQUEST_CODE,
                            it
                        )
                    }
                )
            }
        }
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
                    )
                )
            )

            paymentAuthWebViewStarterFactory(host).start(
                PaymentAuthWebViewContract.Args(
                    requestCode = SOURCE_REQUEST_CODE,
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
        CoroutineScope(workContext).launch {
            runCatching {
                paymentFlowResultProcessor.processPaymentIntent(
                    PaymentFlowResult.Unvalidated.fromIntent(data)
                )
            }.fold(
                onSuccess = {
                    dispatchPaymentIntentResult(it, callback)
                },
                onFailure = {
                    dispatchError(it, callback)
                }
            )
        }
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
        CoroutineScope(workContext).launch {
            runCatching {
                paymentFlowResultProcessor.processSetupIntent(
                    PaymentFlowResult.Unvalidated.fromIntent(data)
                )
            }.fold(
                onSuccess = {
                    dispatchSetupIntentResult(it, callback)
                },
                onFailure = {
                    dispatchError(it, callback)
                }
            )
        }
    }

    override fun handleSourceResult(
        data: Intent,
        callback: ApiResultCallback<Source>
    ) {
        val result = PaymentFlowResult.Unvalidated.fromIntent(data)
        val sourceId = result.sourceId.orEmpty()
        val clientSecret = result.clientSecret.orEmpty()

        val requestOptions = ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = result.stripeAccountId
        )

        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create(
                analyticsDataFactory.createAuthSourceParams(
                    AnalyticsEvent.AuthSourceResult,
                    sourceId
                )
            )
        )

        CoroutineScope(workContext).launch {
            val sourceResult = runCatching {
                requireNotNull(
                    stripeRepository.retrieveSource(sourceId, clientSecret, requestOptions)
                )
            }

            withContext(Dispatchers.Main) {
                sourceResult.fold(
                    onSuccess = callback::onSuccess,
                    onFailure = {
                        dispatchError(it, callback)
                    }
                )
            }
        }
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

        CoroutineScope(workContext).launch {
            runCatching {
                alipayRepository.authenticate(intent, authenticator, requestOptions)
            }.mapCatching { alipayAuth ->
                val paymentIntent = requireNotNull(
                    stripeRepository.retrievePaymentIntent(
                        intent.clientSecret.orEmpty(),
                        requestOptions,
                        expandFields = EXPAND_PAYMENT_METHOD
                    )
                )
                PaymentIntentResult(
                    paymentIntent,
                    alipayAuth.outcome,
                    failureMessageFactory.create(paymentIntent, alipayAuth.outcome)
                )
            }.let { result ->
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = callback::onSuccess,
                        onFailure = {
                            dispatchError(it, callback)
                        }
                    )
                }
            }
        }
    }

    private suspend fun dispatchPaymentIntentResult(
        paymentIntentResult: PaymentIntentResult,
        callback: ApiResultCallback<PaymentIntentResult>
    ) = withContext(Dispatchers.Main) {
        logger.debug("Dispatching PaymentIntentResult for ${paymentIntentResult.intent.id}")
        callback.onSuccess(paymentIntentResult)
    }

    private suspend fun dispatchSetupIntentResult(
        setupIntentResult: SetupIntentResult,
        callback: ApiResultCallback<SetupIntentResult>
    ) = withContext(Dispatchers.Main) {
        logger.debug("Dispatching SetupIntentResult for ${setupIntentResult.intent.id}")
        callback.onSuccess(setupIntentResult)
    }

    private suspend fun dispatchError(
        throwable: Throwable,
        callback: ApiResultCallback<*>
    ) = withContext(Dispatchers.Main) {
        callback.onError(StripeException.create(throwable))
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
                            )
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
                        handleError(
                            paymentRelayStarterFactory(host),
                            getRequestCode(stripeIntent),
                            e
                        )
                    }
                }
                is StripeIntent.NextActionData.SdkData.Use3DS1 -> {
                    analyticsRequestExecutor.executeAsync(
                        analyticsRequestFactory.create(
                            analyticsDataFactory.createAuthParams(
                                AnalyticsEvent.Auth3ds1Sdk,
                                stripeIntent.id.orEmpty()
                            )
                        )
                    )
                    beginWebAuth(
                        paymentAuthWebViewStarterFactory(host),
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
                            )
                        )
                    )

                    beginWebAuth(
                        paymentAuthWebViewStarterFactory(host),
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
                            )
                        )
                    )

                    beginWebAuth(
                        paymentAuthWebViewStarterFactory(host),
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
                            paymentAuthWebViewStarterFactory(host),
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

    @JvmSynthetic
    internal fun bypassAuth(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        stripeAccountId: String?
    ) {
        paymentRelayStarterFactory(host)
            .start(
                PaymentRelayStarter.Args.create(stripeIntent, stripeAccountId)
            )
    }

    private fun bypassAuth(
        host: AuthActivityStarter.Host,
        source: Source,
        stripeAccountId: String?
    ) {
        paymentRelayStarterFactory(host)
            .start(
                PaymentRelayStarter.Args.SourceArgs(source, stripeAccountId)
            )
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
            config.stripe3ds2Config.uiCustomization.uiCustomization,
            transaction.sdkTransactionId
        )

        CoroutineScope(workContext).launch {
            val areqParams = transaction.createAuthenticationRequestParameters()

            val timeout = config.stripe3ds2Config.timeout
            val authParams = Stripe3ds2AuthParams(
                stripe3ds2Fingerprint.source,
                areqParams.sdkAppId,
                areqParams.sdkReferenceNumber,
                areqParams.sdkTransactionId.value,
                areqParams.deviceData,
                areqParams.sdkEphemeralPublicKey,
                areqParams.messageVersion,
                timeout,
                // We do not currently have a fallback url
                // TODO(smaskell-stripe): Investigate more robust error handling
                returnUrl = null
            )

            val start3ds2AuthResult = runCatching {
                requireNotNull(
                    stripeRepository.start3ds2Auth(
                        authParams,
                        stripeIntent.id.orEmpty(),
                        requestOptions
                    )
                )
            }

            val paymentRelayStarter = paymentRelayStarterFactory(host)
            start3ds2AuthResult.fold(
                onSuccess = { authResult ->
                    on3ds2AuthSuccess(
                        authResult,
                        transaction,
                        stripe3ds2Fingerprint.source,
                        timeout,
                        paymentRelayStarter,
                        getRequestCode(stripeIntent),
                        host,
                        stripeIntent,
                        requestOptions
                    )
                },
                onFailure = { throwable ->
                    on3ds2AuthFailure(
                        throwable,
                        getRequestCode(stripeIntent),
                        paymentRelayStarter
                    )
                }
            )
        }
    }

    @VisibleForTesting
    internal suspend fun on3ds2AuthSuccess(
        result: Stripe3ds2AuthResult,
        transaction: Transaction,
        sourceId: String,
        timeout: Int,
        paymentRelayStarter: PaymentRelayStarter,
        requestCode: Int,
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options
    ) = withContext(Dispatchers.Main) {
        val ares = result.ares
        if (ares != null) {
            if (ares.isChallenge) {
                startChallengeFlow(
                    ares,
                    transaction,
                    sourceId,
                    timeout,
                    paymentRelayStarter,
                    host,
                    stripeIntent,
                    requestOptions
                )
            } else {
                startFrictionlessFlow(
                    paymentRelayStarter,
                    stripeIntent
                )
            }
        } else if (result.fallbackRedirectUrl != null) {
            on3ds2AuthFallback(
                result.fallbackRedirectUrl,
                host,
                stripeIntent,
                requestOptions
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

            on3ds2AuthFailure(
                RuntimeException(
                    "Error encountered during 3DS2 authentication request. $errorMessage"
                ),
                requestCode,
                paymentRelayStarter
            )
        }
    }

    /**
     * Used when standard 3DS2 authentication mechanisms are unavailable.
     */
    internal fun on3ds2AuthFallback(
        fallbackRedirectUrl: String,
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create(
                analyticsDataFactory.createAuthParams(
                    AnalyticsEvent.Auth3ds2Fallback,
                    stripeIntent.id.orEmpty()
                )
            )
        )
        beginWebAuth(
            paymentAuthWebViewStarterFactory(host),
            getRequestCode(stripeIntent),
            stripeIntent.clientSecret.orEmpty(),
            fallbackRedirectUrl,
            requestOptions.stripeAccount,
            enableLogging = enableLogging,
            // 3D-Secure requires cancelling the source when the user cancels auth (AUTHN-47)
            shouldCancelSource = true
        )
    }

    private suspend fun startFrictionlessFlow(
        paymentRelayStarter: PaymentRelayStarter,
        stripeIntent: StripeIntent
    ) = withContext(Dispatchers.Main) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.create(
                analyticsDataFactory.createAuthParams(
                    AnalyticsEvent.Auth3ds2Frictionless,
                    stripeIntent.id.orEmpty()
                )
            )
        )
        paymentRelayStarter.start(
            PaymentRelayStarter.Args.create(stripeIntent)
        )
    }

    @VisibleForTesting
    internal suspend fun startChallengeFlow(
        ares: Stripe3ds2AuthResult.Ares,
        transaction: Transaction,
        sourceId: String,
        maxTimeout: Int,
        paymentRelayStarter: PaymentRelayStarter,
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options
    ) = withContext(workContext) {
        runCatching {
            requireNotNull(
                host.fragment?.let { fragment ->
                    Stripe3ds2ActivityStarterHost(fragment)
                } ?: host.activity?.let { activity ->
                    Stripe3ds2ActivityStarterHost(activity)
                }
            ) {
                "Error while attempting to start 3DS2 challenge flow."
            }
        }.fold(
            onSuccess = { stripe3ds2Host ->
                delay(CHALLENGE_DELAY)

                transaction.doChallenge(
                    stripe3ds2Host,
                    ChallengeParameters(
                        acsSignedContent = ares.acsSignedContent,
                        threeDsServerTransactionId = ares.threeDSServerTransId,
                        acsTransactionId = ares.acsTransId
                    ),
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
            },
            onFailure = {
                on3ds2AuthFailure(
                    it,
                    getRequestCode(stripeIntent),
                    paymentRelayStarter
                )
            }
        )
    }

    private suspend fun on3ds2AuthFailure(
        throwable: Throwable,
        requestCode: Int,
        paymentRelayStarter: PaymentRelayStarter
    ) = withContext(Dispatchers.Main) {
        paymentRelayStarter.start(
            PaymentRelayStarter.Args.ErrorArgs(
                StripeException.create(throwable),
                requestCode
            )
        )
    }

    private class ConfirmStripeIntentCallback constructor(
        private val paymentRelayStarter: PaymentRelayStarter,
        private val host: AuthActivityStarter.Host,
        private val requestOptions: ApiRequest.Options,
        private val paymentController: PaymentController,
        private val requestCode: Int
    ) : ApiResultCallback<StripeIntent> {

        override fun onSuccess(result: StripeIntent) {
            paymentController.handleNextAction(host, result, requestOptions)
        }

        override fun onError(e: Exception) {
            handleError(
                paymentRelayStarter,
                requestCode,
                e
            )
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
                    )
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
                    )
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
                    )
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
                    )
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
                    )
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
                    )
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
            uiCustomization: StripeUiCustomization,
            sdkTransactionId: SdkTransactionId
        )

        class Default : ChallengeProgressActivityStarter {
            override fun start(
                context: Context,
                directoryServerName: String,
                cancelable: Boolean,
                uiCustomization: StripeUiCustomization,
                sdkTransactionId: SdkTransactionId
            ) {
                ChallengeProgressActivity.show(
                    context,
                    directoryServerName,
                    cancelable,
                    uiCustomization,
                    sdkTransactionId
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
         */
        private fun beginWebAuth(
            paymentWebWebViewStarter: PaymentAuthWebViewStarter,
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
            paymentWebWebViewStarter.start(
                PaymentAuthWebViewContract.Args(
                    requestCode,
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
            paymentRelayStarter: PaymentRelayStarter,
            requestCode: Int,
            throwable: Throwable
        ) {
            paymentRelayStarter
                .start(
                    PaymentRelayStarter.Args.ErrorArgs(
                        StripeException.create(throwable),
                        requestCode
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

        private val EXPAND_PAYMENT_METHOD = listOf("payment_method")
        internal val CHALLENGE_DELAY = TimeUnit.SECONDS.toMillis(2L)

        private const val REQUIRED_ERROR = "API request returned an invalid response."
    }
}
