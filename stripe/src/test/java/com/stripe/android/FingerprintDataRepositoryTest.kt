package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FingerprintDataRepositoryTest {

    private val repository: FingerprintDataRepository = FingerprintDataRepository.Default(
        ApplicationProvider.getApplicationContext<Context>()
    )

    @Test
    fun roundtrip_shouldReturnOriginalObject() {
        var fingerprintData: FingerprintData? = null
        repository.save(DATA)
        repository.get().observeForever {
            fingerprintData = it
        }
        assertThat(fingerprintData)
            .isEqualTo(DATA)
    }

    internal companion object {
        val DATA = FingerprintData(
            guid = UUID.randomUUID().toString(),
            timestamp = 500
        )
    }
}
