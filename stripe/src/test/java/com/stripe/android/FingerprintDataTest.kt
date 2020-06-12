package com.stripe.android

import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.junit.Test

class FingerprintDataTest {

    @Test
    fun `isExpired() when less than 30 minutes have elapsed should return false`() {
        val fingerprintData = createFingerprintData()
        assertThat(
            fingerprintData.isExpired(
                currentTime = fingerprintData.timestamp + TimeUnit.MINUTES.toMillis(29L)
            )
        ).isFalse()
    }

    @Test
    fun `isExpired() when more than 30 minutes have elapsed should return true`() {
        val fingerprintData = createFingerprintData()
        assertThat(
            fingerprintData.isExpired(
                currentTime = fingerprintData.timestamp + TimeUnit.MINUTES.toMillis(31L)
            )
        ).isTrue()
    }

    private companion object {
        fun createFingerprintData(elapsedTime: Long = 0L): FingerprintData {
            return FingerprintDataFixtures.create(
                Calendar.getInstance().timeInMillis + TimeUnit.MINUTES.toMillis(elapsedTime)
            )
        }
    }
}
