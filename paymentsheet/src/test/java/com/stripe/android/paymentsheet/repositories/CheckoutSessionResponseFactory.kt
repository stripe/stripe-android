package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent

internal object CheckoutSessionResponseFactory {
    fun create(
        id: String = "cs_test_abc123",
        amount: Long = 1000L,
        currency: String = "usd",
        mode: CheckoutSessionResponse.Mode = CheckoutSessionResponse.Mode.PAYMENT,
        customerEmail: String? = null,
        elementsSession: ElementsSession? = null,
        paymentIntent: PaymentIntent? = null,
        setupIntent: SetupIntent? = null,
        customer: CheckoutSessionResponse.Customer? = null,
        savedPaymentMethodsOfferSave: CheckoutSessionResponse.SavedPaymentMethodsOfferSave? = null,
        totalSummary: CheckoutSessionResponse.TotalSummaryResponse? = null,
        lineItems: List<CheckoutSessionResponse.LineItem> = emptyList(),
        shippingOptions: List<CheckoutSessionResponse.ShippingRate> = emptyList(),
    ): CheckoutSessionResponse {
        return CheckoutSessionResponse(
            id = id,
            amount = amount,
            currency = currency,
            mode = mode,
            customerEmail = customerEmail,
            elementsSession = elementsSession,
            paymentIntent = paymentIntent,
            setupIntent = setupIntent,
            customer = customer,
            savedPaymentMethodsOfferSave = savedPaymentMethodsOfferSave,
            totalSummary = totalSummary,
            lineItems = lineItems,
            shippingOptions = shippingOptions,
        )
    }
}
