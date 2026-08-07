package com.stripe.android.paymentsheet.utils

import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.googlepaylauncher.GooglePayAvailabilityClient
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayRepository
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class GooglePayRepositoryTestRule : TestWatcher() {
    override fun starting(description: Description?) {
        super.starting(description)
        // The launcher's init analytic fires once per process, so it lands on whichever test runs
        // first and pollutes strict analytics expectations. Disable it for the whole test process.
        GooglePayPaymentMethodLauncher.setHasSentInitAnalyticEvent(hasSent = true)
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
