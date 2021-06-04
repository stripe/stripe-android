package com.stripe.android.paymentsheet.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.repository.DefaultRepository
import com.stripe.android.paymentsheet.example.repository.Repository
import com.stripe.android.paymentsheet.example.service.BackendApiFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.single
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetPlaygroundViewModel(
    application: Application,
    private val repository: Repository
) : AndroidViewModel(application) {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()

    val customerConfig = MutableLiveData<PaymentSheet.CustomerConfiguration?>()
    val clientSecret = MutableLiveData<String?>()

    var checkoutMode: Repository.CheckoutMode? = null
    var temporaryCustomerId: String? = null

    fun prepareCheckout(
        customer: Repository.CheckoutCustomer,
        mode: Repository.CheckoutMode
    ) =
        liveData {
            customerConfig.value = null
            clientSecret.value = null

            inProgress.postValue(true)

            val checkoutResponse = repository.checkout(
                customer, Repository.CheckoutCurrency.USD, mode
            ).single()

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
                    emit(true)
                },
                onFailure = {
                    status.postValue(
                        "Preparing checkout failed:\n${it.message}"
                    )
                    emit(false)
                }
            )

            inProgress.postValue(false)
        }

    internal class Factory(
        private val application: Application,
        private val workContext: CoroutineContext = Dispatchers.IO
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val checkoutBackendApi = BackendApiFactory(application).createCheckout()

            val repository = DefaultRepository(
                checkoutBackendApi,
                workContext
            )

            return PaymentSheetPlaygroundViewModel(
                application,
                repository
            ) as T
        }
    }
}
