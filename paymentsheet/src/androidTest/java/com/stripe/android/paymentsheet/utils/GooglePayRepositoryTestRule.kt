package com.stripe.android.paymentsheet.utils

import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.googlepaylauncher.GooglePayAvailabilityClient
import com.stripe.android.googlepaylauncher.GooglePayRepository
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class GooglePayRepositoryTestRule : TestWatcher() {
    override fun starting(description: Description?) {
        super.starting(description)
        GooglePayRepository.googlePayAvailabilityClientFactory = createFakeGooglePayAvailabilityClient()
    }

    override fun finished(description: Description?) {
        GooglePayRepository.resetFactory()
        super.finished(description)
    }

    private fun createFakeGooglePayAvailabilityClient(): GooglePayAvailabilityClient.Factory {
        return object : GooglePayAvailabilityClient.Factory {
            override fun create(paymentsClient: PaymentsClient): GooglePayAvailabilityClient {
                return object : GooglePayAvailabilityClient {
                    override suspend fun isReady(request: IsReadyToPayRequest): Boolean {
                        return true
                    }
                }
            }
        }
    }
}
