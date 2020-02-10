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
import com.stripe.android.view.i18n.ErrorMessageTranslator
import com.stripe.android.view.i18n.TranslatorManager

internal class AddPaymentMethodViewModel(
    private val stripe: Stripe,
    private val customerSession: CustomerSession,
    private val args: AddPaymentMethodActivityStarter.Args,
    private val errorMessageTranslator: ErrorMessageTranslator =
        TranslatorManager.getErrorMessageTranslator()
) : ViewModel() {

    private val productUsage: Set<String> = listOfNotNull(
        AddPaymentMethodActivity.PRODUCT_TOKEN,
        PaymentSession.PRODUCT_TOKEN.takeIf { args.isPaymentSessionActive }
    ).toSet()

    internal fun createPaymentMethod(
        params: PaymentMethodCreateParams
    ): LiveData<PaymentMethodResult> {
        val resultData = MutableLiveData<PaymentMethodResult>()
        stripe.createPaymentMethod(
            paymentMethodCreateParams = params.copy(productUsage = productUsage),
            callback = object : ApiResultCallback<PaymentMethod> {
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
            paymentMethodId = paymentMethod.id.orEmpty(),
            productUsage = productUsage,
            listener = object : CustomerSession.PaymentMethodRetrievalListener {
                override fun onPaymentMethodRetrieved(paymentMethod: PaymentMethod) {
                    resultData.value = PaymentMethodResult.Success(paymentMethod)
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    resultData.value = PaymentMethodResult.Error(
                        errorMessageTranslator.translate(
                            errorCode,
                            errorMessage,
                            stripeError
                        )
                    )
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
