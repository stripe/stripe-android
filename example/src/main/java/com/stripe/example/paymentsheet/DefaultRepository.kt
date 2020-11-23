package com.stripe.example.paymentsheet

import android.content.SharedPreferences
import com.stripe.example.service.BackendApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class DefaultRepository(
    private val backendApi: BackendApi,
    private val sharedPrefs: SharedPreferences,
    private val workContext: CoroutineContext
) : Repository {
    override fun clearKeys() {
        CoroutineScope(workContext).launch {
            sharedPrefs
                .edit()
                .clear()
                .apply()
        }
    }

    override suspend fun fetchLocalEphemeralKey() = withContext(workContext) {
        flowOf(
            sharedPrefs.getString(PREF_EK, null)?.let { ephemeralKey ->
                sharedPrefs.getString(PREF_CUSTOMER, null)?.let { customerId ->
                    PaymentSheetViewModel.EphemeralKey(ephemeralKey, customerId)
                }
            }
        )
    }

    override suspend fun fetchRemoteEphemeralKey() = withContext(workContext) {
        flowOf(
            runCatching {
                backendApi
                    .createEphemeralKey(hashMapOf("api_version" to "2020-03-02"))
                    .string()
            }.mapCatching { response ->
                PaymentSheetViewModel.EphemeralKey.fromJson(org.json.JSONObject(response))
            }.onSuccess { (key, customer) ->
                sharedPrefs.edit()
                    .putString(PREF_EK, key)
                    .putString(PREF_CUSTOMER, customer)
                    .apply()
            }
        )
    }

    private companion object {
        private const val PREF_EK = "pref_ek"
        private const val PREF_CUSTOMER = "pref_customer"
    }
}
