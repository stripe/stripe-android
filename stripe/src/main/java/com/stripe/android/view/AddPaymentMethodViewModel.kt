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
import java.lang.RuntimeException

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
    ): LiveData<Result<PaymentMethod>> {
        val resultData = MutableLiveData<Result<PaymentMethod>>()
        stripe.createPaymentMethod(
            paymentMethodCreateParams = params.copy(productUsage = productUsage),
            callback = object : ApiResultCallback<PaymentMethod> {
                override fun onSuccess(result: PaymentMethod) {
                    resultData.value = Result.success(result)
                }

                override fun onError(e: Exception) {
                    resultData.value = Result.failure(e)
                }
            })
        return resultData
    }

    @JvmSynthetic
    internal fun attachPaymentMethod(paymentMethod: PaymentMethod): LiveData<Result<PaymentMethod>> {
        val resultData = MutableLiveData<Result<PaymentMethod>>()
        customerSession.attachPaymentMethod(
            paymentMethodId = paymentMethod.id.orEmpty(),
            productUsage = productUsage,
            listener = object : CustomerSession.PaymentMethodRetrievalListener {
                override fun onPaymentMethodRetrieved(paymentMethod: PaymentMethod) {
                    resultData.value = Result.success(paymentMethod)
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    resultData.value = Result.failure(
                        RuntimeException(
                            errorMessageTranslator.translate(
                                errorCode,
                                errorMessage,
                                stripeError
                            )
                        )
                    )
                }
            }
        )
        return resultData
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
