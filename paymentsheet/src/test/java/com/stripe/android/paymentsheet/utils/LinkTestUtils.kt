package com.stripe.android.paymentsheet.utils

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

object LinkTestUtils {
    val LINK_SAVED_PAYMENT_DETAILS = LinkPaymentDetails.Saved(
        paymentDetails = ConsumerPaymentDetails.Card(
            id = "pm_123",
            last4 = "4242",
        ),
        paymentMethodCreateParams = mock(),
    )

    val LINK_NEW_PAYMENT_DETAILS = LinkPaymentDetails.New(
        paymentDetails = ConsumerPaymentDetails.Card(
            id = "pm_123",
            last4 = "4242",
        ),
        paymentMethodCreateParams = mock(),
        originalParams = mock()
    )

    fun createLinkConfiguration(): LinkConfiguration {
        return LinkConfiguration(
            stripeIntent = mock {
                on { linkFundingSources } doReturn listOf(
                    PaymentMethod.Type.Card.code
                )
            },
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            customerInfo = LinkConfiguration.CustomerInfo(null, null, null, null),
            flags = mapOf(),
            merchantName = "Test merchant inc.",
            merchantCountryCode = "US",
            passthroughModeEnabled = false,
            shippingValues = mapOf(),
        )
    }
}
