package com.stripe.android.view

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession
import com.stripe.android.R
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class PaymentMethodsViewModel(
    application: Application,
    private val customerSession: Result<CustomerSession>,
    internal var selectedPaymentMethodId: String? = null,
    private val startedFromPaymentSession: Boolean
) : AndroidViewModel(application) {
    private val resources = application.resources
    private val cardDisplayTextFactory = CardDisplayTextFactory(application)

    @Volatile
    private var paymentMethodsJob: Job? = null

    internal val productUsage: Set<String> = listOfNotNull(
        PaymentSession.PRODUCT_TOKEN.takeIf { startedFromPaymentSession },
        PaymentMethodsActivity.PRODUCT_TOKEN
    ).toSet()

    internal val paymentMethodsData: MutableStateFlow<Result<List<PaymentMethod>>?> = MutableStateFlow(null)
    internal val snackbarData: MutableStateFlow<String?> = MutableStateFlow(null)
    internal val progressData: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        getPaymentMethods()
    }

    internal fun onPaymentMethodAdded(paymentMethod: PaymentMethod) {
        createSnackbarText(paymentMethod, R.string.stripe_added)?.let {
            snackbarData.value = it
            snackbarData.value = null
        }
        getPaymentMethods()
    }

    internal fun onPaymentMethodRemoved(paymentMethod: PaymentMethod) {
        createSnackbarText(paymentMethod, R.string.stripe_removed)?.let {
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

    private fun getPaymentMethods() {
        paymentMethodsJob?.cancel()

        paymentMethodsJob = viewModelScope.launch {
            progressData.value = true

            customerSession.fold(
                onSuccess = {
                    it.getPaymentMethods(
                        paymentMethodType = PaymentMethod.Type.Card,
                        productUsage = productUsage,
                        listener = object : CustomerSession.PaymentMethodsRetrievalListener {
                            override fun onPaymentMethodsRetrieved(paymentMethods: List<PaymentMethod>) {
                                paymentMethodsData.value = Result.success(paymentMethods)
                                progressData.value = false
                            }

                            override fun onError(
                                errorCode: Int,
                                errorMessage: String,
                                stripeError: StripeError?
                            ) {
                                paymentMethodsData.value = Result.failure(
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
                    paymentMethodsData.value = Result.failure(it)
                    progressData.value = false
                }
            )
        }
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
