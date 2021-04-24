package com.stripe.android.paymentsheet.flowcontroller

import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.PaymentIntentResult
import com.stripe.android.PaymentRelayContract
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentAuthWebViewContract
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.googlepay.StripeGooglePayEnvironment
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
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
import com.stripe.android.paymentsheet.model.ConfirmParamsFactory
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

internal class DefaultFlowController internal constructor(
    viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleScope: CoroutineScope,
    activityLauncherFactory: ActivityLauncherFactory,
    private val statusBarColor: () -> Int?,
    private val authHostSupplier: () -> AuthActivityStarter.Host,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val flowControllerInitializer: FlowControllerInitializer,
    paymentControllerFactory: PaymentControllerFactory,
    private val paymentFlowResultProcessor: PaymentFlowResultProcessor,
    private val eventReporter: EventReporter,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val sessionId: SessionId,
    private val defaultReturnUrl: DefaultReturnUrl,
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

    private val paymentAuthWebViewLauncher = activityLauncherFactory.create(
        PaymentAuthWebViewContract(defaultReturnUrl)
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

    private val paymentController = paymentControllerFactory.create(
        paymentRelayLauncher = paymentRelayLauncher,
        paymentAuthWebViewLauncher = paymentAuthWebViewLauncher,
        stripe3ds2ChallengeLauncher = stripe3ds2ChallengeLauncher
    )

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
                "FlowController must be successfully initialized using configure() before calling presentPaymentOptions()"
            )
        }

        paymentOptionLauncher(
            PaymentOptionContract.Args(
                paymentIntent = initData.paymentIntent,
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
                "FlowController must be successfully initialized using configure() before calling confirmPayment()"
            )
        }

        val config = initData.config
        val paymentSelection = viewModel.paymentSelection
        if (paymentSelection == PaymentSelection.GooglePay) {
            googlePayLauncher(
                StripeGooglePayContract.Args(
                    paymentIntent = initData.paymentIntent,
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
        val confirmParamsFactory = ConfirmParamsFactory(
            defaultReturnUrl,
            PaymentIntentClientSecret(initData.paymentIntent.clientSecret.orEmpty())
        )
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
                        apiKey = publishableKey,
                        stripeAccount = stripeAccountId
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
                    PaymentSheetResult.Failed(googlePayResult.exception)
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
        }?.let {
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
                paymentFlowResultProcessor.processPaymentIntent(paymentFlowResult)
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
        paymentIntentResult: PaymentIntentResult
    ): PaymentSheetResult {
        val paymentIntent = paymentIntentResult.intent
        return when {
            paymentIntent.status == StripeIntent.Status.Succeeded ||
                paymentIntent.status == StripeIntent.Status.RequiresCapture -> {
                PaymentSheetResult.Completed
            }
            paymentIntentResult.outcome == StripeIntentResult.Outcome.CANCELED -> {
                PaymentSheetResult.Canceled
            }
            paymentIntent.lastPaymentError != null -> {
                PaymentSheetResult.Failed(
                    error = IllegalArgumentException(
                        "Failed to confirm PaymentIntent. ${paymentIntent.lastPaymentError.message}"
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

    @Parcelize
    data class Args(
        val clientSecret: String,
        val config: PaymentSheet.Configuration?
    ) : Parcelable
}
