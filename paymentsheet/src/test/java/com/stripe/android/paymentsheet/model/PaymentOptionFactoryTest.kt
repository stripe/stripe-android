package com.stripe.android.paymentsheet.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.image.StripeImageLoader
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class PaymentOptionFactoryTest {

    private val factory = PaymentOptionFactory(
        ApplicationProvider.getApplicationContext<Context>().resources,
        StripeImageLoader(ApplicationProvider.getApplicationContext())
    )

    @Test
    fun `create() with GooglePay should return expected object`() {
        assertThat(
            factory.create(
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
            factory.create(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
        ).isEqualTo(
            PaymentOption(
                R.drawable.stripe_ic_paymentsheet_card_visa,
                "····4242"
            )
        )
    }

    @Test
    fun `create() with card params should return expected object`() {
        assertThat(
            factory.create(
                PaymentSelection.New.Card(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    brand = CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
                )
            )
        ).isEqualTo(
            PaymentOption(
                R.drawable.stripe_ic_paymentsheet_card_visa,
                "····4242"
            )
        )
    }
}
