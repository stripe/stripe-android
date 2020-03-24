package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClientFingerprintDataStoreTest {

    private val store = ClientFingerprintDataStore.Default(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun getMuid_shouldReturnSameValue() {
        assertThat(store.getMuid())
            .isEqualTo(store.getMuid())
    }
}
