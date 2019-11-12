package com.stripe.android.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stripe.android.CustomerSession
import com.stripe.android.StripeError
import com.stripe.android.exception.APIException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.PaymentMethod

internal class PaymentMethodsViewModel : ViewModel() {
    private val customerSession: CustomerSession = CustomerSession.getInstance()

    @JvmSynthetic
    internal val paymentMethods: MutableLiveData<Result<*>> = MutableLiveData()

    @JvmSynthetic
    internal fun loadPaymentMethods() {
        customerSession.getPaymentMethods(PaymentMethod.Type.Card,
            object : CustomerSession.PaymentMethodsRetrievalListener {
                override fun onPaymentMethodsRetrieved(paymentMethods: List<PaymentMethod>) {
                    this@PaymentMethodsViewModel.paymentMethods.value =
                        Result.create(paymentMethods)
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    paymentMethods.value = Result.create(
                        APIException(
                            statusCode = errorCode,
                            message = errorMessage,
                            stripeError = stripeError
                        )
                    )
                }
            }
        )
    }

    internal data class Result<out T> internal constructor(
        internal val status: Status,
        internal val data: T
    ) {
        enum class Status {
            SUCCESS, ERROR
        }

        internal companion object {
            @JvmSynthetic
            internal fun create(paymentMethods: List<PaymentMethod>): Result<List<PaymentMethod>> {
                return Result(Status.SUCCESS, paymentMethods)
            }

            @JvmSynthetic
            internal fun create(exception: StripeException): Result<StripeException> {
                return Result(Status.ERROR, exception)
            }
        }
    }
}
