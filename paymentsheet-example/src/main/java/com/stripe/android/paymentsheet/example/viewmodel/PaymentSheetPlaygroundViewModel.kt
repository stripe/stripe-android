package com.stripe.android.paymentsheet.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.repository.DefaultRepository
import com.stripe.android.paymentsheet.example.repository.Repository
import com.stripe.android.paymentsheet.example.service.BackendApiFactory

internal class PaymentSheetPlaygroundViewModel(
    application: Application,
    private val repository: Repository
) : AndroidViewModel(application) {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()

    val customerConfig = MutableLiveData<PaymentSheet.CustomerConfiguration?>()
    val clientSecret = MutableLiveData<String?>()

    val readyToCheckout: LiveData<Boolean> = customerConfig.switchMap { customerConfig ->
        clientSecret.switchMap { clientSecret ->
            MutableLiveData(customerConfig != null && clientSecret != null)
        }
    }

    var checkoutMode: Repository.CheckoutMode? = null
    var temporaryCustomerId: String? = null

    suspend fun prepareCheckout(
        customer: Repository.CheckoutCustomer,
        mode: Repository.CheckoutMode
    ) {
        customerConfig.value = null
        clientSecret.value = null

        inProgress.postValue(true)

        val checkoutResponse = runCatching {
            repository.checkout(
                customer, Repository.CheckoutCurrency.USD, mode
            )
        }

        checkoutResponse.fold(
            onSuccess = {
                checkoutMode = mode
                temporaryCustomerId = if (customer == Repository.CheckoutCustomer.New) {
                    it.customerId
                } else {
                    null
                }

                // Init PaymentConfiguration with the publishable key returned from the backend,
                // which will be used on all Stripe API calls
                PaymentConfiguration.init(getApplication(), it.publishableKey)

                customerConfig.value = PaymentSheet.CustomerConfiguration(
                    id = it.customerId,
                    ephemeralKeySecret = it.customerEphemeralKeySecret
                )
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
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val checkoutBackendApi = BackendApiFactory(application).createCheckout()

            val repository = DefaultRepository(
                checkoutBackendApi
            )

            return PaymentSheetPlaygroundViewModel(
                application,
                repository
            ) as T
        }
    }
}
