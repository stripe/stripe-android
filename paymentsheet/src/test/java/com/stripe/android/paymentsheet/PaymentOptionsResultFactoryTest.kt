package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.model.AddressFixtures
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Test

internal class PaymentOptionsResultFactoryTest {

    @Test
    fun `createSucceeded includes current result data`() {
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)

        val result = PaymentOptionsResultFactory.createSucceeded(
            paymentSelection = selection,
            initialPaymentSelection = null,
            paymentMethods = paymentMethods,
            linkAccountInfo = linkAccountInfo,
            autocompleteFilledAddress = AddressFixtures.ADDRESS,
        )

        assertThat(result.paymentSelection).isEqualTo(selection)
        assertThat(result.paymentMethods).isEqualTo(paymentMethods)
        assertThat(result.linkAccountInfo).isEqualTo(linkAccountInfo)
        assertThat(result.autocompleteFilledAddress).isEqualTo(AddressFixtures.ADDRESS)
    }

    @Test
    fun `createSucceeded clears Link payment method without Link account`() {
        val selectedPayment = linkPaymentMethod(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD)

        val result = PaymentOptionsResultFactory.createSucceeded(
            paymentSelection = PaymentSelection.Link(
                brand = LinkBrand.Link,
                selectedPayment = selectedPayment,
            ),
            initialPaymentSelection = null,
            paymentMethods = emptyList(),
            linkAccountInfo = LinkAccountUpdate.Value(null),
            autocompleteFilledAddress = null,
        )

        val resultSelection = result.paymentSelection as PaymentSelection.Link
        assertThat(resultSelection.selectedPayment).isNull()
    }

    @Test
    fun `createSucceeded restores initial Link payment method`() {
        val initialSelectedPayment = linkPaymentMethod(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD)

        val result = PaymentOptionsResultFactory.createSucceeded(
            paymentSelection = PaymentSelection.Link(brand = LinkBrand.Link),
            initialPaymentSelection = PaymentSelection.Link(
                brand = LinkBrand.Link,
                selectedPayment = initialSelectedPayment,
            ),
            paymentMethods = emptyList(),
            linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            autocompleteFilledAddress = null,
        )

        val resultSelection = result.paymentSelection as PaymentSelection.Link
        assertThat(resultSelection.selectedPayment).isEqualTo(initialSelectedPayment)
    }

    @Test
    fun `createSucceeded prefers current Link payment method`() {
        val initialSelectedPayment = linkPaymentMethod(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD)
        val currentSelectedPayment = linkPaymentMethod(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
        )

        val result = PaymentOptionsResultFactory.createSucceeded(
            paymentSelection = PaymentSelection.Link(
                brand = LinkBrand.Link,
                selectedPayment = currentSelectedPayment,
            ),
            initialPaymentSelection = PaymentSelection.Link(
                brand = LinkBrand.Link,
                selectedPayment = initialSelectedPayment,
            ),
            paymentMethods = emptyList(),
            linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            autocompleteFilledAddress = null,
        )

        val resultSelection = result.paymentSelection as PaymentSelection.Link
        assertThat(resultSelection.selectedPayment).isEqualTo(currentSelectedPayment)
    }

    @Test
    fun `createCanceled rebinds saved selection to current payment method`() {
        val initialPaymentMethod = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
        val currentPaymentMethod = initialPaymentMethod.copy(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(displayBrand = "visa")
        )
        val linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)

        val result = PaymentOptionsResultFactory.createCanceled(
            initialPaymentSelection = PaymentSelection.Saved(initialPaymentMethod),
            paymentMethods = listOf(currentPaymentMethod),
            linkAccountInfo = linkAccountInfo,
        )

        assertThat(result.paymentSelection).isEqualTo(PaymentSelection.Saved(currentPaymentMethod))
        assertThat(result.paymentMethods).containsExactly(currentPaymentMethod)
        assertThat(result.linkAccountInfo).isEqualTo(linkAccountInfo)
        assertThat(result.mostRecentError).isNull()
    }

    @Test
    fun `createCanceled clears removed saved selection`() {
        val result = PaymentOptionsResultFactory.createCanceled(
            initialPaymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            paymentMethods = emptyList(),
            linkAccountInfo = LinkAccountUpdate.Value(null),
        )

        assertThat(result.paymentSelection).isNull()
    }

    @Test
    fun `createCanceled preserves non-saved selection`() {
        val result = PaymentOptionsResultFactory.createCanceled(
            initialPaymentSelection = PaymentSelection.GooglePay,
            paymentMethods = emptyList(),
            linkAccountInfo = LinkAccountUpdate.Value(null),
        )

        assertThat(result.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `createCanceled clears initial Link payment method without Link account`() {
        val result = PaymentOptionsResultFactory.createCanceled(
            initialPaymentSelection = PaymentSelection.Link(
                brand = LinkBrand.Link,
                selectedPayment = linkPaymentMethod(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD),
            ),
            paymentMethods = emptyList(),
            linkAccountInfo = LinkAccountUpdate.Value(null),
        )

        val resultSelection = result.paymentSelection as PaymentSelection.Link
        assertThat(resultSelection.selectedPayment).isNull()
    }

    @Test
    fun `createCanceled preserves null selection`() {
        val result = PaymentOptionsResultFactory.createCanceled(
            initialPaymentSelection = null,
            paymentMethods = emptyList(),
            linkAccountInfo = LinkAccountUpdate.Value(null),
        )

        assertThat(result.paymentSelection).isNull()
    }

    private fun linkPaymentMethod(
        details: ConsumerPaymentDetails.PaymentDetails,
    ): LinkPaymentMethod {
        return LinkPaymentMethod.ConsumerPaymentDetails(
            details = details,
            collectedCvc = null,
            billingPhone = null,
        )
    }
}
