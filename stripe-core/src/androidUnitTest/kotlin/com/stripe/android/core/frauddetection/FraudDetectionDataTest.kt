package com.stripe.android.core.frauddetection

import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class FraudDetectionDataTest {

    @Test
    fun `isExpired() when less than 30 minutes have elapsed should return false`() {
        val fraudDetectionData = createFraudDetectionData()
        assertThat(
            fraudDetectionData.isExpired(
                currentTime = fraudDetectionData.timestamp + TimeUnit.MINUTES.toMillis(29L)
            )
        ).isFalse()
    }

    @Test
    fun `isExpired() when more than 30 minutes have elapsed should return true`() {
        val fraudDetectionData = createFraudDetectionData()
        assertThat(
            fraudDetectionData.isExpired(
                currentTime = fraudDetectionData.timestamp + TimeUnit.MINUTES.toMillis(31L)
            )
        ).isTrue()
    }

    private companion object {
        fun createFraudDetectionData(elapsedTime: Long = 0L): FraudDetectionData {
            return FraudDetectionDataFixtures.create(
                Calendar.getInstance().timeInMillis + TimeUnit.MINUTES.toMillis(elapsedTime)
            )
        }
    }
}
