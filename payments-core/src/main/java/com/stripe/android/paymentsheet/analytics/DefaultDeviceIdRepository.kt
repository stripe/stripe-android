package com.stripe.android.paymentsheet.analytics

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.stripe.android.payments.core.injection.IOContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultDeviceIdRepository @Inject internal constructor(
    private val context: Context,
    @IOContext private val workContext: CoroutineContext
) : DeviceIdRepository {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    private val mutex = Mutex()

    override suspend fun get(): DeviceId = withContext(workContext) {
        mutex.withLock {
            val deviceIdValue = prefs.getString(KEY_DEVICE_ID, null)
            if (deviceIdValue != null) {
                DeviceId(deviceIdValue)
            } else {
                createDeviceId()
            }
        }
    }

    private fun createDeviceId(): DeviceId {
        val deviceId = DeviceId()
        prefs.edit(commit = true) {
            putString(KEY_DEVICE_ID, deviceId.value)
        }
        return deviceId
    }

    private companion object {
        private const val PREF_FILE = "DefaultDeviceIdRepository"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
