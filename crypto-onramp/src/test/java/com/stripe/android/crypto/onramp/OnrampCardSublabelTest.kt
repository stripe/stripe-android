package com.stripe.android.crypto.onramp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnrampCardSublabelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `wallet card with credit funding uses Credit`() {
        val card = PaymentMethod.Card(brand = CardBrand.Visa, funding = "credit", last4 = "4242")

        assertThat(card.onrampSublabel()?.resolve(context))
            .isEqualTo("Visa Credit \u2066•••• 4242\u2069")
    }

    @Test
    fun `wallet card with debit funding uses Debit`() {
        val card = PaymentMethod.Card(brand = CardBrand.Visa, funding = "debit", last4 = "4242")

        assertThat(card.onrampSublabel()?.resolve(context))
            .isEqualTo("Visa Debit \u2066•••• 4242\u2069")
    }

    @Test
    fun `wallet card with prepaid funding uses Prepaid`() {
        val card = PaymentMethod.Card(brand = CardBrand.Visa, funding = "prepaid", last4 = "4242")

        assertThat(card.onrampSublabel()?.resolve(context))
            .isEqualTo("Visa Prepaid \u2066•••• 4242\u2069")
    }

    @Test
    fun `wallet card with missing funding uses Card`() {
        val card = PaymentMethod.Card(brand = CardBrand.Visa, funding = null, last4 = "4242")

        assertThat(card.onrampSublabel()?.resolve(context))
            .isEqualTo("Visa Card \u2066•••• 4242\u2069")
    }

    @Test
    fun `wallet card with unknown funding uses Card`() {
        val card = PaymentMethod.Card(brand = CardBrand.Visa, funding = "unknown", last4 = "4242")

        assertThat(card.onrampSublabel()?.resolve(context))
            .isEqualTo("Visa Card \u2066•••• 4242\u2069")
    }

    @Test
    fun `wallet card with future funding uses Card`() {
        val card = PaymentMethod.Card(brand = CardBrand.Visa, funding = "future_funding", last4 = "4242")

        assertThat(card.onrampSublabel()?.resolve(context))
            .isEqualTo("Visa Card \u2066•••• 4242\u2069")
    }

    @Test
    fun `wallet card without last4 has no sublabel`() {
        val card = PaymentMethod.Card(brand = CardBrand.Visa, funding = "credit", last4 = null)

        assertThat(card.onrampSublabel()).isNull()
    }
}
