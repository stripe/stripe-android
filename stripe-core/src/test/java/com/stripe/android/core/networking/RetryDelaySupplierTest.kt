package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RetryDelaySupplierTest {

    @Test
    fun `getDelayMillis() should return expected value`() {
        val supplier = RetryDelaySupplier()

        // coerce to 3 remaining retries
        assertThat(
            supplier.getDelayMillis(
                maxRetries = 3,
                remainingRetries = 999
            )
        ).isEqualTo(2000L)

        assertThat(
            supplier.getDelayMillis(
                maxRetries = 3,
                remainingRetries = 3
            )
        ).isEqualTo(2000L)

        assertThat(
            supplier.getDelayMillis(
                maxRetries = 3,
                remainingRetries = 2
            )
        ).isEqualTo(4000L)

        assertThat(
            supplier.getDelayMillis(
                maxRetries = 3,
                remainingRetries = 1
            )
        ).isEqualTo(8000L)

        // coerce to 1 remaining retry
        assertThat(
            supplier.getDelayMillis(
                maxRetries = 3,
                remainingRetries = -100
            )
        ).isEqualTo(8000L)

        // coerce to 1 remaining retry
        assertThat(
            supplier.getDelayMillis(
                maxRetries = 3,
                remainingRetries = 0
            )
        ).isEqualTo(8000L)
    }
}
