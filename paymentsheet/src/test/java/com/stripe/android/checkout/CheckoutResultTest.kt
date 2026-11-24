@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutElementsController.Result
import com.stripe.android.checkout.CheckoutElementsController.Session.Status.PaymentStatus
import org.junit.Test

class CheckoutResultTest {
    @Test
    fun `completed results with the same payment status have value equality`() {
        val first = Result.Completed(PaymentStatus.Paid)
        val second = Result.Completed(PaymentStatus.Paid)

        assertThat(first).isNotSameInstanceAs(second)
        assertThat(first).isEqualTo(second)
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
    }

    @Test
    fun `completed results with different payment statuses are distinct`() {
        val paid = Result.Completed(PaymentStatus.Paid)
        val unpaid = Result.Completed(PaymentStatus.Unpaid)

        assertThat(paid).isNotEqualTo(unpaid)
    }
}
