package com.stripe.android.paymentsheet.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentOptionLabelsFactoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `create with card payment selection returns correct labels`() {
        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        )

        assertThat(labels.label).isEqualTo("Visa")
        assertThat(labels.sublabel).isEqualTo("···· 4242")
    }

    @Test
    fun `create with generic payment selection returns correct labels`() {
        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentMethodFixtures.GENERIC_PAYMENT_SELECTION
        )

        assertThat(labels.label).isEqualTo("PayPal")
        assertThat(labels.sublabel).isNull()
    }

    @Test
    fun `create with US Bank Account payment selection returns correct labels`() {
        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION
        )

        assertThat(labels.label).isEqualTo("Stripe Bank Account")
        assertThat(labels.sublabel).isEqualTo("···· 6789")
    }

    @Test
    fun `create with US Bank Account payment selection without bank name returns correct labels`() {
        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION_WITHOUT_BANK_NAME
        )

        assertThat(labels.label).isEqualTo("Bank")
        assertThat(labels.sublabel).isEqualTo("···· 6789")
    }

    @Test
    fun `create with Link Inline payment selection returns correct labels`() {
        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentMethodFixtures.LINK_INLINE_PAYMENT_SELECTION
        )

        assertThat(labels.label).isEqualTo("Visa")
        assertThat(labels.sublabel).isNotEmpty()
    }

    @Test
    fun `create with saved Link payment method returns correct labels`() {
        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.LINK_PAYMENT_METHOD
            )
        )

        assertThat(labels.label).isEqualTo("Link")
        assertThat(labels.sublabel).isEqualTo("Visa Credit •••• 4242")
    }

    @Test
    fun `create with saved card payment method returns correct labels`() {
        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        )

        assertThat(labels.label).isEqualTo("Visa")
        assertThat(labels.sublabel).isEqualTo("···· 4242")
    }

    @Test
    fun `create with saved US Bank Account returns correct labels`() {
        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT
            )
        )

        assertThat(labels.label).isEqualTo("STRIPE TEST BANK")
        assertThat(labels.sublabel).isNotEmpty()
    }

    @Test
    fun `create with saved card without brand returns correct labels`() {
        val cardWithoutBrand = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD.copy(
                brand = CardBrand.Unknown
            )
        )

        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentSelection.Saved(
                paymentMethod = cardWithoutBrand
            )
        )

        assertThat(labels.label).isEqualTo("···· 4242")
        assertThat(labels.sublabel).isNull()
    }

    @Test
    fun `create with saved US Bank Account without bank name returns correct labels`() {
        val bankAccountWithoutName = PaymentMethodFixtures.US_BANK_ACCOUNT.copy(
            usBankAccount = PaymentMethodFixtures.US_BANK_ACCOUNT.usBankAccount?.copy(
                bankName = null
            )
        )

        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentSelection.Saved(
                paymentMethod = bankAccountWithoutName
            )
        )

        assertThat(labels.label).isNotEmpty()
        assertThat(labels.sublabel).isNull()
    }

    @Test
    fun `create with Link payment selection returns correct labels`() {
        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentSelection.Link(
                selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                    details = TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
                    collectedCvc = null,
                    billingPhone = null,
                ),
            ),
        )

        assertThat(labels.label).isEqualTo("Link")
        assertThat(labels.sublabel).isEqualTo("Stripe Test Bank Account •••• 4242")
    }

    @Test
    fun `create with external payment method returns correct labels`() {
        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentMethodFixtures.createExternalPaymentMethod(
                PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC
            )
        )

        assertThat(labels.label).isEqualTo("external_paypal")
        assertThat(labels.sublabel).isNull()
    }

    @Test
    fun `create with custom payment method returns correct labels`() {
        val labels = PaymentOptionLabelsFactory.create(
            context = context,
            selection = PaymentMethodFixtures.createCustomPaymentMethod(
                PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD
            )
        )

        assertThat(labels.label).isEqualTo("PayPal")
        assertThat(labels.sublabel).isNull()
    }
}
