package com.stripe.android.googlepaylauncher

import android.app.Application
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.StripePaymentController
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.CoroutineContext

internal class GooglePayLauncherViewModel(
    private val requestOptions: ApiRequest.Options,
    private val args: GooglePayLauncherContract.Args,
    private val stripeRepository: StripeRepository,
    private val paymentController: PaymentController,
    private val googlePayJsonFactory: GooglePayJsonFactory
) : ViewModel() {
    var hasLaunched: Boolean = false

    private val _googleResult = MutableLiveData<GooglePayLauncher.Result>()
    internal val googlePayResult = _googleResult.distinctUntilChanged()

    fun createIsReadyToPayRequest(): IsReadyToPayRequest {
        return IsReadyToPayRequest.fromJson(
            googlePayJsonFactory.createIsReadyToPayRequest().toString()
        )
    }

    fun updateResult(result: GooglePayLauncher.Result) {
        _googleResult.value = result
    }

    suspend fun createPaymentDataRequest(): JSONObject {
        val transactionInfo = createTransactionInfo(
            getStripeIntent(args.clientSecret)
        )

        return googlePayJsonFactory.createPaymentDataRequest(
            transactionInfo = transactionInfo,
            merchantInfo = GooglePayJsonFactory.MerchantInfo(
                merchantName = args.config.merchantName
            ),
            billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                isRequired = true,
                format = GooglePayJsonFactory.BillingAddressParameters.Format.Min,
                isPhoneNumberRequired = false
            ),
            isEmailRequired = args.config.isEmailRequired
        )
    }

    @VisibleForTesting
    internal fun createTransactionInfo(
        stripeIntent: StripeIntent
    ): GooglePayJsonFactory.TransactionInfo {
        return when (stripeIntent) {
            is PaymentIntent -> {
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = stripeIntent.currency.orEmpty(),
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                    countryCode = args.config.merchantCountryCode,
                    transactionId = stripeIntent.id,
                    totalPrice = stripeIntent.amount?.toInt(),
                    checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.CompleteImmediatePurchase
                )
            }
            is SetupIntent -> {
                // TODO(mshafrir-stripe): add SetupIntent support
                error("SetupIntents are not currently supported in GooglePayLauncher.")
            }
        }
    }

    @VisibleForTesting
    internal suspend fun getStripeIntent(
        clientSecret: String
    ): StripeIntent {
        return when {
            PaymentIntent.ClientSecret.isMatch(clientSecret) -> {
                requireNotNull(
                    stripeRepository.retrievePaymentIntent(
                        clientSecret,
                        requestOptions
                    )
                ) {
                    "Could not retrieve PaymentIntent."
                }
            }
            SetupIntent.ClientSecret.isMatch(clientSecret) -> {
                requireNotNull(
                    stripeRepository.retrieveSetupIntent(
                        clientSecret,
                        requestOptions
                    )
                ) {
                    "Could not retrieve SetupIntent."
                }
            }
            else -> {
                error("Invalid client secret.")
            }
        }
    }

    suspend fun confirmPaymentIntent(
        host: AuthActivityStarterHost,
        params: PaymentMethodCreateParams
    ) {
        paymentController.startConfirmAndAuth(
            host,
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = params,
                clientSecret = args.clientSecret
            ),
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
    ): GooglePayLauncher.Result {
        return when {
            paymentController.shouldHandlePaymentResult(requestCode, data) -> {
                paymentController.getPaymentIntentResult(data)
                GooglePayLauncher.Result.Completed
            }
            paymentController.shouldHandleSetupResult(requestCode, data) -> {
                paymentController.getSetupIntentResult(data)
                GooglePayLauncher.Result.Completed
            }
            else -> {
                GooglePayLauncher.Result.Failed(
                    IllegalStateException("Unexpected result.")
                )
            }
        }
    }

    internal class Factory(
        private val application: Application,
        private val args: GooglePayLauncherContract.Args,
        private val enableLogging: Boolean = false,
        private val workContext: CoroutineContext = Dispatchers.IO
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val config = PaymentConfiguration.getInstance(application)
            val publishableKey = config.publishableKey
            val stripeAccountId = config.stripeAccountId

            val stripeRepository = StripeApiRepository(
                application,
                { publishableKey },
                workContext = workContext
            )
            return GooglePayLauncherViewModel(
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
                    isJcbEnabled = args.config.merchantCountryCode == Locale.JAPAN.country
                )
            ) as T
        }
    }
}
