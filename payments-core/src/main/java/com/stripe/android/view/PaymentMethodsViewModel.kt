package com.stripe.android.view

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession
import com.stripe.android.R
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.model.PaymentMethod

internal class PaymentMethodsViewModel(
    application: Application,
    private val customerSession: Result<CustomerSession>,
    internal var selectedPaymentMethodId: String? = null,
    private val startedFromPaymentSession: Boolean
) : AndroidViewModel(application) {
    private val resources = application.resources
    private val cardDisplayTextFactory = CardDisplayTextFactory(application)

    internal val productUsage: Set<String> = listOfNotNull(
        PaymentSession.PRODUCT_TOKEN.takeIf { startedFromPaymentSession },
        PaymentMethodsActivity.PRODUCT_TOKEN
    ).toSet()

    internal val snackbarData: MutableLiveData<String?> = MutableLiveData()
    internal val progressData: MutableLiveData<Boolean> = MutableLiveData()

    internal fun onPaymentMethodAdded(paymentMethod: PaymentMethod) {
        createSnackbarText(paymentMethod, R.string.added)?.let {
            snackbarData.value = it
            snackbarData.value = null
        }
    }

    internal fun onPaymentMethodRemoved(paymentMethod: PaymentMethod) {
        createSnackbarText(paymentMethod, R.string.removed)?.let {
            snackbarData.value = it
            snackbarData.value = null
        }
    }

    private fun createSnackbarText(
        paymentMethod: PaymentMethod,
        @StringRes stringRes: Int
    ): String? {
        return paymentMethod.card?.let { paymentMethodId ->
            resources.getString(
                stringRes,
                cardDisplayTextFactory.createUnstyled(paymentMethodId)
            )
        }
    }

    @JvmSynthetic
    internal fun getPaymentMethods(): LiveData<Result<List<PaymentMethod>>> {
        val resultData = MutableLiveData<Result<List<PaymentMethod>>>()
        progressData.value = true

        customerSession.fold(
            onSuccess = {
                it.getPaymentMethods(
                    paymentMethodType = PaymentMethod.Type.Card,
                    productUsage = productUsage,
                    listener = object : CustomerSession.PaymentMethodsRetrievalListener {
                        override fun onPaymentMethodsRetrieved(paymentMethods: List<PaymentMethod>) {
                            resultData.value = Result.success(paymentMethods)
                            progressData.value = false
                        }

                        override fun onError(
                            errorCode: Int,
                            errorMessage: String,
                            stripeError: StripeError?
                        ) {
                            resultData.value = Result.failure(
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
            },
            onFailure = {
                resultData.value = Result.failure(it)
                progressData.value = false
            }
        )

        return resultData
    }

    internal class Factory(
        private val application: Application,
        private val customerSession: Result<CustomerSession>,
        private val initialPaymentMethodId: String?,
        private val startedFromPaymentSession: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaymentMethodsViewModel(
                application,
                customerSession,
                initialPaymentMethodId,
                startedFromPaymentSession
            ) as T
        }
    }
}
