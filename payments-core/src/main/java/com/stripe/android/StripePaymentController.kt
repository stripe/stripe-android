package com.stripe.android

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import com.google.android.instantapps.InstantApps
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.WeChatPayNextAction
import com.stripe.android.networking.AlipayRepository
import com.stripe.android.networking.DefaultAlipayRepository
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowFailureMessageFactory
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.payments.core.authentication.DefaultPaymentAuthenticatorRegistry
import com.stripe.android.payments.core.authentication.PaymentAuthenticatorRegistry
import com.stripe.android.utils.mapResult
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * A controller responsible for confirming and authenticating payment (typically through resolving
 * any required customer action). The payment authentication mechanism (e.g. 3DS) will be determined
 * by the [PaymentIntent] or [SetupIntent] object.
 */
internal class StripePaymentController
constructor(
    context: Context,
    private val publishableKeyProvider: () -> String,
    private val stripeRepository: StripeRepository,
    private val enableLogging: Boolean = false,
    workContext: CoroutineContext = Dispatchers.IO,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor =
        DefaultAnalyticsRequestExecutor(Logger.getInstance(enableLogging), workContext),
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory =
        PaymentAnalyticsRequestFactory(context.applicationContext, publishableKeyProvider),
    private val alipayRepository: AlipayRepository = DefaultAlipayRepository(stripeRepository),
    private val uiContext: CoroutineContext = Dispatchers.Main
) : PaymentController {

    private val failureMessageFactory = PaymentFlowFailureMessageFactory(context)
    private val paymentIntentFlowResultProcessor = PaymentIntentFlowResultProcessor(
        context,
        publishableKeyProvider,
        stripeRepository,
        Logger.getInstance(enableLogging),
        workContext
    )
    private val setupIntentFlowResultProcessor = SetupIntentFlowResultProcessor(
        context,
        publishableKeyProvider,
        stripeRepository,
        Logger.getInstance(enableLogging),
        workContext
    )

    private val defaultReturnUrl = DefaultReturnUrl.create(context)

    private val isInstantApp = InstantApps.isInstantApp(context)

    /**
     * [paymentRelayLauncher] is mutable and might be updated during
     * through [registerLaunchersWithActivityResultCaller]
     */
    private var paymentRelayLauncher: ActivityResultLauncher<PaymentRelayStarter.Args>? = null
    private val paymentRelayStarterFactory = { host: AuthActivityStarterHost ->
        paymentRelayLauncher?.let {
            PaymentRelayStarter.Modern(it)
        } ?: PaymentRelayStarter.Legacy(host)
    }

    /**
     * A map between 3ds1 [StripeIntent] ids to its corresponding returnUrl.
     * An entry will be removed once the [StripeIntent] is confirmed.
     */
    private val threeDs1IntentReturnUrlMap = mutableMapOf<String, String>()

    private val authenticatorRegistry: PaymentAuthenticatorRegistry =
        DefaultPaymentAuthenticatorRegistry.createInstance(
            context,
            stripeRepository,
            analyticsRequestExecutor,
            paymentAnalyticsRequestFactory,
            enableLogging,
            workContext,
            uiContext,
            threeDs1IntentReturnUrlMap,
            publishableKeyProvider,
            paymentAnalyticsRequestFactory.defaultProductUsageTokens,
            isInstantApp
        )

    override fun registerLaunchersWithActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
        paymentRelayLauncher = activityResultCaller.registerForActivityResult(
            PaymentRelayContract(),
            activityResultCallback
        )
        authenticatorRegistry.onNewActivityResultCaller(
            activityResultCaller,
            activityResultCallback
        )
    }

    override fun unregisterLaunchers() {
        paymentRelayLauncher?.unregister()
        paymentRelayLauncher = null
        authenticatorRegistry.onLauncherInvalidated()
    }

    /**
     * Confirm the Stripe Intent and resolve any next actions
     */
    override suspend fun startConfirmAndAuth(
        host: AuthActivityStarterHost,
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        requestOptions: ApiRequest.Options
    ) {
        logReturnUrl(confirmStripeIntentParams.returnUrl)
        val returnUrl =
            if (isInstantApp) {
                confirmStripeIntentParams.returnUrl
            } else {
                confirmStripeIntentParams.returnUrl.takeUnless { it.isNullOrBlank() }
                    ?: defaultReturnUrl.value
            }

        val result = when (confirmStripeIntentParams) {
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
        }

        result.fold(
            onSuccess = { intent ->
                intent.nextActionData?.let {
                    if (it is StripeIntent.NextActionData.SdkData.Use3DS1) {
                        intent.id?.let { intentId ->
                            threeDs1IntentReturnUrlMap[intentId] = returnUrl.orEmpty()
                        }
                    }
                }
                handleNextAction(
                    host,
                    intent,
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
    ): Result<PaymentIntentResult> {
        val paymentIntentResult = confirmPaymentIntent(confirmPaymentIntentParams, requestOptions)

        return paymentIntentResult.mapResult { paymentIntent ->
            authenticateAlipay(
                paymentIntent,
                authenticator,
                requestOptions
            )
        }
    }

    override suspend fun confirmWeChatPay(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        requestOptions: ApiRequest.Options
    ): Result<WeChatPayNextAction> {
        val paymentIntentResult = confirmPaymentIntent(confirmPaymentIntentParams, requestOptions)

        return paymentIntentResult.mapCatching { paymentIntent ->
            require(paymentIntent.nextActionData is StripeIntent.NextActionData.WeChatPayRedirect) {
                "Unable to confirm Payment Intent with WeChatPay SDK"
            }

            WeChatPayNextAction(
                paymentIntent = paymentIntent,
                weChat = paymentIntent.nextActionData.weChat,
            )
        }
    }

    private suspend fun confirmPaymentIntent(
        confirmStripeIntentParams: ConfirmPaymentIntentParams,
        requestOptions: ApiRequest.Options
    ): Result<PaymentIntent> {
        return stripeRepository.confirmPaymentIntent(
            // mark this request as `use_stripe_sdk=true`
            confirmPaymentIntentParams = confirmStripeIntentParams.withShouldUseStripeSdk(true),
            options = requestOptions,
            expandFields = EXPAND_PAYMENT_METHOD
        )
    }

    private suspend fun confirmSetupIntent(
        confirmStripeIntentParams: ConfirmSetupIntentParams,
        requestOptions: ApiRequest.Options
    ): Result<SetupIntent> {
        return stripeRepository.confirmSetupIntent(
            // mark this request as `use_stripe_sdk=true`
            confirmSetupIntentParams = confirmStripeIntentParams.withShouldUseStripeSdk(true),
            options = requestOptions,
            expandFields = EXPAND_PAYMENT_METHOD
        )
    }

    override suspend fun startAuth(
        host: AuthActivityStarterHost,
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        type: PaymentController.StripeIntentType
    ) {
        val stripeIntentResult = when (type) {
            PaymentController.StripeIntentType.PaymentIntent -> {
                stripeRepository.retrievePaymentIntent(clientSecret, requestOptions)
            }
            PaymentController.StripeIntentType.SetupIntent -> {
                stripeRepository.retrieveSetupIntent(clientSecret, requestOptions)
            }
        }

        stripeIntentResult.fold(
            onSuccess = { stripeIntent ->
                handleNextAction(
                    host = host,
                    stripeIntent = stripeIntent,
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
        host: AuthActivityStarterHost,
        source: Source,
        requestOptions: ApiRequest.Options
    ) {
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.AuthSourceStart)
        )

        stripeRepository.retrieveSource(
            sourceId = source.id.orEmpty(),
            clientSecret = source.clientSecret.orEmpty(),
            options = requestOptions
        ).fold(
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
        host: AuthActivityStarterHost,
        source: Source,
        requestOptions: ApiRequest.Options
    ) {
        authenticatorRegistry.getAuthenticator(source).authenticate(
            host,
            source,
            requestOptions
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
     */
    override suspend fun getPaymentIntentResult(data: Intent) =
        paymentIntentFlowResultProcessor.processResult(
            PaymentFlowResult.Unvalidated.fromIntent(data)
        )

    /**
     * Get the SetupIntent's client_secret from {@param data} and use to retrieve
     * the PaymentIntent object with updated status.
     *
     * @param data the result Intent
     * @return the [SetupIntentResult] object
     */
    override suspend fun getSetupIntentResult(data: Intent) =
        setupIntentFlowResultProcessor.processResult(
            PaymentFlowResult.Unvalidated.fromIntent(data)
        )

    /**
     * Get the Source's client_secret from {@param data} and use to retrieve
     * the Source object with updated status.
     *
     * @param data the result Intent
     * @return the [Source] object
     */
    override suspend fun getAuthenticateSourceResult(data: Intent): Result<Source> {
        val result = PaymentFlowResult.Unvalidated.fromIntent(data)
        val sourceId = result.sourceId.orEmpty()
        val clientSecret = result.clientSecret.orEmpty()

        val requestOptions = ApiRequest.Options(
            apiKey = publishableKeyProvider(),
            stripeAccount = result.stripeAccountId
        )

        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.AuthSourceResult)
        )

        return stripeRepository.retrieveSource(
            sourceId,
            clientSecret,
            requestOptions
        )
    }

    private suspend fun authenticateAlipay(
        paymentIntent: PaymentIntent,
        authenticator: AlipayAuthenticator,
        requestOptions: ApiRequest.Options
    ): Result<PaymentIntentResult> {
        val outcome = alipayRepository.authenticate(
            paymentIntent = paymentIntent,
            authenticator = authenticator,
            requestOptions = requestOptions,
        ).outcome

        val refreshedPaymentIntentResult = stripeRepository.retrievePaymentIntent(
            paymentIntent.clientSecret.orEmpty(),
            requestOptions,
            expandFields = EXPAND_PAYMENT_METHOD
        )

        return refreshedPaymentIntentResult.map { refreshedPaymentIntent ->
            PaymentIntentResult(
                refreshedPaymentIntent,
                outcome,
                failureMessageFactory.create(refreshedPaymentIntent, outcome)
            )
        }
    }

    private suspend fun handleError(
        host: AuthActivityStarterHost,
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
     */
    @VisibleForTesting
    override suspend fun handleNextAction(
        host: AuthActivityStarterHost,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        authenticatorRegistry.getAuthenticator(stripeIntent).authenticate(
            host,
            stripeIntent,
            requestOptions
        )
    }

    private fun logReturnUrl(returnUrl: String?) {
        when (returnUrl) {
            defaultReturnUrl.value -> {
                PaymentAnalyticsEvent.ConfirmReturnUrlDefault
            }
            null -> {
                PaymentAnalyticsEvent.ConfirmReturnUrlNull
            }
            else -> {
                PaymentAnalyticsEvent.ConfirmReturnUrlCustom
            }
        }.let { event ->
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(event)
            )
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
            return when (params) {
                is ConfirmPaymentIntentParams -> PAYMENT_REQUEST_CODE
                is ConfirmSetupIntentParams -> SETUP_REQUEST_CODE
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
                { publishableKey },
                stripeRepository,
                enableLogging
            )
        }

        internal val EXPAND_PAYMENT_METHOD = listOf("payment_method")
        internal val CHALLENGE_DELAY = TimeUnit.SECONDS.toMillis(2L)
    }
}
