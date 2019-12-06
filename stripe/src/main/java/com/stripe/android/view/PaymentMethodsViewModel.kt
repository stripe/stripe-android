package com.stripe.android.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.CustomerSession
import com.stripe.android.StripeError
import com.stripe.android.exception.APIException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.PaymentMethod

internal class PaymentMethodsViewModel(
    private val customerSession: CustomerSession,
    internal var selectedPaymentMethodId: String? = null
) : ViewModel() {

    @JvmSynthetic
    internal fun getPaymentMethods(): LiveData<Result> {
        val resultData = MutableLiveData<Result>()
        customerSession.getPaymentMethods(PaymentMethod.Type.Card,
            object : CustomerSession.PaymentMethodsRetrievalListener {
                override fun onPaymentMethodsRetrieved(paymentMethods: List<PaymentMethod>) {
                    resultData.value = Result.Success(paymentMethods)
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    resultData.value = Result.Error(
                        APIException(
                            statusCode = errorCode,
                            message = errorMessage,
                            stripeError = stripeError
                        )
                    )
                }
            }
        )

        return resultData
    }

    internal sealed class Result {
        data class Success(val paymentMethods: List<PaymentMethod>) : Result()
        data class Error(val exception: StripeException) : Result()
    }

    internal class Factory(
        private val customerSession: CustomerSession,
        private val initialPaymentMethodId: String?
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentMethodsViewModel(customerSession, initialPaymentMethodId) as T
        }
    }
}
