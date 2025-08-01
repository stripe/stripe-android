package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.Address
import com.stripe.android.elements.BillingDetails
import org.junit.Test

internal class PaymentSheetBillingDetailsTest {
    @Test
    fun `isFilledOut is false when all values are null`() {
        assertThat(BillingDetails().isFilledOut()).isFalse()
    }

    @Test
    fun `isFilledOut is true when any value is not null`() {
        assertThat(BillingDetails(address = Address()).isFilledOut()).isTrue()
        assertThat(BillingDetails(email = "example@test.com").isFilledOut()).isTrue()
        assertThat(BillingDetails(name = "Jane Doe").isFilledOut()).isTrue()
        assertThat(BillingDetails(phone = "1234567890").isFilledOut()).isTrue()
    }
}
