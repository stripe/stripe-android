package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryCode
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CardPaymentDetailTest {

    private val card = ConsumerPaymentDetails.Card(
        id = "QAAAKIL",
        last4 = "4242",
        expiryYear = 2500,
        expiryMonth = 4,
        brand = CardBrand.Visa,
        cvcCheck = CvcCheck.Fail,
        isDefault = false,
        billingAddress = ConsumerPaymentDetails.BillingAddress(
            countryCode = CountryCode.US,
            postalCode = "42424"
        )
    )

    @Test
    fun `isExpired should be true when expiry date is in the past`() {
        assertThat(card.copy(expiryYear = 2005).isExpired).isTrue()
    }

    @Test
    fun `isExpired should be false when expiry date is in the past`() {
        assertThat(card.isExpired).isFalse()
    }

    @Test
    fun `requiresCardDetailsRecollection should be true when isExpired and cvc needs collection`() {
        val cvcCheck: CvcCheck = mock()
        whenever(cvcCheck.requiresRecollection).thenReturn(true)

        assertThat(
            card.copy(
                cvcCheck = cvcCheck,
                expiryYear = 2005
            ).requiresCardDetailsRecollection
        ).isTrue()
    }

    @Test
    fun `requiresCardDetailsRecollection should be true when isExpired and cvc does not require collection`() {
        val cvcCheck: CvcCheck = mock()
        whenever(cvcCheck.requiresRecollection).thenReturn(false)

        assertThat(
            card.copy(
                cvcCheck = cvcCheck,
                expiryYear = 2005
            ).requiresCardDetailsRecollection
        ).isTrue()
    }

    @Test
    fun `requiresCardDetailsRecollection should be true when isNotExpired and cvc requires collection`() {
        val cvcCheck: CvcCheck = mock()
        whenever(cvcCheck.requiresRecollection).thenReturn(true)

        assertThat(
            card.copy(
                cvcCheck = cvcCheck,
            ).requiresCardDetailsRecollection
        ).isTrue()
    }

    @Test
    fun `requiresCardDetailsRecollection should be false when isNotExpired and cvc does not require collection`() {
        val cvcCheck: CvcCheck = mock()
        whenever(cvcCheck.requiresRecollection).thenReturn(false)

        assertThat(
            card.copy(
                cvcCheck = cvcCheck,
            ).requiresCardDetailsRecollection
        ).isFalse()
    }
}
