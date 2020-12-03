package com.stripe.android.paymentsheet.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import kotlin.test.Test

class PaymentOptionFactoryTest {

    @Test
    fun `create() with GooglePay should return expected object`() {
        assertThat(
            PaymentOptionFactory().create(
                PaymentSelection.GooglePay
            )
        ).isEqualTo(
            PaymentOption(
                R.drawable.stripe_google_pay_mark,
                "Google Pay"
            )
        )
    }

    @Test
    fun `create() with card PaymentMethod should return expected object`() {
        assertThat(
            PaymentOptionFactory().create(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
        ).isEqualTo(
            PaymentOption(
                CardBrand.Visa.icon,
                "Visa"
            )
        )
    }

    @Test
    fun `create() with FPX PaymentMethod should return null`() {
        assertThat(
            PaymentOptionFactory().create(
                PaymentSelection.Saved(PaymentMethodFixtures.FPX_PAYMENT_METHOD)
            )
        ).isNull()
    }

    @Test
    fun `create() with card params should return null`() {
        assertThat(
            PaymentOptionFactory().create(
                PaymentSelection.New(PaymentMethodCreateParamsFixtures.DEFAULT_CARD)
            )
        ).isNull()
    }
}
