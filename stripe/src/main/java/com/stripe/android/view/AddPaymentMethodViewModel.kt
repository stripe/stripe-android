package com.stripe.android.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ApiResultCallback
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession
import com.stripe.android.Stripe
import com.stripe.android.StripeError
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams

internal class AddPaymentMethodViewModel(
    private val stripe: Stripe,
    private val customerSession: CustomerSession,
    private val args: AddPaymentMethodActivityStarter.Args
) : ViewModel() {

    @JvmSynthetic
    internal fun logProductUsage() {
        if (args.shouldInitCustomerSessionTokens) {
            customerSession
                .addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
            if (args.isPaymentSessionActive) {
                customerSession
                    .addProductUsageTokenIfValid(PaymentSession.TOKEN_PAYMENT_SESSION)
            }
        }
    }

    internal fun createPaymentMethod(
        params: PaymentMethodCreateParams
    ): LiveData<PaymentMethodResult> {
        val resultData = MutableLiveData<PaymentMethodResult>()
        stripe.createPaymentMethod(params, object : ApiResultCallback<PaymentMethod> {
            override fun onSuccess(result: PaymentMethod) {
                resultData.value = PaymentMethodResult.Success(result)
            }

            override fun onError(e: Exception) {
                resultData.value = PaymentMethodResult.Error(e.localizedMessage.orEmpty())
            }
        })
        return resultData
    }

    @JvmSynthetic
    internal fun attachPaymentMethod(paymentMethod: PaymentMethod): LiveData<PaymentMethodResult> {
        val resultData = MutableLiveData<PaymentMethodResult>()
        customerSession.attachPaymentMethod(
            paymentMethod.id.orEmpty(),
            object : CustomerSession.PaymentMethodRetrievalListener {
                override fun onPaymentMethodRetrieved(paymentMethod: PaymentMethod) {
                    resultData.value = PaymentMethodResult.Success(paymentMethod)
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    resultData.value = PaymentMethodResult.Error(errorMessage)
                }
            }
        )
        return resultData
    }

    sealed class PaymentMethodResult {
        data class Success(val paymentMethod: PaymentMethod) : PaymentMethodResult()
        data class Error(val errorMessage: String) : PaymentMethodResult()
    }

    internal class Factory(
        private val stripe: Stripe,
        private val customerSession: CustomerSession,
        private val args: AddPaymentMethodActivityStarter.Args
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return AddPaymentMethodViewModel(
                stripe, customerSession, args
            ) as T
        }
    }
}
