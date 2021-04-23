package com.stripe.example.paymentsheet

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.stripe.example.module.BackendApiFactory
import com.stripe.example.module.StripeIntentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetViewModel(
    application: Application,
    private val repository: Repository
) : StripeIntentViewModel(application) {

    fun clearKeys() {
        repository.clearKeys()
    }

    fun fetchEphemeralKey() = liveData {
        val localEphemeralKey = repository.fetchLocalEphemeralKey().singleOrNull()
        if (localEphemeralKey != null) {
            emit(localEphemeralKey)
        } else {
            inProgress.postValue(true)
            status.postValue("Fetching ephemeral key")

            val remoteEphemeralKeyResult = repository.fetchRemoteEphemeralKey().single()
            remoteEphemeralKeyResult.fold(
                onSuccess = { (key, customer) ->
                    // TODO: create separate endpoint that only sends necessary info

                    status.postValue(
                        "${status.value}\n\nFetched key $key for customer $customer"
                    )
                },
                onFailure = {
                    status.postValue(
                        "${status.value}\n\nFetching ephemeral key failed\n${it.message}"
                    )
                }
            )

            inProgress.postValue(false)
            emit(remoteEphemeralKeyResult.getOrNull())
        }
    }

    fun prepareCheckout(customer: String, mode: String) = liveData {
        inProgress.postValue(true)
        status.postValue("Preparing checkout...")

        val checkoutResponse = repository.checkout(
            customer, CURRENCY, mode
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
        private val sharedPrefs: SharedPreferences,
        private val workContext: CoroutineContext = Dispatchers.IO
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val backendApi = BackendApiFactory(application).create()
            val checkoutBackendApi = BackendApiFactory(application).createCheckout()

            val repository = DefaultRepository(
                backendApi,
                checkoutBackendApi,
                sharedPrefs,
                workContext
            )

            return PaymentSheetViewModel(
                application,
                repository
            ) as T
        }
    }

    private companion object {
        private const val CURRENCY = "usd"
    }
}
