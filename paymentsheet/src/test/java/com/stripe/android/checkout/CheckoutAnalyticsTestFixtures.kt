package com.stripe.android.checkout

import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory

/**
 * A Checkout session response with a known value for every common analytics payload key, and the
 * payload it produces.
 */
internal object CheckoutAnalyticsTestFixtures {
    fun sessionResponse(stripeIntent: StripeIntent): CheckoutSessionResponse = CheckoutSessionResponseFactory.create(
        configId = "cfg_checkout",
        elementsSession = ElementsSession.createFromFallback(
            stripeIntent = stripeIntent,
            sessionsError = null,
        ).copy(elementsSessionConfigId = "cfg_elements"),
    )

    val sessionParams: Map<String, Any> = mapOf(
        "checkout_session_id" to DEFAULT_CHECKOUT_SESSION_ID,
        "checkout_config_id" to "cfg_checkout",
        "elements_session_config_id" to "cfg_elements",
        "currency" to "usd",
        "session_status" to "open",
        "payment_status" to "unpaid",
        "adaptive_pricing_active" to false,
        "automatic_tax_enabled" to false,
        "ordered_lpms" to "card,link",
    )
}
