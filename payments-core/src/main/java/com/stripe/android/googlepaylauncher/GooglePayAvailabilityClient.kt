package com.stripe.android.googlepaylauncher

import androidx.annotation.RestrictTo
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import kotlinx.coroutines.tasks.await

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface GooglePayAvailabilityClient {
    suspend fun isReady(request: IsReadyToPayRequest): Boolean

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface Factory {
        fun create(paymentsClient: PaymentsClient): GooglePayAvailabilityClient
    }
}

internal class DefaultGooglePayAvailabilityClient internal constructor(
    private val paymentsClient: PaymentsClient
) : GooglePayAvailabilityClient {
    override suspend fun isReady(request: IsReadyToPayRequest): Boolean {
        return paymentsClient.isReadyToPay(request).await()
    }

    class Factory : GooglePayAvailabilityClient.Factory {
        override fun create(paymentsClient: PaymentsClient): GooglePayAvailabilityClient {
            return DefaultGooglePayAvailabilityClient(paymentsClient)
        }
    }
}
