package com.stripe.android.view

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ApiResultCallback
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession
import com.stripe.android.Stripe
import com.stripe.android.core.StripeError
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.i18n.ErrorMessageTranslator
import com.stripe.android.view.i18n.TranslatorManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class AddPaymentMethodViewModel(
    private val stripe: Stripe,
    private val args: AddPaymentMethodActivityStarter.Args,
    private val errorMessageTranslator: ErrorMessageTranslator =
        TranslatorManager.getErrorMessageTranslator()
) : ViewModel() {

    private val productUsage: Set<String> = listOfNotNull(
        AddPaymentMethodActivity.PRODUCT_TOKEN,
        PaymentSession.PRODUCT_TOKEN.takeIf { args.isPaymentSessionActive }
    ).toSet()

    internal suspend fun createPaymentMethod(
        params: PaymentMethodCreateParams
    ): Result<PaymentMethod> = suspendCoroutine { continuation ->
        stripe.createPaymentMethod(
            paymentMethodCreateParams = updatedPaymentMethodCreateParams(params),
            callback = object : ApiResultCallback<PaymentMethod> {
                override fun onSuccess(result: PaymentMethod) {
                    continuation.resume(Result.success(result))
                }

                override fun onError(e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        )
    }

    @VisibleForTesting
    internal fun updatedPaymentMethodCreateParams(
        params: PaymentMethodCreateParams
    ) = params.copy(productUsage = productUsage)

    @JvmSynthetic
    internal suspend fun attachPaymentMethod(
        customerSession: CustomerSession,
        paymentMethod: PaymentMethod
    ): Result<PaymentMethod> = suspendCoroutine { continuation ->
        customerSession.attachPaymentMethod(
            paymentMethodId = paymentMethod.id.orEmpty(),
            productUsage = productUsage,
            listener = object : CustomerSession.PaymentMethodRetrievalListener {
                override fun onPaymentMethodRetrieved(paymentMethod: PaymentMethod) {
                    continuation.resume(Result.success(paymentMethod))
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    continuation.resume(
                        Result.failure(
                            RuntimeException(
                                errorMessageTranslator.translate(
                                    errorCode,
                                    errorMessage,
                                    stripeError
                                )
                            )
                        )
                    )
                }
            }
        )
    }

    internal class Factory(
        private val stripe: Stripe,
        private val args: AddPaymentMethodActivityStarter.Args
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddPaymentMethodViewModel(stripe, args) as T
        }
    }
}
