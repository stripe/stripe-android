package com.stripe.android.customersheet.data

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.SetupIntentFactory

internal class FakeCustomerSessionElementsSessionManager(
    private val ephemeralKey: Result<CachedCustomerEphemeralKey.Available> = Result.success(
        CachedCustomerEphemeralKey.Available(
            customerId = "cus_1",
            ephemeralKey = "ek_123",
            expiresAt = 999999,
        )
    ),
    private val paymentMethods: List<PaymentMethod> = listOf(),
    private val customerSheetComponent: ElementsSession.Customer.Components.CustomerSheet =
        ElementsSession.Customer.Components.CustomerSheet.Enabled(
            isPaymentMethodRemoveEnabled = true,
        ),
    private val elementsSession: Result<ElementsSession> = Result.success(
        ElementsSession(
            linkSettings = null,
            paymentMethodSpecs = null,
            stripeIntent = SetupIntentFactory.create(),
            merchantCountry = null,
            isGooglePayEnabled = true,
            sessionsError = null,
            externalPaymentMethodData = null,
            customer = ElementsSession.Customer(
                session = ElementsSession.Customer.Session(
                    id = "cuss_1",
                    customerId = "cus_1",
                    apiKey = "ek_123",
                    apiKeyExpiry = 999999,
                    components = ElementsSession.Customer.Components(
                        mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
                        customerSheet = customerSheetComponent,
                    ),
                    liveMode = false,
                ),
                defaultPaymentMethod = null,
                paymentMethods = paymentMethods,
            ),
            cardBrandChoice = null,
        )
    )
) : CustomerSessionElementsSessionManager {
    override suspend fun fetchCustomerSessionEphemeralKey(): Result<CachedCustomerEphemeralKey.Available> {
        return ephemeralKey
    }

    override suspend fun fetchElementsSession(): Result<ElementsSession> {
        return elementsSession
    }
}
