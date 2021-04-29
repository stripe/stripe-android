package com.stripe.android

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.InvalidRequestException
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
import com.stripe.android.networking.AlipayRepository
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.DefaultAlipayRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.CustomTabsCapabilities
import com.stripe.android.payments.DefaultPaymentFlowResultProcessor
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.DefaultStripeChallengeStatusReceiver
import com.stripe.android.payments.PaymentFlowFailureMessageFactory
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.Stripe3ds2CompletionStarter
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.service.StripeThreeDs2ServiceImpl
import com.stripe.android.stripe3ds2.transaction.ChallengeParameters
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transaction.Stripe3ds2ActivityStarterHost
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.stripe3ds2.views.ChallengeProgressActivity
import com.stripe.android.view.AuthActivityStarter
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
    private val analyticsRequestFactory: AnalyticsRequestFactory =
        AnalyticsRequestFactory(context.applicationContext, publishableKey),
    private val challengeProgressActivityStarter: ChallengeProgressActivityStarter =
        ChallengeProgressActivityStarter.Default(),
    private val alipayRepository: AlipayRepository = DefaultAlipayRepository(stripeRepository),
    private val paymentRelayLauncher: ActivityResultLauncher<PaymentRelayStarter.Args>? = null,
    private val paymentBrowserAuthLauncher: ActivityResultLauncher<PaymentBrowserAuthContract.Args>? = null,
    private val stripe3ds2ChallengeLauncher: ActivityResultLauncher<PaymentFlowResult.Unvalidated>? = null,
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val uiContext: CoroutineContext = Dispatchers.Main
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
    private val defaultReturnUrl = DefaultReturnUrl.create(context)

    private val paymentRelayStarterFactory = { host: AuthActivityStarter.Host ->
        paymentRelayLauncher?.let {
            PaymentRelayStarter.Modern(it)
        } ?: PaymentRelayStarter.Legacy(host)
    }

    private val isCustomTabsSupported: Boolean by lazy {
        CustomTabsCapabilities(context).isSupported()
    }

    private val paymentBrowserAuthStarterFactory = { host: AuthActivityStarter.Host ->
        paymentBrowserAuthLauncher?.let {
            PaymentBrowserAuthStarter.Modern(it)
        } ?: PaymentBrowserAuthStarter.Legacy(
            host,
            isCustomTabsSupported,
            defaultReturnUrl
        )
    }

    private val stripe3ds2CompletionStarterFactory =
        { host: AuthActivityStarter.Host, requestCode: Int ->
            stripe3ds2ChallengeLauncher?.let {
                Stripe3ds2CompletionStarter.Modern(it)
            } ?: Stripe3ds2CompletionStarter.Legacy(host, requestCode)
        }

    init {
        threeDs2Service.initialize(
            config.stripe3ds2Config.uiCustomization.uiCustomization
        )
    }

    /**
     * Confirm the Stripe Intent and resolve any next actions
     */
    override suspend fun startConfirmAndAuth(
        host: AuthActivityStarter.Host,
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        requestOptions: ApiRequest.Options
    ) {
        logReturnUrl(confirmStripeIntentParams.returnUrl)

        val returnUrl = confirmStripeIntentParams.returnUrl.takeUnless { it.isNullOrBlank() }
            ?: defaultReturnUrl.value

        runCatching {
            when (confirmStripeIntentParams) {
                is ConfirmPaymentIntentParams -> {
                    confirmPaymentIntent(
                        confirmStripeIntentParams.also {
                            it.returnUrl = returnUrl
                        },
                        requestOptions
                    )
                }
                is ConfirmSetupIntentParams -> {
                    confirmSetupIntent(
                        confirmStripeIntentParams.also {
                            it.returnUrl = returnUrl
                        },
                        requestOptions
                    )
                }
                else -> error("Confirmation params must be ConfirmPaymentIntentParams or ConfirmSetupIntentParams")
            }
        }.fold(
            onSuccess = { intent ->
                handleNextAction(
                    host,
                    intent,
                    returnUrl,
                    requestOptions
                )
            },
            onFailure = {
                handleError(
                    host,
                    getRequestCode(confirmStripeIntentParams),
                    it
                )
            }
        )
    }

    override suspend fun confirmAndAuthenticateAlipay(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        authenticator: AlipayAuthenticator,
        requestOptions: ApiRequest.Options
    ): PaymentIntentResult {
        return authenticateAlipay(
            confirmPaymentIntent(
                confirmPaymentIntentParams,
                requestOptions
            ),
            authenticator,
            requestOptions
        )
    }

    override suspend fun confirmWeChatPay(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        requestOptions: ApiRequest.Options
    ): WeChatPayNextAction {
        confirmPaymentIntent(
            confirmPaymentIntentParams,
            requestOptions
        ).let { paymentIntent ->
            require(paymentIntent.nextActionData is StripeIntent.NextActionData.WeChatPayRedirect) {
                "Unable to confirm Payment Intent with WeChatPay SDK"
            }
            return WeChatPayNextAction(
                paymentIntent,
                paymentIntent.nextActionData.weChat,
            )
        }
    }

    private suspend fun confirmPaymentIntent(
        confirmStripeIntentParams: ConfirmPaymentIntentParams,
        requestOptions: ApiRequest.Options
    ): PaymentIntent {
        return requireNotNull(
            stripeRepository.confirmPaymentIntent(
                // mark this request as `use_stripe_sdk=true`
                confirmStripeIntentParams
                    .withShouldUseStripeSdk(shouldUseStripeSdk = true),
                requestOptions,
                expandFields = EXPAND_PAYMENT_METHOD
            )
        ) {
            REQUIRED_ERROR
        }
    }

    private suspend fun confirmSetupIntent(
        confirmStripeIntentParams: ConfirmSetupIntentParams,
        requestOptions: ApiRequest.Options
    ): SetupIntent {
        return requireNotNull(
            stripeRepository.confirmSetupIntent(
                // mark this request as `use_stripe_sdk=true`
                confirmStripeIntentParams
                    .withShouldUseStripeSdk(shouldUseStripeSdk = true),
                requestOptions,
                expandFields = EXPAND_PAYMENT_METHOD
            )
        ) {
            REQUIRED_ERROR
        }
    }

    override suspend fun startAuth(
        host: AuthActivityStarter.Host,
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        type: PaymentController.StripeIntentType
    ) {
        runCatching {
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
        }.fold(
            onSuccess = { stripeIntent ->
                handleNextAction(
                    host = host,
                    stripeIntent = stripeIntent,
                    returnUrl = null,
                    requestOptions = requestOptions
                )
            },
            onFailure = {
                handleError(
                    host,
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

    override suspend fun startAuthenticateSource(
        host: AuthActivityStarter.Host,
        source: Source,
        requestOptions: ApiRequest.Options
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createAuthSource(
                AnalyticsEvent.AuthSourceStart,
                source.id
            )
        )

        runCatching {
            requireNotNull(
                stripeRepository.retrieveSource(
                    sourceId = source.id.orEmpty(),
                    clientSecret = source.clientSecret.orEmpty(),
                    options = requestOptions
                )
            )
        }.fold(
            onSuccess = { retrievedSourced ->
                onSourceRetrieved(host, retrievedSourced, requestOptions)
            },
            onFailure = {
                handleError(
                    host,
                    SOURCE_REQUEST_CODE,
                    it
                )
            }
        )
    }

    private suspend fun onSourceRetrieved(
        host: AuthActivityStarter.Host,
        source: Source,
        requestOptions: ApiRequest.Options
    ) {
        if (source.flow == Source.Flow.Redirect) {
            startSourceAuth(
                paymentBrowserAuthStarterFactory(host),
                source,
                requestOptions
            )
        } else {
            bypassAuth(host, source, requestOptions.stripeAccount)
        }
    }

    private suspend fun startSourceAuth(
        paymentBrowserAuthStarter: PaymentBrowserAuthStarter,
        source: Source,
        requestOptions: ApiRequest.Options
    ) = withContext(uiContext) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createAuthSource(
                AnalyticsEvent.AuthSourceRedirect,
                source.id
            )
        )

        paymentBrowserAuthStarter.start(
            PaymentBrowserAuthContract.Args(
                objectId = source.id.orEmpty(),
                requestCode = SOURCE_REQUEST_CODE,
                clientSecret = source.clientSecret.orEmpty(),
                url = source.redirect?.url.orEmpty(),
                returnUrl = source.redirect?.returnUrl,
                enableLogging = enableLogging,
                stripeAccountId = requestOptions.stripeAccount
            )
        )
    }

    /**
     * Decide whether [getPaymentIntentResult] should be called.
     */
    override fun shouldHandlePaymentResult(requestCode: Int, data: Intent?): Boolean {
        return requestCode == PAYMENT_REQUEST_CODE && data != null
    }

    /**
     * Decide whether [getSetupIntentResult] should be called.
     */
    override fun shouldHandleSetupResult(requestCode: Int, data: Intent?): Boolean {
        return requestCode == SETUP_REQUEST_CODE && data != null
    }

    override fun shouldHandleSourceResult(requestCode: Int, data: Intent?): Boolean {
        return requestCode == SOURCE_REQUEST_CODE && data != null
    }

    /**
     * Get the PaymentIntent's client_secret from {@param data} and use to retrieve
     * the PaymentIntent object with updated status.
     *
     * @param data the result Intent
     * @return the [PaymentIntentResult] object
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
     * @throws IllegalArgumentException if the response's JsonParser returns null
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        IllegalArgumentException::class
    )
    override suspend fun getPaymentIntentResult(data: Intent) =
        paymentFlowResultProcessor.processPaymentIntent(
            PaymentFlowResult.Unvalidated.fromIntent(data)
        )

    /**
     * Get the SetupIntent's client_secret from {@param data} and use to retrieve
     * the PaymentIntent object with updated status.
     *
     * @param data the result Intent
     * @return the [SetupIntentResult] object
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
     * @throws IllegalArgumentException if the response's JsonParser returns null
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        IllegalArgumentException::class
    )
    override suspend fun getSetupIntentResult(data: Intent) =
        paymentFlowResultProcessor.processSetupIntent(
            PaymentFlowResult.Unvalidated.fromIntent(data)
        )

    /**
     * Get the Source's client_secret from {@param data} and use to retrieve
     * the Source object with updated status.
     *
     * @param data the result Intent
     * @return the [Source] object
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
     * @throws IllegalArgumentException if the Source response's JsonParser returns null
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        IllegalArgumentException::class
    )
    override suspend fun getAuthenticateSourceResult(data: Intent): Source {
        val result = PaymentFlowResult.Unvalidated.fromIntent(data)
        val sourceId = result.sourceId.orEmpty()
        val clientSecret = result.clientSecret.orEmpty()

        val requestOptions = ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = result.stripeAccountId
        )

        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createAuthSource(
                AnalyticsEvent.AuthSourceResult,
                sourceId
            )
        )

        return requireNotNull(
            stripeRepository.retrieveSource(
                sourceId,
                clientSecret,
                requestOptions
            )
        )
    }

    private suspend fun authenticateAlipay(
        paymentIntent: PaymentIntent,
        authenticator: AlipayAuthenticator,
        requestOptions: ApiRequest.Options
    ): PaymentIntentResult {
        val outcome =
            alipayRepository.authenticate(paymentIntent, authenticator, requestOptions).outcome
        val refreshedPaymentIntent = requireNotNull(
            stripeRepository.retrievePaymentIntent(
                paymentIntent.clientSecret.orEmpty(),
                requestOptions,
                expandFields = EXPAND_PAYMENT_METHOD
            )
        )

        return PaymentIntentResult(
            refreshedPaymentIntent,
            outcome,
            failureMessageFactory.create(refreshedPaymentIntent, outcome)
        )
    }

    private suspend fun handleError(
        host: AuthActivityStarter.Host,
        requestCode: Int,
        throwable: Throwable
    ) = withContext(uiContext) {
        paymentRelayStarterFactory(host)
            .start(
                PaymentRelayStarter.Args.ErrorArgs(
                    StripeException.create(throwable),
                    requestCode
                )
            )
    }

    /**
     * Determine which authentication mechanism should be used, or bypass authentication
     * if it is not needed.
     *
     * @param returnUrl in some cases, the return URL is not provided in
     * [StripeIntent.NextActionData]. Specifically, it is not available in
     * [StripeIntent.NextActionData.SdkData.Use3DS1]. Wire it through so that we can correctly
     * determine how we should handle authentication.
     */
    @VisibleForTesting
    override suspend fun handleNextAction(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        returnUrl: String?,
        requestOptions: ApiRequest.Options
    ) {
        if (stripeIntent.requiresAction()) {
            when (val nextActionData = stripeIntent.nextActionData) {
                is StripeIntent.NextActionData.SdkData.Use3DS2 -> {
                    handle3ds2Auth(
                        host,
                        stripeIntent,
                        requestOptions,
                        nextActionData
                    )
                }
                is StripeIntent.NextActionData.SdkData.Use3DS1 -> {
                    // can only triggered when `use_stripe_sdk=true`
                    handle3ds1Auth(
                        host,
                        stripeIntent,
                        requestOptions,
                        nextActionData,
                        returnUrl
                    )
                }
                is StripeIntent.NextActionData.RedirectToUrl -> {
                    // can only triggered when `use_stripe_sdk=false`
                    handleRedirectToUrlAuth(
                        host,
                        stripeIntent,
                        requestOptions,
                        nextActionData
                    )
                }
                is StripeIntent.NextActionData.AlipayRedirect -> {
                    handleAlipayAuth(
                        host,
                        stripeIntent,
                        requestOptions,
                        nextActionData
                    )
                }
                is StripeIntent.NextActionData.DisplayOxxoDetails -> {
                    handleOxxoAuth(
                        host,
                        stripeIntent,
                        requestOptions,
                        nextActionData
                    )
                }
                else -> bypassAuth(host, stripeIntent, requestOptions.stripeAccount)
            }
        } else {
            // no action required, so bypass authentication
            bypassAuth(host, stripeIntent, requestOptions.stripeAccount)
        }
    }

    private suspend fun handle3ds2Auth(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options,
        nextActionData: StripeIntent.NextActionData.SdkData.Use3DS2
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createAuth(
                AnalyticsEvent.Auth3ds2Fingerprint,
                stripeIntent.id.orEmpty()
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
                host,
                getRequestCode(stripeIntent),
                e
            )
        }
    }

    private suspend fun handle3ds1Auth(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options,
        nextActionData: StripeIntent.NextActionData.SdkData.Use3DS1,
        returnUrl: String?
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createAuth(
                AnalyticsEvent.Auth3ds1Sdk,
                stripeIntent.id.orEmpty()
            )
        )
        beginWebAuth(
            paymentBrowserAuthStarterFactory(host),
            stripeIntent,
            getRequestCode(stripeIntent),
            stripeIntent.clientSecret.orEmpty(),
            nextActionData.url,
            requestOptions.stripeAccount,
            returnUrl = returnUrl,
            // 3D-Secure requires cancelling the source when the user cancels auth (AUTHN-47)
            shouldCancelSource = true
        )
    }

    private suspend fun handleRedirectToUrlAuth(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options,
        nextActionData: StripeIntent.NextActionData.RedirectToUrl
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createAuth(
                AnalyticsEvent.AuthRedirect,
                stripeIntent.id.orEmpty()
            )
        )

        beginWebAuth(
            paymentBrowserAuthStarterFactory(host),
            stripeIntent,
            getRequestCode(stripeIntent),
            stripeIntent.clientSecret.orEmpty(),
            nextActionData.url.toString(),
            requestOptions.stripeAccount,
            nextActionData.returnUrl
        )
    }

    /**
     * If using the standard confirmation path, handle Alipay the same as
     * a standard webview redirect.
     * Alipay Native SDK use case is handled by [Stripe.confirmAlipayPayment]
     * outside of the standard confirmation path.
     */
    private suspend fun handleAlipayAuth(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options,
        nextActionData: StripeIntent.NextActionData.AlipayRedirect
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createAuth(
                AnalyticsEvent.AuthRedirect,
                stripeIntent.id.orEmpty()
            )
        )

        beginWebAuth(
            paymentBrowserAuthStarterFactory(host),
            stripeIntent,
            getRequestCode(stripeIntent),
            stripeIntent.clientSecret.orEmpty(),
            nextActionData.webViewUrl.toString(),
            requestOptions.stripeAccount,
            nextActionData.returnUrl
        )
    }

    private suspend fun handleOxxoAuth(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options,
        nextActionData: StripeIntent.NextActionData.DisplayOxxoDetails
    ) {
        // TODO(smaskell): add analytics event
        if (nextActionData.hostedVoucherUrl != null) {
            beginWebAuth(
                paymentBrowserAuthStarterFactory(host),
                stripeIntent,
                getRequestCode(stripeIntent),
                stripeIntent.clientSecret.orEmpty(),
                nextActionData.hostedVoucherUrl,
                requestOptions.stripeAccount,
                shouldCancelIntentOnUserNavigation = false
            )
        } else {
            // TODO(smaskell): Determine how to handle missing URL
            bypassAuth(host, stripeIntent, requestOptions.stripeAccount)
        }
    }

    @JvmSynthetic
    internal suspend fun bypassAuth(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        stripeAccountId: String?
    ) = withContext(uiContext) {
        paymentRelayStarterFactory(host)
            .start(
                PaymentRelayStarter.Args.create(stripeIntent, stripeAccountId)
            )
    }

    private suspend fun bypassAuth(
        host: AuthActivityStarter.Host,
        source: Source,
        stripeAccountId: String?
    ) = withContext(uiContext) {
        paymentRelayStarterFactory(host)
            .start(
                PaymentRelayStarter.Args.SourceArgs(source, stripeAccountId)
            )
    }

    private suspend fun begin3ds2Auth(
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
            stripe3ds2Fingerprint.directoryServerEncryption.keyId
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
    ) {
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
            val errorMessage = result.error?.let { error ->
                listOf(
                    "Code: ${error.errorCode}",
                    "Detail: ${error.errorDetail}",
                    "Description: ${error.errorDescription}",
                    "Component: ${error.errorComponent}"
                ).joinToString(separator = ", ")
            } ?: "Invalid 3DS2 authentication response"

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
    internal suspend fun on3ds2AuthFallback(
        fallbackRedirectUrl: String,
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createAuth(
                AnalyticsEvent.Auth3ds2Fallback,
                stripeIntent.id.orEmpty()
            )
        )
        beginWebAuth(
            paymentBrowserAuthStarterFactory(host),
            stripeIntent,
            getRequestCode(stripeIntent),
            stripeIntent.clientSecret.orEmpty(),
            fallbackRedirectUrl,
            requestOptions.stripeAccount,
            // 3D-Secure requires cancelling the source when the user cancels auth (AUTHN-47)
            shouldCancelSource = true
        )
    }

    private suspend fun startFrictionlessFlow(
        paymentRelayStarter: PaymentRelayStarter,
        stripeIntent: StripeIntent
    ) = withContext(uiContext) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createAuth(
                AnalyticsEvent.Auth3ds2Frictionless,
                stripeIntent.id.orEmpty()
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
                    DefaultStripeChallengeStatusReceiver(
                        stripe3ds2CompletionStarterFactory(host, getRequestCode(stripeIntent)),
                        stripeRepository,
                        stripeIntent,
                        sourceId,
                        requestOptions,
                        analyticsRequestExecutor,
                        analyticsRequestFactory,
                        transaction,
                        workContext = workContext
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
    ) = withContext(uiContext) {
        paymentRelayStarter.start(
            PaymentRelayStarter.Args.ErrorArgs(
                StripeException.create(throwable),
                requestCode
            )
        )
    }

    /**
     * Start in-app WebView activity.
     */
    private suspend fun beginWebAuth(
        paymentBrowserWebStarter: PaymentBrowserAuthStarter,
        stripeIntent: StripeIntent,
        requestCode: Int,
        clientSecret: String,
        authUrl: String,
        stripeAccount: String?,
        returnUrl: String? = null,
        shouldCancelSource: Boolean = false,
        shouldCancelIntentOnUserNavigation: Boolean = true
    ) = withContext(uiContext) {
        logger.debug("PaymentBrowserAuthStarter#start()")
        paymentBrowserWebStarter.start(
            PaymentBrowserAuthContract.Args(
                objectId = stripeIntent.id.orEmpty(),
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

    private fun logReturnUrl(returnUrl: String?) {
        when (returnUrl) {
            defaultReturnUrl.value -> {
                AnalyticsEvent.ConfirmReturnUrlDefault
            }
            null -> {
                AnalyticsEvent.ConfirmReturnUrlNull
            }
            else -> {
                AnalyticsEvent.ConfirmReturnUrlCustom
            }
        }.let { event ->
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(event)
            )
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

        internal val EXPAND_PAYMENT_METHOD = listOf("payment_method")
        internal val CHALLENGE_DELAY = TimeUnit.SECONDS.toMillis(2L)

        private const val REQUIRED_ERROR = "API request returned an invalid response."
    }
}
