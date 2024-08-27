package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ElementsSessionTest {
    @Test
    fun `on link enabled without passthrough mode, link mode should be payment method`() {
        val elementsSession = createElementsSession(
            paymentMethodTypes = listOf("card", "link"),
            linkFundingSources = listOf("card", "bank_account"),
            isPassthroughModeEnabled = false,
        )

        assertThat(elementsSession.isLinkEnabled).isTrue()
        assertThat(elementsSession.linkMode).isEqualTo(LinkMode.PaymentMethod)
    }

    @Test
    fun `on link disabled because payment method types does have 'link', link mode should be null`() {
        val elementsSession = createElementsSession(
            paymentMethodTypes = listOf("card"),
            linkFundingSources = listOf("card", "bank_account"),
            isPassthroughModeEnabled = false,
        )

        assertThat(elementsSession.isLinkEnabled).isFalse()
        assertThat(elementsSession.linkMode).isNull()
    }

    @Test
    fun `on link disabled because no valid funding sources, link mode should be null`() {
        val elementsSession = createElementsSession(
            paymentMethodTypes = listOf("card", "link"),
            linkFundingSources = listOf(),
            isPassthroughModeEnabled = false,
        )

        assertThat(elementsSession.isLinkEnabled).isFalse()
        assertThat(elementsSession.linkMode).isNull()
    }

    @Test
    fun `on link enabled with passthrough mode, link mode should be passthrough`() {
        val elementsSession = createElementsSession(
            paymentMethodTypes = listOf("card"),
            linkFundingSources = listOf(),
            isPassthroughModeEnabled = true,
        )

        assertThat(elementsSession.isLinkEnabled).isTrue()
        assertThat(elementsSession.linkMode).isEqualTo(LinkMode.Passthrough)
    }

    private fun createElementsSession(
        paymentMethodTypes: List<String>,
        linkFundingSources: List<String>,
        isPassthroughModeEnabled: Boolean,
    ): ElementsSession {
        return ElementsSession(
            linkSettings = if (isPassthroughModeEnabled) {
                ElementsSession.LinkSettings(
                    linkPassthroughModeEnabled = true,
                    linkFlags = mapOf(),
                    linkFundingSources = linkFundingSources,
                    disableLinkSignup = false
                )
            } else {
                null
            },
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = paymentMethodTypes,
                linkFundingSources = linkFundingSources,
            ),
            customer = null,
            paymentMethodSpecs = null,
            isGooglePayEnabled = true,
            isEligibleForCardBrandChoice = false,
            externalPaymentMethodData = null,
            merchantCountry = null,
        )
    }
}
