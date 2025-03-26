package com.stripe.android.customersheet.data

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.SetupIntentFactory

internal class FakeCustomerSessionElementsSessionManager(
    private val ephemeralKey: Result<CachedCustomerEphemeralKey> = Result.success(
        CachedCustomerEphemeralKey(
            customerId = "cus_1",
            ephemeralKey = "ek_123",
            expiresAt = 999999,
            customerSessionClientSecret = "cuss_123",
        )
    ),
    private val intent: StripeIntent = SetupIntentFactory.create(),
    private val paymentMethods: List<PaymentMethod> = listOf(),
    private val defaultPaymentMethodId: String? = null,
    private val isPaymentMethodSyncDefaultEnabled: Boolean = false,
    private val customerSheetComponent: ElementsSession.Customer.Components.CustomerSheet =
        ElementsSession.Customer.Components.CustomerSheet.Enabled(
            isPaymentMethodRemoveEnabled = true,
            canRemoveLastPaymentMethod = true,
            isPaymentMethodSyncDefaultEnabled = isPaymentMethodSyncDefaultEnabled,
        ),
    private val customer: ElementsSession.Customer = ElementsSession.Customer(
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
        defaultPaymentMethod = defaultPaymentMethodId,
        paymentMethods = paymentMethods,
    ),
    private val elementsSession: Result<CustomerSessionElementsSession> = Result.success(
        CustomerSessionElementsSession(
            elementsSession = ElementsSession(
                linkSettings = null,
                paymentMethodSpecs = null,
                stripeIntent = intent,
                merchantCountry = null,
                isGooglePayEnabled = true,
                sessionsError = null,
                externalPaymentMethodData = null,
                customPaymentMethods = emptyList(),
                customer = customer,
                cardBrandChoice = null,
                elementsSessionId = "session_1234",
                flags = emptyMap()
            ),
            customer = customer,
            ephemeralKey = CachedCustomerEphemeralKey(
                customerId = "cus_1",
                ephemeralKey = "ek_123",
                expiresAt = 999999,
                customerSessionClientSecret = "cuss_123",
            ),
        )
    )
) : CustomerSessionElementsSessionManager {
    override suspend fun fetchCustomerSessionEphemeralKey(): Result<CachedCustomerEphemeralKey> {
        return ephemeralKey
    }

    override suspend fun fetchElementsSession(): Result<CustomerSessionElementsSession> {
        return elementsSession
    }
}
