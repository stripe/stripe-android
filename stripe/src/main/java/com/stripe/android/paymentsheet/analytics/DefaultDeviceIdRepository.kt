package com.stripe.android.paymentsheet.analytics

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class DefaultDeviceIdRepository(
    private val context: Context,
    private val workContext: CoroutineContext
) : DeviceIdRepository {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    override suspend fun get(): DeviceId = withContext(workContext) {
        val deviceIdValue = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceIdValue != null) {
            DeviceId(deviceIdValue)
        } else {
            createDeviceId()
        }
    }

    private fun createDeviceId(): DeviceId {
        val deviceId = DeviceId()
        prefs.edit {
            putString(KEY_DEVICE_ID, deviceId.value)
        }
        return deviceId
    }

    private companion object {
        private const val PREF_FILE = "pref_file"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
