package com.stripe.android

import android.app.Application
import androidx.annotation.IntRange
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod

internal class PaymentSessionViewModel(
    application: Application,
    var paymentSessionData: PaymentSessionData,
    private val customerSession: CustomerSession,
    private val paymentSessionPrefs: PaymentSessionPrefs =
        PaymentSessionPrefs.create(application.applicationContext)
) : AndroidViewModel(application) {
    init {
        customerSession.resetUsageTokens()
        customerSession.addProductUsageTokenIfValid(PaymentSession.TOKEN_PAYMENT_SESSION)
    }

    @JvmSynthetic
    fun updateCartTotal(@IntRange(from = 0) cartTotal: Long) {
        paymentSessionData = paymentSessionData.copy(cartTotal = cartTotal)
    }

    @JvmSynthetic
    fun persistPaymentMethodResult(
        paymentMethod: PaymentMethod?,
        useGooglePay: Boolean
    ) {
        customerSession.cachedCustomer?.id?.let { customerId ->
            paymentSessionPrefs.saveSelectedPaymentMethodId(customerId, paymentMethod?.id)
        }
        paymentSessionData = paymentSessionData.copy(
            paymentMethod = paymentMethod,
            useGooglePay = useGooglePay
        )
    }

    @JvmSynthetic
    fun onCompleted() {
        customerSession.resetUsageTokens()
    }

    @JvmSynthetic
    fun fetchCustomer(): LiveData<FetchCustomerResult> {
        val resultData: MutableLiveData<FetchCustomerResult> = MutableLiveData()
        customerSession.retrieveCurrentCustomer(
            object : CustomerSession.CustomerRetrievalListener {
                override fun onCustomerRetrieved(customer: Customer) {
                    resultData.value = FetchCustomerResult.Success
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    resultData.value = FetchCustomerResult.Error(
                        errorCode, errorMessage, stripeError
                    )
                }
            }
        )
        return resultData
    }

    @JvmSynthetic
    fun getSelectedPaymentMethodId(userSelectedPaymentMethodId: String? = null): String? {
        return userSelectedPaymentMethodId
            ?: if (paymentSessionData.paymentMethod != null) {
                paymentSessionData.paymentMethod?.id
            } else {
                customerSession.cachedCustomer?.id?.let { customerId ->
                    paymentSessionPrefs.getSelectedPaymentMethodId(customerId)
                }
            }
    }

    sealed class FetchCustomerResult {
        object Success : FetchCustomerResult()
        class Error(
            val errorCode: Int,
            val errorMessage: String,
            val stripeError: StripeError?
        ) : FetchCustomerResult()
    }

    internal class Factory(
        private val application: Application,
        private val paymentSessionData: PaymentSessionData,
        private val customerSession: CustomerSession
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentSessionViewModel(
                application,
                paymentSessionData,
                customerSession
            ) as T
        }
    }
}
