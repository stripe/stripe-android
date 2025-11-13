package com.stripe.android.paymentsheet.utils

import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.googlepaylauncher.GooglePayAvailabilityClient
import com.stripe.android.googlepaylauncher.GooglePayRepository
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

internal class FakeGooglePayRepositoryRule() : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws
            override fun evaluate() {
                GooglePayRepository.googlePayAvailabilityClientFactory = object : GooglePayAvailabilityClient.Factory {
                    override fun create(paymentsClient: PaymentsClient): GooglePayAvailabilityClient {
                        return object : GooglePayAvailabilityClient {
                            override suspend fun isReady(request: IsReadyToPayRequest): Boolean {
                                return true
                            }
                        }
                    }
                }
                try {
                    base.evaluate()
                } finally {
                    GooglePayRepository.resetFactory()
                }
            }
        }
    }
}
