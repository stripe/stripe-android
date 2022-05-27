package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaymentMethodRequirementsTest {
    @Test
    fun `Customer supported confirm is only available for hard coded lpms`() {
        assertThat(
            PaymentMethodRequirements(
                null,
                null,
                true
            ).getConfirmPMFromCustomer("unknown")
        ).isFalse()

        assertThat(
            PaymentMethodRequirements(
                null,
                null,
                true
            ).getConfirmPMFromCustomer("card")
        ).isTrue()
    }
}
