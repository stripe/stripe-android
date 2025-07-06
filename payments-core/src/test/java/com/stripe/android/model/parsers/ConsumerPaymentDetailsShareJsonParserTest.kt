package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConsumerFixtures
import com.stripe.android.model.ConsumerPaymentDetailsShare
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test

internal class ConsumerPaymentDetailsShareJsonParserTest {
    @Test
    fun `parse result`() {
        assertThat(
            ConsumerPaymentDetailsShareJsonParser
                .parse(ConsumerFixtures.PAYMENT_DETAILS_SHARE_JSON)
        ).isEqualTo(
            ConsumerPaymentDetailsShare(
                PaymentMethodFactory.card(id = "pm_1NsnWALu5o3P18Zp36Q7YfWW")
            )
        )
    }
}
