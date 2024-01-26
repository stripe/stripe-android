package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class LinearRetryDelaySupplierTest {
    @Test
    fun `getDelay() should return expected value`() {
        val supplier = LinearRetryDelaySupplier()

        // coerce to 3 remaining retries
        assertThat(
            supplier.getDelay(
                maxRetries = 3,
                remainingRetries = 999
            )
        ).isEqualTo((3L).toDuration(DurationUnit.SECONDS))

        assertThat(
            supplier.getDelay(
                maxRetries = 3,
                remainingRetries = 3
            )
        ).isEqualTo((3L).toDuration(DurationUnit.SECONDS))

        assertThat(
            supplier.getDelay(
                maxRetries = 3,
                remainingRetries = 2
            )
        ).isEqualTo((3L).toDuration(DurationUnit.SECONDS))

        assertThat(
            supplier.getDelay(
                maxRetries = 3,
                remainingRetries = 1
            )
        ).isEqualTo((3L).toDuration(DurationUnit.SECONDS))

        // coerce to 1 remaining retry
        assertThat(
            supplier.getDelay(
                maxRetries = 3,
                remainingRetries = -100
            )
        ).isEqualTo((3L).toDuration(DurationUnit.SECONDS))

        // coerce to 1 remaining retry
        assertThat(
            supplier.getDelay(
                maxRetries = 3,
                remainingRetries = 0
            )
        ).isEqualTo((3L).toDuration(DurationUnit.SECONDS))
    }
}
