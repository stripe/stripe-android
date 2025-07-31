package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.Address
import org.junit.Test

internal class PaymentSheetBillingDetailsTest {
    @Test
    fun `isFilledOut is false when all values are null`() {
        assertThat(PaymentSheet.BillingDetails().isFilledOut()).isFalse()
    }

    @Test
    fun `isFilledOut is true when any value is not null`() {
        assertThat(PaymentSheet.BillingDetails(address = Address()).isFilledOut()).isTrue()
        assertThat(PaymentSheet.BillingDetails(email = "example@test.com").isFilledOut()).isTrue()
        assertThat(PaymentSheet.BillingDetails(name = "Jane Doe").isFilledOut()).isTrue()
        assertThat(PaymentSheet.BillingDetails(phone = "1234567890").isFilledOut()).isTrue()
    }
}
