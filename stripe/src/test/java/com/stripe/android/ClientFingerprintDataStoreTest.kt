package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClientFingerprintDataStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = ClientFingerprintDataStore.Default(context)

    @Test
    fun getMuid_shouldReturnSameValue() {
        assertThat(store.getMuid())
            .isEqualTo(store.getMuid())
    }

    @Test
    fun getSid_whenSameSession_shouldReturnSameValue() {
        assertThat(store.getSid())
            .isEqualTo(store.getSid())
    }

    @Test
    fun getSid_whenDifferentSession_shouldReturnDifferentValue() {
        var callbacks = 0
        val store = ClientFingerprintDataStore.Default(
            context = context,
            timestampSupplier = {
                val currentTime = Calendar.getInstance().timeInMillis
                // simulate different sessions
                val timestamp = currentTime + TimeUnit.HOURS.toMillis(callbacks.toLong())
                callbacks++
                timestamp
            }
        )
        val firstSid = store.getSid()
        val secondSid = store.getSid()
        assertThat(firstSid)
            .isNotEqualTo(secondSid)
    }
}
