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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.single
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetViewModel(
    application: Application,
    private val repository: Repository
) : AndroidViewModel(application) {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()

    fun prepareCheckout(customer: Repository.CheckoutCustomer, mode: Repository.CheckoutMode) =
        liveData {
            inProgress.postValue(true)
            status.postValue("Preparing checkout...")

            val checkoutResponse = repository.checkout(
                customer, Repository.CheckoutCurrency.USD, mode
            ).single()

            checkoutResponse.fold(
                onSuccess = { response ->
                    status.postValue(
                        "${status.value}\n\nReady to checkout: $response"
                    )
                },
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
        private val application: Application,
        private val workContext: CoroutineContext = Dispatchers.IO
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val checkoutBackendApi = BackendApiFactory(application).createCheckout()

            val repository = DefaultRepository(
                checkoutBackendApi,
                workContext
            )

            return PaymentSheetViewModel(
                application,
                repository
            ) as T
        }
    }
}
