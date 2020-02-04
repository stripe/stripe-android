package com.stripe.android.view

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.CustomerSession
import com.stripe.android.R
import com.stripe.android.StripeError
import com.stripe.android.exception.APIException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.PaymentMethod

internal class PaymentMethodsViewModel(
    application: Application,
    private val customerSession: CustomerSession,
    internal var selectedPaymentMethodId: String? = null
) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val cardDisplayTextFactory = CardDisplayTextFactory(context)

    internal val snackbarData: MutableLiveData<String> = MutableLiveData()
    internal val progressData: MutableLiveData<Boolean> = MutableLiveData()

    internal fun onPaymentMethodAdded(paymentMethod: PaymentMethod) {
        createSnackbarText(paymentMethod, R.string.added)?.let {
            snackbarData.value = it
        }
    }

    internal fun onPaymentMethodRemoved(paymentMethod: PaymentMethod) {
        createSnackbarText(paymentMethod, R.string.removed)?.let {
            snackbarData.value = it
        }
    }

    private fun createSnackbarText(
        paymentMethod: PaymentMethod,
        @StringRes stringRes: Int
    ): String? {
        return paymentMethod.card?.let { paymentMethodId ->
            context.getString(
                stringRes,
                cardDisplayTextFactory.createUnstyled(paymentMethodId)
            )
        }
    }

    @JvmSynthetic
    internal fun getPaymentMethods(): LiveData<Result> {
        val resultData = MutableLiveData<Result>()
        progressData.value = true
        customerSession.getPaymentMethods(PaymentMethod.Type.Card,
            object : CustomerSession.PaymentMethodsRetrievalListener {
                override fun onPaymentMethodsRetrieved(paymentMethods: List<PaymentMethod>) {
                    resultData.value = Result.Success(paymentMethods)
                    progressData.value = false
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    resultData.value = Result.Error(
                        APIException(
                            stripeError = stripeError,
                            statusCode = errorCode,
                            message = errorMessage
                        )
                    )
                    progressData.value = false
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
        private val application: Application,
        private val customerSession: CustomerSession,
        private val initialPaymentMethodId: String?
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentMethodsViewModel(
                application,
                customerSession,
                initialPaymentMethodId
            ) as T
        }
    }
}
