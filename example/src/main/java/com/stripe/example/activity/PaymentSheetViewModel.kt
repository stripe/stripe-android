package com.stripe.example.activity

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.liveData
import com.stripe.example.module.StripeIntentViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class PaymentSheetViewModel(
    application: Application,
    private val sharedPrefs: SharedPreferences
) : StripeIntentViewModel(application) {

    fun clearKeys() {
        CoroutineScope(workContext).launch {
            sharedPrefs
                .edit()
                .clear()
                .apply()
        }
    }

    fun fetchEphemeralKey() = liveData {
        withContext(workContext) {
            emit(
                fetchLocalEphemeralKey().firstOrNull() ?: fetchRemoteEphemeralKey().firstOrNull()
            )
        }
    }

    private suspend fun fetchLocalEphemeralKey() = flowOf(
        sharedPrefs.getString(PREF_EK, null)?.let { ephemeralKey ->
            sharedPrefs.getString(PREF_CUSTOMER, null)?.let { customerId ->
                EphemeralKey(ephemeralKey, customerId)
            }
        }
    )

    private suspend fun fetchRemoteEphemeralKey() = flow {
        inProgress.postValue(true)
        status.postValue("Fetching ephemeral key")

        val ephemeralKeyResult = runCatching {
            backendApi
                .createEphemeralKey(hashMapOf("api_version" to "2020-03-02"))
                .string()
        }.mapCatching { response ->
            EphemeralKey.fromJson(JSONObject(response))
        }

        ephemeralKeyResult.fold(
            onSuccess = { (key, customer) ->
                // TODO: create separate endpoint that only sends necessary info

                status.postValue(
                    "${status.value}\n\nFetched key $key for customer $customer"
                )

                sharedPrefs.edit()
                    .putString(PREF_EK, key)
                    .putString(PREF_CUSTOMER, customer)
                    .apply()
            },
            onFailure = {
                status.postValue(
                    "${status.value}\n\nFetching ephemeral key failed\n${it.message}"
                )
            }
        )
        inProgress.postValue(false)

        emit(ephemeralKeyResult.getOrNull())
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

    private companion object {
        private const val PREF_EK = "pref_ek"
        private const val PREF_CUSTOMER = "pref_customer"
    }
}
