package com.stripe.android.customersheet.data

import com.stripe.android.model.ElementsSession

internal class FakeCustomerSessionElementsSessionManager(
    private val ephemeralKey: Result<CachedCustomerEphemeralKey> = Result.success(
        CachedCustomerEphemeralKey(
            customerId = "cus_1",
            ephemeralKey = "ek_123",
            expiresAt = 999999,
        )
    ),
) : CustomerSessionElementsSessionManager {
    override suspend fun fetchCustomerSessionEphemeralKey(): Result<CachedCustomerEphemeralKey> {
        return ephemeralKey
    }

    override suspend fun fetchElementsSession(): Result<ElementsSession> {
        throw NotImplementedError("Not implemented yet!")
    }
}
