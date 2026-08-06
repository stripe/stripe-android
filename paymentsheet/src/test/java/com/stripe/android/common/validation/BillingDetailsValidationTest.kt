package com.stripe.android.common.validation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import kotlin.test.Test

class BillingDetailsValidationTest {

    private fun card(
        line1: String? = "123 Main St",
        city: String? = "San Francisco",
        state: String? = "CA",
        postalCode: String? = "94111",
        country: String? = "US",
        hasAddress: Boolean = true,
    ): PaymentMethod = PaymentMethodFactory.card(id = "pm_test").copy(
        billingDetails = PaymentMethod.BillingDetails(
            address = if (hasAddress) {
                Address(
                    line1 = line1,
                    city = city,
                    state = state,
                    postalCode = postalCode,
                    country = country,
                )
            } else {
                null
            },
        ),
    )

    @Test
    fun `null address is insufficient`() {
        assertThat(card(hasAddress = false).hasSufficientBillingDetailsForAutomaticTax()).isFalse()
    }

    @Test
    fun `missing country is insufficient`() {
        assertThat(card(country = null).hasSufficientBillingDetailsForAutomaticTax()).isFalse()
    }

    @Test
    fun `country with no additional tax requirement needs only the country`() {
        val pm = card(country = "FR", line1 = null, city = null, state = null, postalCode = null)
        assertThat(pm.hasSufficientBillingDetailsForAutomaticTax()).isTrue()
    }

    @Test
    fun `CA GB IN require postal code only`() {
        assertThat(card(country = "CA", postalCode = null).hasSufficientBillingDetailsForAutomaticTax()).isFalse()
        assertThat(card(country = "CA", postalCode = "K1A0B1").hasSufficientBillingDetailsForAutomaticTax()).isTrue()
        assertThat(card(country = "GB", postalCode = null).hasSufficientBillingDetailsForAutomaticTax()).isFalse()
        assertThat(card(country = "IN", postalCode = null).hasSufficientBillingDetailsForAutomaticTax()).isFalse()
    }

    @Test
    fun `PR requires line1, city, and postal code but not state`() {
        val missingCity = card(country = "PR", city = null)
        assertThat(missingCity.hasSufficientBillingDetailsForAutomaticTax()).isFalse()

        val completeWithoutState = card(country = "PR", state = null)
        assertThat(completeWithoutState.hasSufficientBillingDetailsForAutomaticTax()).isTrue()
    }

    @Test
    fun `US requires line1, city, state, and postal code`() {
        assertThat(card(country = "US", state = null).hasSufficientBillingDetailsForAutomaticTax()).isFalse()
        assertThat(card(country = "US").hasSufficientBillingDetailsForAutomaticTax()).isTrue()
    }
}
