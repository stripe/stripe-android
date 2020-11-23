package com.stripe.example.paymentsheet

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.stripe.example.module.BackendApiFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetViewModel(
    private val repository: Repository
) : ViewModel() {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()

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

    internal data class EphemeralKey(
        val key: String,
        val customer: String
    ) {
        companion object {
            fun fromJson(json: JSONObject): EphemeralKey {
                val secret = json.getString("secret")
                val associatedObjectArray = json.getJSONArray("associated_objects")
                val typeObject = associatedObjectArray.getJSONObject(0)
                val customerId = typeObject.getString("id")

                return EphemeralKey(
                    key = secret,
                    customer = customerId
                )
            }
        }
    }

    internal class Factory(
        private val application: Application,
        private val sharedPrefs: SharedPreferences,
        private val workContext: CoroutineContext = Dispatchers.IO
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val backendApi = BackendApiFactory(application).create()

            val repository = DefaultRepository(
                backendApi,
                sharedPrefs,
                workContext
            )

            return PaymentSheetViewModel(
                repository
            ) as T
        }
    }
}
