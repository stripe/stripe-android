package com.stripe.android.paymentsheet.flowcontroller

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.StripeIntentResult
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.Injector
import com.stripe.android.payments.core.injection.InjectorKey
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.DaggerFlowControllerComponent
import com.stripe.android.paymentsheet.injection.FlowControllerComponent
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.ConfirmStripeIntentParamsFactory
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.view.AuthActivityStarterHost
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
internal class DefaultFlowController @Inject internal constructor(
    // Properties provided through FlowControllerComponent.Builder
    private val lifecycleScope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    private val statusBarColor: () -> Int?,
    private val authHostSupplier: () -> AuthActivityStarterHost,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback,
    activityResultCaller: ActivityResultCaller,
    @InjectorKey private val injectorKey: Int,
    // Properties provided through injection
    private val flowControllerInitializer: FlowControllerInitializer,
    private val eventReporter: EventReporter,
    private val viewModel: FlowControllerViewModel,
    private val paymentController: PaymentController,
    /**
     * [PaymentConfiguration] is [Lazy] because the client might set publishableKey and
     * stripeAccountId after creating a [DefaultFlowController].
     */
    private val lazyPaymentConfiguration: Lazy<PaymentConfiguration>,
    /**
     * [PaymentFlowResultProcessor] is wrapped with [Provider] because it needs
     * [FlowControllerViewModel.initData] to be set, which might happen multiple times post
     * [DefaultFlowController] creation and after [configureWithPaymentIntent] or
     * [configureWithPaymentIntent] is called.
     * TODO: Observe on [FlowControllerViewModel.initData] change and initialize
     *   paymentFlowResultProcessor afterwards.
     */
    private val paymentFlowResultProcessorProvider:
        Provider<PaymentFlowResultProcessor<out StripeIntent, StripeIntentResult<StripeIntent>>>
) : PaymentSheet.FlowController, Injector {
    private val paymentOptionActivityLauncher: ActivityResultLauncher<PaymentOptionContract.Args>
    private var googlePayActivityLauncher:
        ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>

    /**
     * [FlowControllerComponent] is hold to inject into [Activity]s and created
     * after [DefaultFlowController].
     */
    lateinit var flowControllerComponent: FlowControllerComponent

    override fun inject(injectable: Injectable) {
        when (injectable) {
            is PaymentOptionsViewModel.Factory -> {
                flowControllerComponent.inject(injectable)
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
                fun onCreate() {
                    paymentController.registerLaunchersWithActivityResultCaller(
                        activityResultCaller,
                        ::onPaymentFlowResult
                    )
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    paymentController.unregisterLaunchers()
                }
            }
        )

        paymentOptionActivityLauncher =
            activityResultCaller.registerForActivityResult(
                PaymentOptionContract(),
                ::onPaymentOptionResult
            )
        googlePayActivityLauncher =
            activityResultCaller.registerForActivityResult(
                GooglePayPaymentMethodLauncherContract(),
                ::onGooglePayResult
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

        paymentOptionActivityLauncher.launch(
            PaymentOptionContract.Args(
                stripeIntent = initData.stripeIntent,
                paymentMethods = initData.paymentMethods,
                config = initData.config,
                isGooglePayReady = initData.isGooglePayReady,
                newCard = viewModel.paymentSelection as? PaymentSelection.New.Card,
                statusBarColor = statusBarColor(),
                injectorKey = injectorKey
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

        val paymentSelection = viewModel.paymentSelection
        if (paymentSelection == PaymentSelection.GooglePay) {
            // initData.config.googlePay is guaranteed not to be null or GooglePay would be disabled
            val config = requireNotNull(initData.config)
            val googlePayConfig = requireNotNull(config.googlePay)

            googlePayActivityLauncher.launch(
                GooglePayPaymentMethodLauncherContract.Args(
                    config = GooglePayPaymentMethodLauncher.Config(
                        environment = when (googlePayConfig.environment) {
                            PaymentSheet.GooglePayConfiguration.Environment.Production ->
                                GooglePayEnvironment.Production
                            else ->
                                GooglePayEnvironment.Test
                        },
                        merchantCountryCode = googlePayConfig.countryCode,
                        merchantName = config.merchantDisplayName
                    ),
                    currencyCode = (initData.stripeIntent as? PaymentIntent)?.currency
                        ?: googlePayConfig.currencyCode.orEmpty(),
                    amount = (initData.stripeIntent as? PaymentIntent)?.amount?.toInt() ?: 0,
                    transactionId = initData.stripeIntent.id
                )
            )
        } else {
            confirmPaymentSelection(paymentSelection, initData)
        }
    }

    @VisibleForTesting
    fun confirmPaymentSelection(
        paymentSelection: PaymentSelection?,
        initData: InitData
    ) {
        val confirmParamsFactory =
            ConfirmStripeIntentParamsFactory.createFactory(initData.clientSecret)

        when (paymentSelection) {
            is PaymentSelection.Saved -> {
                confirmParamsFactory.create(paymentSelection)
            }
            is PaymentSelection.New -> {
                confirmParamsFactory.create(paymentSelection)
            }
            else -> null
        }?.let { confirmParams ->
            lifecycleScope.launch {
                paymentController.startConfirmAndAuth(
                    authHostSupplier(),
                    confirmParams,
                    ApiRequest.Options(
                        apiKey = lazyPaymentConfiguration.get().publishableKey,
                        stripeAccount = lazyPaymentConfiguration.get().stripeAccountId
                    )
                )
            }
        }
    }

    internal fun onGooglePayResult(
        googlePayResult: GooglePayPaymentMethodLauncher.Result
    ) {
        when (googlePayResult) {
            is GooglePayPaymentMethodLauncher.Result.Completed -> {
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
            is GooglePayPaymentMethodLauncher.Result.Failed -> {
                eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                paymentResultCallback.onPaymentSheetResult(
                    PaymentSheetResult.Failed(
                        GooglePayException(
                            googlePayResult.error
                        )
                    )
                )
            }
            is GooglePayPaymentMethodLauncher.Result.Canceled -> {
                // don't log cancellations as failures
                paymentResultCallback.onPaymentSheetResult(PaymentSheetResult.Canceled)
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

    internal fun onPaymentFlowResult(
        paymentFlowResult: PaymentFlowResult.Unvalidated
    ) {
        lifecycleScope.launch {
            runCatching {
                paymentFlowResultProcessorProvider.get().processResult(
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
    ) = when (stripeIntentResult.outcome) {
        StripeIntentResult.Outcome.SUCCEEDED -> {
            PaymentSheetResult.Completed
        }
        StripeIntentResult.Outcome.CANCELED -> {
            PaymentSheetResult.Canceled
        }
        else -> {
            PaymentSheetResult.Failed(
                error = stripeIntentResult.intent.lastErrorMessage?.let {
                    IllegalArgumentException(
                        "Failed to confirm ${stripeIntentResult.intent.javaClass.simpleName}: $it"
                    )
                } ?: RuntimeException("Failed to complete payment.")
            )
        }
    }

    class GooglePayException(
        val throwable: Throwable
    ) : Exception(throwable)

    @Parcelize
    data class Args(
        val clientSecret: String,
        val config: PaymentSheet.Configuration?
    ) : Parcelable

    companion object {
        fun getInstance(
            appContext: Context,
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleScope: CoroutineScope,
            lifecycleOwner: LifecycleOwner,
            activityResultCaller: ActivityResultCaller,
            statusBarColor: () -> Int?,
            authHostSupplier: () -> AuthActivityStarterHost,
            paymentOptionFactory: PaymentOptionFactory,
            paymentOptionCallback: PaymentOptionCallback,
            paymentResultCallback: PaymentSheetResultCallback
        ): PaymentSheet.FlowController {
            val injectorKey = WeakMapInjectorRegistry.nextKey()
            val flowControllerComponent = DaggerFlowControllerComponent.builder()
                .appContext(appContext)
                .viewModelStoreOwner(viewModelStoreOwner)
                .lifecycleScope(lifecycleScope)
                .lifeCycleOwner(lifecycleOwner)
                .activityResultCaller(activityResultCaller)
                .statusBarColor(statusBarColor)
                .authHostSupplier(authHostSupplier)
                .paymentOptionFactory(paymentOptionFactory)
                .paymentOptionCallback(paymentOptionCallback)
                .paymentResultCallback(paymentResultCallback)
                .injectorKey(injectorKey)
                .build()
            val flowController = flowControllerComponent.flowController
            flowController.flowControllerComponent = flowControllerComponent
            WeakMapInjectorRegistry.register(flowController, injectorKey)
            return flowController
        }
    }
}
