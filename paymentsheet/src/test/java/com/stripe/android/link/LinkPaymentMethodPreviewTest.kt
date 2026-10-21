package com.stripe.android.link

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkPaymentMethodPreviewTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val iconLoader = PaymentSelection.IconLoader(context.resources, DefaultStripeImageLoader(context))

    @Test
    fun `saved debit card retains structured metadata`() = assertSavedCard(ConsumerPaymentDetails.Card.Funding.Debit)

    @Test
    fun `saved credit card retains structured metadata`() = assertSavedCard(ConsumerPaymentDetails.Card.Funding.Credit)

    @Test
    fun `saved prepaid card retains structured metadata`() =
        assertSavedCard(ConsumerPaymentDetails.Card.Funding.Prepaid)

    @Test
    fun `saved unknown funding card remains a card`() = assertSavedCard(ConsumerPaymentDetails.Card.Funding.Unknown)

    @Test
    fun `new card uses structured consumer details`() {
        val preview = selection(
            LinkPaymentDetails.New(
                paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                confirmParams = TestFactory.PAYMENT_METHOD_CREATE_PARAMS,
                originalParams = TestFactory.PAYMENT_METHOD_CREATE_PARAMS,
            )
        ).preview()

        assertThat(preview.instrumentDetails)
            .isEqualTo(PaymentMethodPreviewDetails.Card(CardBrand.Visa, "credit", "4242"))
    }

    @Test
    fun `saved wrapper prefers consumer card over outer PaymentMethod`() {
        val preview = selection(
            LinkPaymentDetails.Saved(
                paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                paymentMethod = paymentMethod("debit"),
            )
        ).preview()

        assertThat(preview.instrumentDetails)
            .isEqualTo(PaymentMethodPreviewDetails.Card(CardBrand.Visa, "credit", "4242"))
    }

    @Test
    fun `new passthrough debit card uses retained PaymentMethod`() = assertPassthroughCard("debit", "debit")

    @Test
    fun `new passthrough credit card uses retained PaymentMethod`() = assertPassthroughCard("credit", "credit")

    @Test
    fun `unrecognized passthrough funding becomes unknown`() = assertPassthroughCard("future", "unknown")

    @Test
    fun `lightweight passthrough retains last4 with unknown brand and funding`() {
        val preview = consumerSelection(TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH).preview()

        assertThat(preview.instrumentDetails)
            .isEqualTo(PaymentMethodPreviewDetails.Card(CardBrand.Unknown, "unknown", "4242"))
    }

    @Test
    fun `passthrough without actual card falls back to known card record`() {
        val preview = selection(
            LinkPaymentDetails.Passthrough(
                paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH,
                paymentMethod = PaymentMethod.Builder().setId("pm_link").setType(PaymentMethod.Type.Link).build(),
            )
        ).preview()

        assertThat(preview.instrumentDetails)
            .isEqualTo(PaymentMethodPreviewDetails.Card(CardBrand.Unknown, "unknown", "4242"))
    }

    @Test
    fun `bank selection retains bank details only`() {
        val preview = consumerSelection(TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT).preview()

        assertThat(preview.instrumentDetails).isEqualTo(
            PaymentMethodPreviewDetails.BankAccount(null, "Stripe Test Bank Account", "4242")
        )
        assertThat(preview.type).isEqualTo(LinkController.PaymentMethodType.BankAccount)
    }

    @Test
    fun `generic selection is not classified by an outer card PaymentMethod`() {
        val preview = selection(
            LinkPaymentDetails.Saved(TestFactory.CONSUMER_PAYMENT_DETAILS_GENERIC, paymentMethod("debit"))
        ).preview()

        assertThat(preview.instrumentDetails).isNull()
        assertThat(preview.type).isEqualTo(LinkController.PaymentMethodType.Generic)
    }

    @Test
    fun `card factory preserves its structured source`() {
        val details = PaymentMethodPreviewDetails.Card(CardBrand.Unknown, "future", "1234")

        assertThat(LinkController.PaymentMethodPreview.create(context, details).instrumentDetails)
            .isSameInstanceAs(details)
    }

    @Test
    fun `bank factory preserves its structured source`() {
        val details = PaymentMethodPreviewDetails.BankAccount(null, null, "6789")

        assertThat(LinkController.PaymentMethodPreview.create(context, details).instrumentDetails)
            .isSameInstanceAs(details)
    }

    @Test
    fun `legacy constructor and JVM signature remain available`() {
        val preview = LinkController.PaymentMethodPreview(
            imageLoader = { error("Image not requested") },
            label = "Link",
            sublabel = null,
            type = LinkController.PaymentMethodType.Card,
        )

        assertThat(preview.instrumentDetails).isNull()
        assertThat(preview.label).isEqualTo("Link")
        assertThat(preview.sublabel).isNull()
        assertThat(
            LinkController.PaymentMethodPreview::class.java.getConstructor(
                kotlin.jvm.functions.Function1::class.java,
                String::class.java,
                String::class.java,
                LinkController.PaymentMethodType::class.java,
            )
        ).isNotNull()
    }

    private fun assertSavedCard(funding: ConsumerPaymentDetails.Card.Funding) {
        val details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(funding = funding)
        val preview = consumerSelection(details).preview()
        val legacyPreview = details.toPreview(context, iconLoader, reduceLinkBranding = false, isDarkTheme = false)

        assertThat(preview.instrumentDetails)
            .isEqualTo(PaymentMethodPreviewDetails.Card(details.brand, funding.cardFunding.code, details.last4))
        assertThat(preview.label).isEqualTo(legacyPreview.label)
        assertThat(preview.sublabel).isEqualTo(legacyPreview.sublabel)
        assertThat(preview.type).isEqualTo(legacyPreview.type)
    }

    private fun assertPassthroughCard(funding: String, expectedFunding: String) {
        val preview = selection(
            LinkPaymentDetails.Passthrough(
                paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH,
                paymentMethod = paymentMethod(funding),
                createdFromNewPaymentMethod = true,
            )
        ).preview()

        assertThat(preview.instrumentDetails)
            .isEqualTo(PaymentMethodPreviewDetails.Card(CardBrand.MasterCard, expectedFunding, "4444"))
    }

    private fun paymentMethod(funding: String): PaymentMethod = PaymentMethod.Builder()
        .setId("pm_card")
        .setType(PaymentMethod.Type.Card)
        .setCard(PaymentMethod.Card(brand = CardBrand.MasterCard, funding = funding, last4 = "4444"))
        .build()

    private fun consumerSelection(details: ConsumerPaymentDetails.PaymentDetails) =
        LinkPaymentMethod.ConsumerPaymentDetails(details, collectedCvc = null, billingPhone = null)

    private fun selection(details: LinkPaymentDetails) =
        LinkPaymentMethod.LinkPaymentDetails(details, collectedCvc = null, billingPhone = null)

    private fun LinkPaymentMethod.preview() =
        toPreview(context, iconLoader, reduceLinkBranding = false, isDarkTheme = false)
}
