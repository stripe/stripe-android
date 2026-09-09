package com.stripe.android.paymentsheet.example.playground.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class DefaultBillingAddressSettingsDefinitionTest {
    @Test
    fun `custom email without phone survives serialization`() {
        val billingAddress = DefaultBillingAddress.WithEmailAndNoPhone("email_123@example.com")

        val serialized = DefaultBillingAddressSettingsDefinition.convertToString(billingAddress)
        val restored = DefaultBillingAddressSettingsDefinition.convertToValue(serialized)

        assertThat(restored).isEqualTo(billingAddress)
    }
}
