package com.stripe.android.core.networking

import com.google.common.truth.Truth
import org.junit.Test

class LinearRetryDelaySupplierTest {
    @Test
    fun `getDelayMillis() should return expected value`() {
        val supplier = LinearRetryDelaySupplier()

        // coerce to 3 remaining retries
        Truth.assertThat(
            supplier.getDelayMillis(
                maxRetries = 3,
                remainingRetries = 999
            )
        ).isEqualTo(3000L)

        Truth.assertThat(
            supplier.getDelayMillis(
                maxRetries = 3,
                remainingRetries = 3
            )
        ).isEqualTo(3000L)

        Truth.assertThat(
            supplier.getDelayMillis(
                maxRetries = 3,
                remainingRetries = 2
            )
        ).isEqualTo(3000L)

        Truth.assertThat(
            supplier.getDelayMillis(
                maxRetries = 3,
                remainingRetries = 1
            )
        ).isEqualTo(3000L)

        // coerce to 1 remaining retry
        Truth.assertThat(
            supplier.getDelayMillis(
                maxRetries = 3,
                remainingRetries = -100
            )
        ).isEqualTo(3000L)

        // coerce to 1 remaining retry
        Truth.assertThat(
            supplier.getDelayMillis(
                maxRetries = 3,
                remainingRetries = 0
            )
        ).isEqualTo(3000L)
    }
}
