package com.stripe.android.googlepay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext

internal class StripeGooglePayViewModel(
    application: Application,
    private val publishableKey: String,
    private val args: StripeGooglePayContract.Args,
    private val stripeRepository: StripeRepository,
    private val appName: String,
    private val workContext: CoroutineContext
) : AndroidViewModel(application) {
    var hasLaunched: Boolean = false
    var paymentMethod: PaymentMethod? = null

    private val googlePayJsonFactory = GooglePayJsonFactory(application)

    private val _googleResult = MutableLiveData<StripeGooglePayContract.Result>()
    internal val googlePayResult = _googleResult.distinctUntilChanged()

    fun createIsReadyToPayRequest(): IsReadyToPayRequest {
        return IsReadyToPayRequest.fromJson(
            googlePayJsonFactory.createIsReadyToPayRequest().toString()
        )
    }

    fun updateGooglePayResult(result: StripeGooglePayContract.Result) {
        _googleResult.value = result
    }

    fun createPaymentDataRequestForPaymentIntentArgs(): JSONObject {
        val paymentIntent = args.paymentIntent
        return googlePayJsonFactory.createPaymentDataRequest(
            transactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = paymentIntent.currency.orEmpty(),
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                countryCode = args.config.countryCode,
                transactionId = paymentIntent.id,
                totalPrice = paymentIntent.amount?.toInt(),
                checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.CompleteImmediatePurchase
            ),
            merchantInfo = GooglePayJsonFactory.MerchantInfo(
                merchantName = args.config.merchantName ?: appName
            ),
            billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                isRequired = true,
                format = GooglePayJsonFactory.BillingAddressParameters.Format.Min,
                isPhoneNumberRequired = false
            ),
            isEmailRequired = args.config.isEmailRequired
        )
    }

    fun createPaymentMethod(
        params: PaymentMethodCreateParams
    ) = liveData {
        withContext(workContext) {
            emit(
                runCatching {
                    requireNotNull(
                        stripeRepository.createPaymentMethod(
                            params,
                            ApiRequest.Options(publishableKey)
                        )
                    )
                }
            )
        }
    }

    internal class Factory(
        private val application: Application,
        private val publishableKey: String,
        private val args: StripeGooglePayContract.Args
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val appName = application.applicationInfo.loadLabel(application.packageManager).toString()
            return StripeGooglePayViewModel(
                application,
                publishableKey,
                args,
                StripeApiRepository(application, publishableKey),
                appName,
                Dispatchers.IO
            ) as T
        }
    }
}
