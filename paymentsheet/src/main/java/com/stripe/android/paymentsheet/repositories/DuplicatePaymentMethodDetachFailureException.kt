package com.stripe.android.paymentsheet.repositories

internal class DuplicatePaymentMethodDetachFailureException(
    val failures: List<DuplicateDetachFailure>
) : Exception() {
    override val message: String = "Failed to detach the following duplicates:${
        failures.map { failure ->
            "\n - (paymentMethodId: ${failure.paymentMethodId}, " +
                "reason: ${failure.exception.message ?: "Unknown reason"})"
        }
    }"

    internal class DuplicateDetachFailure(
        val paymentMethodId: String,
        val exception: Throwable
    )
}
