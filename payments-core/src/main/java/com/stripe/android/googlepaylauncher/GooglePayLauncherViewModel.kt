package com.stripe.android.googlepaylauncher

import android.app.Application
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.StripePaymentController
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext

internal class GooglePayLauncherViewModel(
    private val paymentsClient: PaymentsClient,
    private val requestOptions: ApiRequest.Options,
    private val args: GooglePayLauncherContract.Args,
    private val stripeRepository: StripeRepository,
    private val paymentController: PaymentController,
    private val googlePayJsonFactory: GooglePayJsonFactory,
    private val googlePayRepository: GooglePayRepository
) : ViewModel() {
    var hasLaunched: Boolean = false

    private val _googleResult = MutableLiveData<GooglePayLauncher.Result>()
    internal val googlePayResult = _googleResult.distinctUntilChanged()

    fun updateResult(result: GooglePayLauncher.Result) {
        _googleResult.value = result
    }

    @VisibleForTesting
    suspend fun isReadyToPay(): Boolean {
        return googlePayRepository.isReady().first()
    }

    @VisibleForTesting
    suspend fun createPaymentDataRequest(
        args: GooglePayLauncherContract.Args
    ): JSONObject {
        val transactionInfo = when (args) {
            is GooglePayLauncherContract.PaymentIntentArgs -> {
                val paymentIntent = requireNotNull(
                    stripeRepository.retrievePaymentIntent(
                        args.clientSecret,
                        requestOptions,
                    )
                ) {
                    "Could not retrieve PaymentIntent."
                }
                createTransactionInfo(
                    paymentIntent,
                    paymentIntent.currency.orEmpty()
                )
            }
            is GooglePayLauncherContract.SetupIntentArgs -> {
                val setupIntent = requireNotNull(
                    stripeRepository.retrieveSetupIntent(
                        args.clientSecret,
                        requestOptions,
                    )
                ) {
                    "Could not retrieve SetupIntent."
                }
                createTransactionInfo(
                    setupIntent,
                    args.currencyCode
                )
            }
        }

        return googlePayJsonFactory.createPaymentDataRequest(
            transactionInfo = transactionInfo,
            merchantInfo = GooglePayJsonFactory.MerchantInfo(
                merchantName = args.config.merchantName
            ),
            billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                isRequired = args.config.billingAddressConfig.isRequired,
                format = when (args.config.billingAddressConfig.format) {
                    GooglePayLauncher.BillingAddressConfig.Format.Min ->
                        GooglePayJsonFactory.BillingAddressParameters.Format.Min
                    GooglePayLauncher.BillingAddressConfig.Format.Full ->
                        GooglePayJsonFactory.BillingAddressParameters.Format.Full
                },
                isPhoneNumberRequired = args.config.billingAddressConfig.isPhoneNumberRequired
            ),
            isEmailRequired = args.config.isEmailRequired
        )
    }

    @VisibleForTesting
    internal fun createTransactionInfo(
        stripeIntent: StripeIntent,
        currencyCode: String
    ): GooglePayJsonFactory.TransactionInfo {
        return when (stripeIntent) {
            is PaymentIntent -> {
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = currencyCode,
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                    countryCode = args.config.merchantCountryCode,
                    transactionId = stripeIntent.id,
                    totalPrice = stripeIntent.amount?.toInt(),
                    checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.CompleteImmediatePurchase
                )
            }
            is SetupIntent -> {
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = currencyCode,
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                    countryCode = args.config.merchantCountryCode,
                    transactionId = stripeIntent.id,
                    totalPrice = 0,
                    checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default
                )
            }
        }
    }

    suspend fun createLoadPaymentDataTask(): Task<PaymentData> {
        check(isReadyToPay()) {
            "Google Pay is unavailable."
        }
        return paymentsClient.loadPaymentData(
            PaymentDataRequest.fromJson(
                createPaymentDataRequest(args).toString()
            )
        )
    }

    suspend fun confirmStripeIntent(
        host: AuthActivityStarterHost,
        params: PaymentMethodCreateParams
    ) {
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

    fun onConfirmResult(
        requestCode: Int,
        data: Intent
    ) {
        viewModelScope.launch {
            val result = getResultFromConfirmation(requestCode, data)
            _googleResult.postValue(result)
        }
    }

    @VisibleForTesting
    internal suspend fun getResultFromConfirmation(
        requestCode: Int,
        data: Intent
    ): GooglePayLauncher.Result =
        runCatching {
            when {
                paymentController.shouldHandlePaymentResult(requestCode, data) -> {
                    paymentController.getPaymentIntentResult(data)
                    GooglePayLauncher.Result.Completed
                }
                paymentController.shouldHandleSetupResult(requestCode, data) -> {
                    paymentController.getSetupIntentResult(data)
                    GooglePayLauncher.Result.Completed
                }
                else -> throw IllegalStateException("Unexpected confirmation result.")
            }
        }.getOrElse { GooglePayLauncher.Result.Failed(it) }

    internal class Factory(
        private val application: Application,
        private val args: GooglePayLauncherContract.Args,
        private val enableLogging: Boolean = false,
        private val workContext: CoroutineContext = Dispatchers.IO
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
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

            val googlePayRepository = DefaultGooglePayRepository(
                application,
                args.config.environment,
                args.config.billingAddressConfig.convert(),
                args.config.existingPaymentMethodRequired,
                logger
            )

            return GooglePayLauncherViewModel(
                PaymentsClientFactory(application).create(googlePayEnvironment),
                ApiRequest.Options(
                    publishableKey,
                    stripeAccountId
                ),
                args,
                stripeRepository,
                StripePaymentController(
                    application,
                    { publishableKey },
                    stripeRepository,
                    enableLogging,
                    workContext = workContext
                ),
                GooglePayJsonFactory(
                    googlePayConfig = GooglePayConfig(publishableKey, stripeAccountId),
                    isJcbEnabled = args.config.isJcbEnabled
                ),
                googlePayRepository
            ) as T
        }
    }
}
