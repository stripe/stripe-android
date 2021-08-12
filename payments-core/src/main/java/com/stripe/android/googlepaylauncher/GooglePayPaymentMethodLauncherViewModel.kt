package com.stripe.android.googlepaylauncher

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext

internal class GooglePayPaymentMethodLauncherViewModel(
    private val paymentsClient: PaymentsClient,
    private val requestOptions: ApiRequest.Options,
    private val args: GooglePayPaymentMethodLauncherContract.Args,
    private val stripeRepository: StripeRepository,
    private val googlePayJsonFactory: GooglePayJsonFactory,
    private val googlePayRepository: GooglePayRepository
) : ViewModel() {
    var hasLaunched: Boolean = false

    private val _googleResult = MutableLiveData<GooglePayPaymentMethodLauncher.Result>()
    internal val googlePayResult = _googleResult.distinctUntilChanged()

    fun updateResult(result: GooglePayPaymentMethodLauncher.Result) {
        _googleResult.value = result
    }

    @VisibleForTesting
    suspend fun isReadyToPay(): Boolean {
        return googlePayRepository.isReady().first()
    }

    @VisibleForTesting
    fun createPaymentDataRequest(): JSONObject {
        return googlePayJsonFactory.createPaymentDataRequest(
            transactionInfo = createTransactionInfo(args),
            merchantInfo = GooglePayJsonFactory.MerchantInfo(
                merchantName = args.config.merchantName
            ),
            billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                isRequired = args.config.billingAddressConfig.isRequired,
                format = when (args.config.billingAddressConfig.format) {
                    GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Min ->
                        GooglePayJsonFactory.BillingAddressParameters.Format.Min
                    GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full ->
                        GooglePayJsonFactory.BillingAddressParameters.Format.Full
                },
                isPhoneNumberRequired = args.config.billingAddressConfig.isPhoneNumberRequired
            ),
            isEmailRequired = args.config.isEmailRequired
        )
    }

    @VisibleForTesting
    internal fun createTransactionInfo(
        args: GooglePayPaymentMethodLauncherContract.Args
    ): GooglePayJsonFactory.TransactionInfo {
        return GooglePayJsonFactory.TransactionInfo(
            currencyCode = args.currencyCode,
            totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
            countryCode = args.config.merchantCountryCode,
            transactionId = args.transactionId,
            totalPrice = args.amount,
            checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default
        )
    }

    suspend fun createLoadPaymentDataTask(): Task<PaymentData> {
        check(isReadyToPay()) {
            "Google Pay is unavailable."
        }
        return paymentsClient.loadPaymentData(
            PaymentDataRequest.fromJson(createPaymentDataRequest().toString())
        )
    }

    suspend fun createPaymentMethod(
        paymentData: PaymentData
    ): GooglePayPaymentMethodLauncher.Result {
        val paymentDataJson = JSONObject(paymentData.toJson())

        val params = PaymentMethodCreateParams.createFromGooglePay(paymentDataJson)

        return runCatching {
            requireNotNull(
                stripeRepository.createPaymentMethod(params, requestOptions)
            )
        }.fold(
            onSuccess = {
                GooglePayPaymentMethodLauncher.Result.Completed(it)
            },
            onFailure = {
                GooglePayPaymentMethodLauncher.Result.Failed(
                    it,
                    when (it) {
                        is APIConnectionException -> GooglePayPaymentMethodLauncher.NETWORK_ERROR
                        is InvalidRequestException -> GooglePayPaymentMethodLauncher.DEVELOPER_ERROR
                        else -> GooglePayPaymentMethodLauncher.INTERNAL_ERROR
                    }
                )
            }
        )
    }

    internal class Factory(
        private val application: Application,
        private val args: GooglePayPaymentMethodLauncherContract.Args,
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

            val analyticsRequestFactory = AnalyticsRequestFactory(
                application,
                publishableKey,
                setOf(GooglePayPaymentMethodLauncher.PRODUCT_USAGE)
            )

            val stripeRepository = StripeApiRepository(
                application,
                { publishableKey },
                logger = logger,
                workContext = workContext,
                analyticsRequestFactory = analyticsRequestFactory
            )

            val billingAddressConfig = args.config.billingAddressConfig
            val googlePayRepository = DefaultGooglePayRepository(
                application,
                googlePayEnvironment,
                GooglePayJsonFactory.BillingAddressParameters(
                    billingAddressConfig.isRequired,
                    when (billingAddressConfig.format) {
                        GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Min -> {
                            GooglePayJsonFactory.BillingAddressParameters.Format.Min
                        }
                        GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full -> {
                            GooglePayJsonFactory.BillingAddressParameters.Format.Full
                        }
                    },
                    billingAddressConfig.isPhoneNumberRequired
                ),
                args.config.existingPaymentMethodRequired,
                logger
            )

            return GooglePayPaymentMethodLauncherViewModel(
                PaymentsClientFactory(application).create(googlePayEnvironment),
                ApiRequest.Options(
                    publishableKey,
                    stripeAccountId
                ),
                args,
                stripeRepository,
                GooglePayJsonFactory(
                    googlePayConfig = GooglePayConfig(publishableKey, stripeAccountId),
                    isJcbEnabled = args.config.isJcbEnabled
                ),
                googlePayRepository
            ) as T
        }
    }
}
