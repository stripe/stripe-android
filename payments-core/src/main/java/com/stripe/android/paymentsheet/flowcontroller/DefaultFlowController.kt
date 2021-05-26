package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.gms.common.api.Status
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.PaymentRelayContract
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.googlepay.StripeGooglePayEnvironment
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.payments.Stripe3ds2CompletionContract
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.ConfirmStripeIntentParamsFactory
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.repositories.PaymentMethodsApiRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

internal class DefaultFlowController internal constructor(
    private val appContext: Context,
    viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleScope: CoroutineScope,
    activityLauncherFactory: ActivityLauncherFactory,
    private val statusBarColor: () -> Int?,
    private val authHostSupplier: () -> AuthActivityStarter.Host,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val flowControllerInitializer: FlowControllerInitializer,
    paymentControllerFactory: PaymentControllerFactory,
    paymentFlowResultProcessorFactory:
        (ClientSecret, String, StripeApiRepository) ->
        PaymentFlowResultProcessor<out StripeIntent, StripeIntentResult<StripeIntent>>,
    private val eventReporter: EventReporter,
    private val sessionId: SessionId,
    defaultReturnUrl: DefaultReturnUrl,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback
) : PaymentSheet.FlowController {

    private val paymentOptionActivityLauncher = activityLauncherFactory.create(
        PaymentOptionContract()
    ) { paymentOptionResult ->
        onPaymentOptionResult(paymentOptionResult)
    }

    private val googlePayActivityLauncher = activityLauncherFactory.create(
        StripeGooglePayContract()
    ) { result ->
        onGooglePayResult(result)
    }

    internal var paymentOptionLauncher: (PaymentOptionContract.Args) -> Unit = { args ->
        paymentOptionActivityLauncher.launch(args)
    }

    internal var googlePayLauncher: (StripeGooglePayContract.Args) -> Unit = { args ->
        googlePayActivityLauncher.launch(args)
    }

    private val paymentRelayLauncher = activityLauncherFactory.create(
        PaymentRelayContract()
    ) { result ->
        onPaymentFlowResult(result)
    }

    private val paymentBrowserAuthLauncher = activityLauncherFactory.create(
        PaymentBrowserAuthContract(defaultReturnUrl)
    ) { result ->
        onPaymentFlowResult(result)
    }

    private val stripe3ds2ChallengeLauncher = activityLauncherFactory.create(
        Stripe3ds2CompletionContract()
    ) { result ->
        onPaymentFlowResult(result)
    }

    private val viewModel =
        ViewModelProvider(viewModelStoreOwner)[FlowControllerViewModel::class.java]

    // The properties below are lazily initialized to allow the developer to set the publishableKey
    // and stripeAccountId in PaymentConfiguration any time before configuring the FlowController
    // through configureWithPaymentIntent or configureWithSetupIntent.

    private val paymentConfiguration: PaymentConfiguration by lazy {
        PaymentConfiguration.getInstance(appContext)
    }

    private val stripeApiRepository: StripeApiRepository by lazy {
        StripeApiRepository(
            appContext,
            paymentConfiguration.publishableKey
        )
    }

    private val paymentFlowResultProcessor by lazy {
        paymentFlowResultProcessorFactory(
            viewModel.initData.clientSecret,
            paymentConfiguration.publishableKey,
            stripeApiRepository
        )
    }

    private val paymentController: PaymentController by lazy {
        paymentControllerFactory.create(
            paymentConfiguration.publishableKey,
            stripeApiRepository,
            paymentRelayLauncher = paymentRelayLauncher,
            paymentBrowserAuthLauncher = paymentBrowserAuthLauncher,
            stripe3ds2ChallengeLauncher = stripe3ds2ChallengeLauncher
        )
    }

    override fun configureWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        configureInternal(
            PaymentIntentClientSecret(paymentIntentClientSecret),
            configuration,
            callback
        )
    }

    override fun configureWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        configureInternal(
            SetupIntentClientSecret(setupIntentClientSecret),
            configuration,
            callback
        )
    }

    private fun configureInternal(
        clientSecret: ClientSecret,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        lifecycleScope.launch {
            val result = flowControllerInitializer.init(
                clientSecret,
                StripeIntentRepository.Api(
                    stripeRepository = stripeApiRepository,
                    requestOptions = ApiRequest.Options(
                        paymentConfiguration.publishableKey,
                        paymentConfiguration.stripeAccountId
                    ),
                    workContext = Dispatchers.IO
                ),
                PaymentMethodsApiRepository(
                    stripeRepository = stripeApiRepository,
                    publishableKey = paymentConfiguration.publishableKey,
                    stripeAccountId = paymentConfiguration.stripeAccountId,
                    workContext = Dispatchers.IO
                ),
                configuration
            )

            if (isActive) {
                dispatchResult(result, callback)
            } else {
                callback.onConfigured(false, null)
            }
        }
    }

    override fun getPaymentOption(): PaymentOption? {
        return viewModel.paymentSelection?.let {
            paymentOptionFactory.create(it)
        }
    }

    override fun presentPaymentOptions() {
        val initData = runCatching {
            viewModel.initData
        }.getOrElse {
            error(
                "FlowController must be successfully initialized using " +
                    "configureWithPaymentIntent() or configureWithSetupIntent() " +
                    "before calling presentPaymentOptions()"
            )
        }

        paymentOptionLauncher(
            PaymentOptionContract.Args(
                stripeIntent = initData.stripeIntent,
                paymentMethods = initData.paymentMethods,
                sessionId = sessionId,
                config = initData.config,
                isGooglePayReady = initData.isGooglePayReady,
                newCard = viewModel.paymentSelection as? PaymentSelection.New.Card,
                statusBarColor = statusBarColor()
            )
        )
    }

    override fun confirm() {
        val initData = runCatching {
            viewModel.initData
        }.getOrElse {
            error(
                "FlowController must be successfully initialized using " +
                    "configureWithPaymentIntent() or configureWithSetupIntent() " +
                    "before calling confirm()"
            )
        }

        val config = initData.config
        val paymentSelection = viewModel.paymentSelection
        if (paymentSelection == PaymentSelection.GooglePay) {
            if (initData.stripeIntent !is PaymentIntent) {
                error("Google Pay is currently supported only for PaymentIntents")
            }
            googlePayLauncher(
                StripeGooglePayContract.Args(
                    paymentIntent = initData.stripeIntent,
                    config = StripeGooglePayContract.GooglePayConfig(
                        environment = when (config?.googlePay?.environment) {
                            PaymentSheet.GooglePayConfiguration.Environment.Production ->
                                StripeGooglePayEnvironment.Production
                            else ->
                                StripeGooglePayEnvironment.Test
                        },
                        countryCode = config?.googlePay?.countryCode.orEmpty(),
                        merchantName = config?.merchantDisplayName
                    ),
                    statusBarColor = statusBarColor()
                )
            )
        } else {
            confirmPaymentSelection(paymentSelection, initData)
        }
    }

    private fun confirmPaymentSelection(
        paymentSelection: PaymentSelection?,
        initData: InitData
    ) {
        val confirmParamsFactory =
            ConfirmStripeIntentParamsFactory.createFactory(initData.clientSecret)

        when (paymentSelection) {
            is PaymentSelection.Saved -> {
                confirmParamsFactory.create(paymentSelection)
            }
            is PaymentSelection.New.Card -> {
                confirmParamsFactory.create(paymentSelection)
            }
            else -> null
        }?.let { confirmParams ->
            lifecycleScope.launch {
                paymentController.startConfirmAndAuth(
                    authHostSupplier(),
                    confirmParams,
                    ApiRequest.Options(
                        apiKey = paymentConfiguration.publishableKey,
                        stripeAccount = paymentConfiguration.stripeAccountId
                    )
                )
            }
        }
    }

    @VisibleForTesting
    internal fun onGooglePayResult(
        googlePayResult: StripeGooglePayContract.Result
    ) {
        when (googlePayResult) {
            is StripeGooglePayContract.Result.PaymentData -> {
                runCatching {
                    viewModel.initData
                }.fold(
                    onSuccess = { initData ->
                        val paymentSelection = PaymentSelection.Saved(
                            googlePayResult.paymentMethod
                        )
                        viewModel.paymentSelection = paymentSelection
                        confirmPaymentSelection(
                            paymentSelection,
                            initData
                        )
                    },
                    onFailure = {
                        eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                        paymentResultCallback.onPaymentSheetResult(
                            PaymentSheetResult.Failed(it)
                        )
                    }
                )
            }
            is StripeGooglePayContract.Result.Error -> {
                eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                paymentResultCallback.onPaymentSheetResult(
                    PaymentSheetResult.Failed(
                        GooglePayException(
                            googlePayResult.exception,
                            googlePayResult.googlePayStatus
                        )
                    )
                )
            }
            is StripeGooglePayContract.Result.Canceled -> {
                // don't log cancellations as failures
                paymentResultCallback.onPaymentSheetResult(PaymentSheetResult.Canceled)
            }
            else -> {
                eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                // TODO(mshafrir-stripe): handle other outcomes; for now, treat these as payment failures
            }
        }
    }

    private suspend fun dispatchResult(
        result: FlowControllerInitializer.InitResult,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) = withContext(Dispatchers.Main) {
        when (result) {
            is FlowControllerInitializer.InitResult.Success -> {
                onInitSuccess(result.initData, callback)
            }
            is FlowControllerInitializer.InitResult.Failure -> {
                callback.onConfigured(false, result.throwable)
            }
        }
    }

    private fun onInitSuccess(
        initData: InitData,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        eventReporter.onInit(initData.config)

        when (val savedString = initData.savedSelection) {
            SavedSelection.GooglePay -> {
                PaymentSelection.GooglePay
            }
            is SavedSelection.PaymentMethod -> {
                initData.paymentMethods.firstOrNull {
                    it.id == savedString.id
                }?.let {
                    PaymentSelection.Saved(it)
                }
            }
            else -> null
        }.let {
            viewModel.paymentSelection = it
        }

        viewModel.setInitData(initData)
        callback.onConfigured(true, null)
    }

    @JvmSynthetic
    internal fun onPaymentOptionResult(
        paymentOptionResult: PaymentOptionResult?
    ) {
        when (paymentOptionResult) {
            is PaymentOptionResult.Succeeded -> {
                val paymentSelection = paymentOptionResult.paymentSelection
                viewModel.paymentSelection = paymentSelection

                paymentOptionCallback.onPaymentOption(
                    paymentOptionFactory.create(
                        paymentSelection
                    )
                )
            }
            is PaymentOptionResult.Failed, is PaymentOptionResult.Canceled -> {
                paymentOptionCallback.onPaymentOption(
                    viewModel.paymentSelection?.let {
                        paymentOptionFactory.create(it)
                    }
                )
            }
            else -> {
                viewModel.paymentSelection = null
                paymentOptionCallback.onPaymentOption(null)
            }
        }
    }

    @VisibleForTesting
    internal fun onPaymentFlowResult(
        paymentFlowResult: PaymentFlowResult.Unvalidated
    ) {
        lifecycleScope.launch {
            runCatching {
                paymentFlowResultProcessor.processResult(
                    paymentFlowResult
                )
            }.fold(
                onSuccess = {
                    withContext(Dispatchers.Main) {
                        paymentResultCallback.onPaymentSheetResult(
                            createPaymentSheetResult(it)
                        )
                    }
                },
                onFailure = {
                    withContext(Dispatchers.Main) {
                        paymentResultCallback.onPaymentSheetResult(
                            PaymentSheetResult.Failed(it)
                        )
                    }
                }
            )
        }
    }

    private fun createPaymentSheetResult(
        stripeIntentResult: StripeIntentResult<StripeIntent>
    ): PaymentSheetResult {
        val stripeIntent = stripeIntentResult.intent
        return when {
            stripeIntent.status == StripeIntent.Status.Succeeded ||
                stripeIntent.status == StripeIntent.Status.RequiresCapture -> {
                PaymentSheetResult.Completed
            }
            stripeIntentResult.outcome == StripeIntentResult.Outcome.CANCELED -> {
                PaymentSheetResult.Canceled
            }
            stripeIntent.lastErrorMessage != null -> {
                PaymentSheetResult.Failed(
                    error = IllegalArgumentException(
                        "Failed to confirm ${stripeIntent.javaClass.simpleName}: " +
                            stripeIntent.lastErrorMessage
                    )
                )
            }
            else -> {
                PaymentSheetResult.Failed(
                    error = RuntimeException("Failed to complete payment.")
                )
            }
        }
    }

    class GooglePayException(
        val throwable: Throwable,
        val googleStatus: Status?
    ) : Exception()

    @Parcelize
    data class Args(
        val clientSecret: String,
        val config: PaymentSheet.Configuration?
    ) : Parcelable
}
