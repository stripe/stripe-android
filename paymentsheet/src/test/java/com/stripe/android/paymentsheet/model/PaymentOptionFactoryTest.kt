package com.stripe.android.paymentsheet.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
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

    @Test
    fun `create() with saved card params with known brand from wallet should return expected object`() {
        assertThat(
            factory.create(
                PaymentSelection.Saved(
                    paymentMethod = card(CardBrand.Visa),
                    walletType = PaymentSelection.Saved.WalletType.GooglePay
                )
            )
        ).isEqualTo(
            PaymentOption(
                R.drawable.stripe_ic_paymentsheet_card_visa,
                "····4242"
            )
        )
    }

    @Test
    fun `create() with saved card params with unknown brand from Link wallet should return expected object`() {
        assertThat(
            factory.create(
                PaymentSelection.Saved(
                    paymentMethod = card(),
                    walletType = PaymentSelection.Saved.WalletType.Link
                )
            )
        ).isEqualTo(
            PaymentOption(
                R.drawable.stripe_ic_paymentsheet_link,
                "····4242"
            )
        )
    }

    @Test
    fun `create() with saved card params without last 4 digits from Link wallet should return expected object`() {
        assertThat(
            factory.create(
                PaymentSelection.Saved(
                    paymentMethod = card(last4 = null),
                    walletType = PaymentSelection.Saved.WalletType.Link
                )
            )
        ).isEqualTo(
            PaymentOption(
                R.drawable.stripe_ic_paymentsheet_link,
                "Link"
            )
        )
    }

    @Test
    fun `create() with saved card params with unknown brand from Google wallet should return expected object`() {
        assertThat(
            factory.create(
                PaymentSelection.Saved(
                    paymentMethod = card(),
                    walletType = PaymentSelection.Saved.WalletType.GooglePay
                )
            )
        ).isEqualTo(
            PaymentOption(
                R.drawable.stripe_google_pay_mark,
                "····4242"
            )
        )
    }

    @Test
    fun `create() with saved card params without last 4 digits from Google wallet should return expected object`() {
        assertThat(
            factory.create(
                PaymentSelection.Saved(
                    paymentMethod = card(last4 = null),
                    walletType = PaymentSelection.Saved.WalletType.GooglePay
                )
            )
        ).isEqualTo(
            PaymentOption(
                R.drawable.stripe_google_pay_mark,
                "Google Pay"
            )
        )
    }

    private fun card(
        brand: CardBrand = CardBrand.Unknown,
        last4: String? = "4242"
    ): PaymentMethod {
        return PaymentMethod.Builder()
            .setId("pm_1")
            .setCode("card")
            .setType(PaymentMethod.Type.Card)
            .setCard(PaymentMethod.Card(last4 = last4, brand = brand, displayBrand = brand.code))
            .build()
    }
}
