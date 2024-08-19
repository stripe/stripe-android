package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ExponentialBackoffRetryDelaySupplierTest {

    @Test
    fun `getDelay() should return expected value`() {
        val supplier = ExponentialBackoffRetryDelaySupplier()

        // coerce to 3 remaining retries
        assertThat(
            supplier.getDelay(
                maxRetries = 3,
                remainingRetries = 999
            )
        ).isEqualTo((2L).toDuration(DurationUnit.SECONDS))

        assertThat(
            supplier.getDelay(
                maxRetries = 3,
                remainingRetries = 3
            )
        ).isEqualTo((2L).toDuration(DurationUnit.SECONDS))

        assertThat(
            supplier.getDelay(
                maxRetries = 3,
                remainingRetries = 2
            )
        ).isEqualTo((4L).toDuration(DurationUnit.SECONDS))

        assertThat(
            supplier.getDelay(
                maxRetries = 3,
                remainingRetries = 1
            )
        ).isEqualTo((8L).toDuration(DurationUnit.SECONDS))

        // coerce to 1 remaining retry
        assertThat(
            supplier.getDelay(
                maxRetries = 3,
                remainingRetries = -100
            )
        ).isEqualTo((8L).toDuration(DurationUnit.SECONDS))

        // coerce to 1 remaining retry
        assertThat(
            supplier.getDelay(
                maxRetries = 3,
                remainingRetries = 0
            )
        ).isEqualTo((8L).toDuration(DurationUnit.SECONDS))
    }

    @Test
    fun `maxDuration() should return expected value`() {
        val supplier = ExponentialBackoffRetryDelaySupplier()

        assertThat(
            supplier.maxDuration(
                maxRetries = 3,
            )
        ).isEqualTo((14L).toDuration(DurationUnit.SECONDS))

        assertThat(
            supplier.maxDuration(
                maxRetries = 5,
            )
        ).isEqualTo((62L).toDuration(DurationUnit.SECONDS))
    }
}
