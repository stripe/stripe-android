package com.stripe.android.paymentsheet.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.stripe.android.paymentsheet.example.repository.DefaultRepository
import com.stripe.android.paymentsheet.example.repository.Repository
import com.stripe.android.paymentsheet.example.service.BackendApiFactory
import com.stripe.android.paymentsheet.example.service.CheckoutResponse

internal class PaymentSheetViewModel(
    application: Application,
    private val repository: Repository
) : AndroidViewModel(application) {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()

    fun statusDisplayed() {
        status.value = ""
    }

    fun prepareCheckout(customer: Repository.CheckoutCustomer, mode: Repository.CheckoutMode) =
        liveData<CheckoutResponse?> {
            inProgress.postValue(true)

            val checkoutResponse = runCatching {
                repository.checkout(
                    customer,
                    Repository.CheckoutCurrency.USD,
                    mode,
                    setShippingAddress = false,
                    setAutomaticPaymentMethods = false // need to verify this default
                )
            }

            checkoutResponse.fold(
                onSuccess = { },
                onFailure = {
                    status.postValue(
                        "${status.value}\n\nPreparing checkout failed\n${it.message}"
                    )
                }
            )

            inProgress.postValue(false)
            emit(checkoutResponse.getOrNull())
        }

    internal class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val checkoutBackendApi = BackendApiFactory(application).createCheckout()

            val repository = DefaultRepository(
                checkoutBackendApi
            )

            return PaymentSheetViewModel(
                application,
                repository
            ) as T
        }
    }
}
