package com.stripe.android.paymentsheet.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.repository.DefaultRepository
import com.stripe.android.paymentsheet.example.repository.Repository
import com.stripe.android.paymentsheet.example.service.BackendApiFactory

internal class LpmPlaygroundViewModel(
    application: Application,
    private val repository: Repository
) : AndroidViewModel(application) {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()

    val customerConfig = MutableLiveData<PaymentSheet.CustomerConfiguration?>()
    val clientSecret = MutableLiveData<String?>()

    val readyToCheckout: LiveData<Boolean> = clientSecret.map {
        it != null
    }

    var checkoutMode: Repository.CheckoutMode? = null
    var temporaryCustomerId: String? = null

    /**
     * Calls the backend to prepare for checkout. The server creates a new Payment or Setup Intent
     * that will be confirmed on the client using Payment Sheet.
     */
    suspend fun prepareCheckout(
        customer: Repository.CheckoutCustomer,
        currency: Repository.CheckoutCurrency,
        mode: Repository.CheckoutMode,
        setShippingAddress: Boolean
    ) {
        customerConfig.value = null
        clientSecret.value = null

        inProgress.postValue(true)

        runCatching {
            repository.checkout(customer, currency, mode, setShippingAddress)
        }.fold(
            onSuccess = {
                checkoutMode = mode
                temporaryCustomerId = if (customer == Repository.CheckoutCustomer.New) {
                    it.customerId
                } else {
                    null
                }

                customerConfig.value = it.makeCustomerConfig()
                clientSecret.value = it.intentClientSecret
            },
            onFailure = {
                status.postValue(
                    "Preparing checkout failed:\n${it.message}"
                )
            }
        )

        inProgress.postValue(false)
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

            return LpmPlaygroundViewModel(
                application,
                repository
            ) as T
        }
    }
}
