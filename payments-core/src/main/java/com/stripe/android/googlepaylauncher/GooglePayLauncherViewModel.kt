package com.stripe.android.googlepaylauncher

import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.StripePaymentController
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.googlepaylauncher.GooglePayLauncher.BillingAddressConfig.Format
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.utils.mapResult
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@Suppress("LongParameterList")
internal class GooglePayLauncherViewModel(
    private val paymentsClient: PaymentsClient,
    private val requestOptions: ApiRequest.Options,
    private val args: GooglePayLauncherContract.Args,
    private val stripeRepository: StripeRepository,
    private val paymentController: PaymentController,
    private val googlePayJsonFactory: GooglePayJsonFactory,
    private val googlePayRepository: GooglePayRepository,
    private val savedStateHandle: SavedStateHandle,
    private val errorReporter: ErrorReporter,
    private val workContext: CoroutineContext,
) : ViewModel() {
    /**
     * [hasLaunched] indicates whether Google Pay has already been launched, and must be persisted
     * across process death in case the Activity and ViewModel are destroyed while the user is
     * interacting with Google Pay.
     */
    private var hasLaunched: Boolean
        get() = savedStateHandle.get<Boolean>(HAS_LAUNCHED_KEY) == true
        set(value) = savedStateHandle.set(HAS_LAUNCHED_KEY, value)

    private val _googleResult = MutableSharedFlow<GooglePayLauncher.Result?>(replay = 1)
    internal val googlePayResult = _googleResult.asSharedFlow()

    private val _googlePayLaunchTask = MutableSharedFlow<Task<PaymentData>?>(replay = 1)
    val googlePayLaunchTask = _googlePayLaunchTask.asSharedFlow()

    init {
        viewModelScope.launch(workContext) {
            if (!hasLaunched) {
                resolveLoadPaymentDataTask().fold(
                    onSuccess = {
                        _googlePayLaunchTask.emit(it)
                    },
                    onFailure = {
                        updateResult(
                            GooglePayLauncher.Result.Failed(it)
                        )
                    }
                )
            }
        }
    }

    fun updateResult(result: GooglePayLauncher.Result) {
        _googleResult.tryEmit(result)
    }

    @VisibleForTesting
    suspend fun isReadyToPay(): Boolean {
        return googlePayRepository.isReady().first()
    }

    @VisibleForTesting
    suspend fun createPaymentDataRequest(
        args: GooglePayLauncherContract.Args
    ): Result<String> {
        val transactionInfoResult = when (args) {
            is GooglePayLauncherContract.PaymentIntentArgs -> {
                stripeRepository.retrievePaymentIntent(
                    args.clientSecret,
                    requestOptions
                ).map { intent ->
                    createTransactionInfo(
                        stripeIntent = intent,
                        currencyCode = intent.currency.orEmpty(),
                        label = args.label,
                    )
                }
            }
            is GooglePayLauncherContract.SetupIntentArgs -> {
                stripeRepository.retrieveSetupIntent(
                    args.clientSecret,
                    requestOptions
                ).map { intent ->
                    createTransactionInfo(
                        stripeIntent = intent,
                        currencyCode = args.currencyCode,
                        amount = args.amount,
                        label = args.label,
                    )
                }
            }
        }

        return transactionInfoResult.map { info ->
            googlePayJsonFactory.createPaymentDataRequest(
                transactionInfo = info,
                merchantInfo = GooglePayJsonFactory.MerchantInfo(
                    merchantName = args.config.merchantName,
                ),
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                    isRequired = args.config.billingAddressConfig.isRequired,
                    format = when (args.config.billingAddressConfig.format) {
                        Format.Min -> GooglePayJsonFactory.BillingAddressParameters.Format.Min
                        Format.Full -> GooglePayJsonFactory.BillingAddressParameters.Format.Full
                    },
                    isPhoneNumberRequired = args.config.billingAddressConfig.isPhoneNumberRequired,
                ),
                isEmailRequired = args.config.isEmailRequired,
                allowCreditCards = args.config.allowCreditCards,
                cardBrandFilter = DefaultCardBrandFilter
            ).toString()
        }
    }

    @VisibleForTesting
    internal fun createTransactionInfo(
        stripeIntent: StripeIntent,
        currencyCode: String,
        amount: Long? = null,
        label: String? = null,
    ): GooglePayJsonFactory.TransactionInfo {
        return when (stripeIntent) {
            is PaymentIntent -> {
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = currencyCode,
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                    countryCode = args.config.merchantCountryCode,
                    transactionId = stripeIntent.id,
                    totalPrice = stripeIntent.amount,
                    totalPriceLabel = null,
                    checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.CompleteImmediatePurchase,
                )
            }
            is SetupIntent -> {
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = currencyCode,
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                    countryCode = args.config.merchantCountryCode,
                    transactionId = stripeIntent.id,
                    totalPrice = amount ?: 0L,
                    totalPriceLabel = label,
                    checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default,
                )
            }
        }
    }

    private suspend fun resolveLoadPaymentDataTask(): Result<Task<PaymentData>> {
        return runCatching {
            check(isReadyToPay()) { "Google Pay is unavailable." }
        }.mapResult {
            createPaymentDataRequest(args)
        }.mapCatching { json ->
            PaymentDataRequest.fromJson(json)
        }.map { request ->
            paymentsClient.loadPaymentData(request).awaitTask()
        }
    }

    fun confirmStripeIntent(
        host: AuthActivityStarterHost,
        params: PaymentMethodCreateParams
    ) {
        viewModelScope.launch(workContext) {
            val confirmStripeIntentParams = when (args) {
                is GooglePayLauncherContract.PaymentIntentArgs ->
                    ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                        paymentMethodCreateParams = params,
                        clientSecret = args.clientSecret
                    )
                is GooglePayLauncherContract.SetupIntentArgs ->
                    ConfirmSetupIntentParams.create(
                        paymentMethodCreateParams = params,
                        clientSecret = args.clientSecret
                    )
            }

            paymentController.startConfirmAndAuth(
                host,
                confirmStripeIntentParams,
                requestOptions
            )
        }
    }

    fun onConfirmResult(
        requestCode: Int,
        data: Intent
    ) {
        viewModelScope.launch(workContext) {
            val result = getResultFromConfirmation(requestCode, data)
            _googleResult.emit(result)
        }
    }

    @VisibleForTesting
    internal suspend fun getResultFromConfirmation(
        requestCode: Int,
        data: Intent
    ): GooglePayLauncher.Result {
        val result = when {
            paymentController.shouldHandlePaymentResult(requestCode, data) -> {
                paymentController.getPaymentIntentResult(data)
            }
            paymentController.shouldHandleSetupResult(requestCode, data) -> {
                paymentController.getSetupIntentResult(data)
            }
            else -> {
                val error = IllegalStateException("Unexpected confirmation result.")
                errorReporter.report(
                    ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_UNEXPECTED_CONFIRM_RESULT,
                    StripeException.create(error),
                    additionalNonPiiParams = mapOf("request_code" to requestCode.toString()),
                )
                Result.failure(error)
            }
        }

        return result.fold(
            onSuccess = { GooglePayLauncher.Result.Completed },
            onFailure = { GooglePayLauncher.Result.Failed(it) },
        )
    }

    fun markTaskAsLaunched() {
        hasLaunched = true
        _googlePayLaunchTask.tryEmit(null)
    }

    internal class Factory(
        private val args: GooglePayLauncherContract.Args,
        private val enableLogging: Boolean = false,
        private val workContext: CoroutineContext = Dispatchers.IO,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()

            val googlePayEnvironment = args.config.environment
            val logger = Logger.getInstance(enableLogging)

            val config = PaymentConfiguration.getInstance(application)
            val publishableKey = config.publishableKey
            val stripeAccountId = config.stripeAccountId
            val productUsageTokens = setOf(GooglePayLauncher.PRODUCT_USAGE)

            val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
                application,
                publishableKey,
                productUsageTokens
            )

            val stripeRepository = StripeApiRepository(
                application,
                { publishableKey },
                logger = logger,
                workContext = workContext,
                productUsageTokens = productUsageTokens,
                paymentAnalyticsRequestFactory = analyticsRequestFactory
            )

            val errorReporter = ErrorReporter.createFallbackInstance(
                context = application,
                productUsage = productUsageTokens
            )

            val googlePayRepository = DefaultGooglePayRepository(
                context = application,
                environment = args.config.environment,
                billingAddressParameters = args.config.billingAddressConfig.convert(),
                existingPaymentMethodRequired = args.config.existingPaymentMethodRequired,
                allowCreditCards = args.config.allowCreditCards,
                errorReporter = errorReporter,
                logger = logger
            )

            return GooglePayLauncherViewModel(
                paymentsClient = DefaultPaymentsClientFactory(context = application).create(googlePayEnvironment),
                requestOptions = ApiRequest.Options(
                    publishableKey,
                    stripeAccountId
                ),
                args = args,
                stripeRepository = stripeRepository,
                paymentController = StripePaymentController(
                    application,
                    { publishableKey },
                    stripeRepository,
                    enableLogging,
                    workContext = workContext
                ),
                googlePayJsonFactory = GooglePayJsonFactory(
                    googlePayConfig = GooglePayConfig(publishableKey, stripeAccountId),
                    isJcbEnabled = args.config.isJcbEnabled
                ),
                googlePayRepository = googlePayRepository,
                savedStateHandle = extras.createSavedStateHandle(),
                errorReporter = errorReporter,
                workContext = workContext,
            ) as T
        }
    }

    companion object {
        @VisibleForTesting
        const val HAS_LAUNCHED_KEY = "has_launched"
    }
}
