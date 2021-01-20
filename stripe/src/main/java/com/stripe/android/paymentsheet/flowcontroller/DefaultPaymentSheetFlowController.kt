package com.stripe.android.paymentsheet.flowcontroller

import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.googlepay.StripeGooglePayEnvironment
import com.stripe.android.googlepay.StripeGooglePayLauncher
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.paymentsheet.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.ConfirmParamsFactory
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

internal class DefaultPaymentSheetFlowController internal constructor(
    private val activity: ComponentActivity,
    private val flowControllerInitializer: FlowControllerInitializer,
    private val paymentController: PaymentController,
    private val eventReporter: EventReporter,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val sessionId: SessionId,
    private val initScope: CoroutineScope,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback
) : PaymentSheet.FlowController {
    private val paymentOptionFactory = PaymentOptionFactory()

    private val paymentOptionActivityLauncher = activity.registerForActivityResult(
        PaymentOptionContract()
    ) { paymentOptionResult ->
        onPaymentOptionResult(paymentOptionResult)
    }

    private val googlePayActivityLauncher = activity.registerForActivityResult(
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

    private val viewModel = ViewModelProvider(activity)[FlowControllerViewModel::class.java]

    override fun init(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration,
        onInit: (Boolean, Throwable?) -> Unit
    ) {
        initScope.launch {
            val result = flowControllerInitializer.init(
                paymentIntentClientSecret,
                configuration
            )
            dispatchResult(result, onInit)
        }
    }

    override fun init(
        paymentIntentClientSecret: String,
        onInit: (Boolean, Throwable?) -> Unit
    ) {
        initScope.launch {
            val result = flowControllerInitializer.init(paymentIntentClientSecret)

            if (isActive) {
                dispatchResult(result, onInit)
            } else {
                onInit(false, null)
            }
        }
    }

    override fun getPaymentOption(): PaymentOption? {
        return viewModel.paymentSelection?.let {
            paymentOptionFactory.create(it)
        }
    }

    override fun presentPaymentOptions() {
        val initData = viewModel.initData ?: error("")

        paymentOptionLauncher(
            PaymentOptionContract.Args(
                paymentIntent = initData.paymentIntent,
                paymentMethods = initData.paymentMethods,
                sessionId = sessionId,
                config = initData.config
            )
        )
    }

    override fun confirmPayment() {
        val initData = viewModel.initData ?: error("")

        val config = initData.config
        val paymentSelection = viewModel.paymentSelection
        if (paymentSelection == PaymentSelection.GooglePay) {
            googlePayLauncher(
                StripeGooglePayContract.Args(
                    environment = when (config?.googlePay?.environment) {
                        PaymentSheet.GooglePayConfiguration.Environment.Production ->
                            StripeGooglePayEnvironment.Production
                        else ->
                            StripeGooglePayEnvironment.Test
                    },
                    paymentIntent = initData.paymentIntent,
                    countryCode = config?.googlePay?.countryCode.orEmpty(),
                    merchantName = config?.merchantDisplayName
                )
            )
        } else {
            val confirmParamsFactory = ConfirmParamsFactory(
                initData.paymentIntent.clientSecret.orEmpty()
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
                paymentController.startConfirmAndAuth(
                    AuthActivityStarter.Host.create(activity),
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
            is StripeGooglePayContract.Result.PaymentIntent -> {
                eventReporter.onPaymentSuccess(PaymentSelection.GooglePay)

                val paymentIntentResult = googlePayResult.paymentIntentResult
                val paymentIntent = paymentIntentResult.intent
                val paymentResult = when {
                    paymentIntent.status == StripeIntent.Status.Succeeded -> {
                        PaymentResult.Succeeded(paymentIntent)
                    }
                    paymentIntent.lastPaymentError != null -> {
                        PaymentResult.Failed(
                            error = IllegalArgumentException(""),
                            paymentIntent = paymentIntent
                        )
                    }
                    paymentIntentResult.outcome == StripeIntentResult.Outcome.CANCELED -> {
                        PaymentResult.Cancelled(
                            mostRecentError = null,
                            paymentIntent = paymentIntent
                        )
                    }
                    else -> {
                        PaymentResult.Failed(
                            error = RuntimeException(),
                            paymentIntent = paymentIntent
                        )
                    }
                }
                paymentResultCallback.onPaymentResult(paymentResult)
            }
            is StripeGooglePayContract.Result.Error -> {
                eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                paymentResultCallback.onPaymentResult(
                    PaymentResult.Failed(
                        googlePayResult.exception,
                        paymentIntent = null
                    )
                )
            }
            is StripeGooglePayContract.Result.Canceled -> {
                paymentResultCallback.onPaymentResult(
                    PaymentResult.Cancelled(
                        mostRecentError = null,
                        paymentIntent = null
                    )
                )
            }
            else -> {
                eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                // TODO(mshafrir-stripe): handle other outcomes; for now, treat these as payment failures
            }
        }
    }

    override fun isPaymentResult(requestCode: Int, data: Intent?): Boolean {
        return requestCode == StripeGooglePayLauncher.REQUEST_CODE ||
            paymentController.shouldHandlePaymentResult(requestCode, data)
    }

    /**
     * Handles results from both the standard confirmation flow via [PaymentController] and the
     * [StripeGooglePayContract] flow.
     */
    override fun onPaymentResult(
        requestCode: Int,
        data: Intent?,
        callback: PaymentSheetResultCallback
    ) {
        if (data != null && paymentController.shouldHandlePaymentResult(requestCode, data)) {
            paymentController.handlePaymentResult(
                data,
                object : ApiResultCallback<PaymentIntentResult> {
                    override fun onSuccess(result: PaymentIntentResult) {
                        if (result.outcome == StripeIntentResult.Outcome.SUCCEEDED) {
                            eventReporter.onPaymentSuccess(viewModel.paymentSelection)
                            callback.onPaymentResult(
                                PaymentResult.Succeeded(result.intent)
                            )
                        } else {
                            eventReporter.onPaymentFailure(viewModel.paymentSelection)

                            callback.onPaymentResult(
                                PaymentResult.Failed(
                                    RuntimeException(result.failureMessage),
                                    result.intent
                                )
                            )
                        }
                    }

                    override fun onError(e: Exception) {
                        eventReporter.onPaymentFailure(viewModel.paymentSelection)
                        callback.onPaymentResult(
                            PaymentResult.Failed(e, null)
                        )
                    }
                }
            )
        } else if (data != null && requestCode == StripeGooglePayLauncher.REQUEST_CODE) {
            when (val googlePayResult = StripeGooglePayContract.Result.fromIntent(data)) {
                is StripeGooglePayContract.Result.PaymentIntent -> {
                    eventReporter.onPaymentSuccess(PaymentSelection.GooglePay)
                    callback.onPaymentResult(
                        PaymentResult.Succeeded(
                            googlePayResult.paymentIntentResult.intent
                        )
                    )
                }
                is StripeGooglePayContract.Result.Error -> {
                    eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                    val exception = googlePayResult.exception
                    callback.onPaymentResult(
                        PaymentResult.Failed(
                            exception,
                            null
                        )
                    )
                }
                else -> {
                    eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                    callback.onPaymentResult(
                        PaymentResult.Failed(
                            RuntimeException("Google Pay attempt failed"),
                            null
                        )
                    )
                }
            }
        }
    }

    private suspend fun dispatchResult(
        result: FlowControllerInitializer.InitResult,
        onInit: (Boolean, Throwable?) -> Unit
    ) = withContext(Dispatchers.Main) {
        when (result) {
            is FlowControllerInitializer.InitResult.Success -> {
                onInitSuccess(result.initData, onInit)
            }
            is FlowControllerInitializer.InitResult.Failure -> {
                onInit(false, result.throwable)
            }
        }
    }

    private fun onInitSuccess(
        initData: InitData,
        onInit: (Boolean, Throwable?) -> Unit
    ) {
        eventReporter.onInit(initData.config)

        val defaultPaymentMethod = initData.paymentMethods.firstOrNull {
            it.id == initData.defaultPaymentMethodId
        }
        viewModel.paymentSelection = defaultPaymentMethod?.let {
            PaymentSelection.Saved(it)
        }

        viewModel.initData = initData
        onInit(true, null)
    }

    @JvmSynthetic
    internal fun onPaymentOptionResult(
        paymentOptionResult: PaymentOptionResult?
    ) {
        val paymentSelection = (paymentOptionResult as? PaymentOptionResult.Succeeded)?.paymentSelection
        viewModel.paymentSelection = paymentSelection
        paymentOptionCallback.onPaymentOption(
            paymentSelection?.let(paymentOptionFactory::create)
        )
    }

    @Parcelize
    data class Args(
        val clientSecret: String,
        val config: PaymentSheet.Configuration?
    ) : Parcelable
}
