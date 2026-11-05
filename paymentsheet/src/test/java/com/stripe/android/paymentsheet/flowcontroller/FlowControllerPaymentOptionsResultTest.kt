package com.stripe.android.paymentsheet.flowcontroller

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.model.Address
import com.stripe.android.model.AddressFixtures
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FlowControllerPaymentOptionsResultTest {

    @Test
    fun `selected result includes current result data`() {
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)

        val result = FlowControllerPaymentOptionsResult.from(
            result = complete(
                selection = selection,
                customerState = customerState(paymentMethods),
                linkAccountInfo = linkAccountInfo,
                autocompleteFilledAddress = AddressFixtures.ADDRESS,
            ),
            initialPaymentSelection = null,
        ) as FlowControllerPaymentOptionsResult.Selected

        assertThat(result.paymentSelection).isEqualTo(selection)
        assertThat(result.paymentMethods).isEqualTo(paymentMethods)
        assertThat(result.linkAccountInfo).isEqualTo(linkAccountInfo)
        assertThat(result.autocompleteFilledAddress).isEqualTo(AddressFixtures.ADDRESS)
    }

    @Test
    fun `selected result clears Link payment method without Link account`() {
        val result = FlowControllerPaymentOptionsResult.from(
            result = complete(
                selection = PaymentSelection.Link(
                    brand = LinkBrand.Link,
                    selectedPayment = linkPaymentMethod(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD),
                ),
                linkAccountInfo = LinkAccountUpdate.Value(null),
            ),
            initialPaymentSelection = null,
        ) as FlowControllerPaymentOptionsResult.Selected

        val resultSelection = result.paymentSelection as PaymentSelection.Link
        assertThat(resultSelection.selectedPayment).isNull()
    }

    @Test
    fun `selected result restores initial Link payment method`() {
        val initialSelectedPayment = linkPaymentMethod(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD)

        val result = FlowControllerPaymentOptionsResult.from(
            result = complete(
                selection = PaymentSelection.Link(brand = LinkBrand.Link),
                linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            ),
            initialPaymentSelection = PaymentSelection.Link(
                brand = LinkBrand.Link,
                selectedPayment = initialSelectedPayment,
            ),
        ) as FlowControllerPaymentOptionsResult.Selected

        val resultSelection = result.paymentSelection as PaymentSelection.Link
        assertThat(resultSelection.selectedPayment).isEqualTo(initialSelectedPayment)
    }

    @Test
    fun `selected result prefers current Link payment method`() {
        val currentSelectedPayment = linkPaymentMethod(TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT)

        val result = FlowControllerPaymentOptionsResult.from(
            result = complete(
                selection = PaymentSelection.Link(
                    brand = LinkBrand.Link,
                    selectedPayment = currentSelectedPayment,
                ),
                linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            ),
            initialPaymentSelection = PaymentSelection.Link(
                brand = LinkBrand.Link,
                selectedPayment = linkPaymentMethod(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD),
            ),
        ) as FlowControllerPaymentOptionsResult.Selected

        val resultSelection = result.paymentSelection as PaymentSelection.Link
        assertThat(resultSelection.selectedPayment).isEqualTo(currentSelectedPayment)
    }

    @Test
    fun `complete result without a selection restores the initial selection`() {
        val result = FlowControllerPaymentOptionsResult.from(
            result = complete(selection = null),
            initialPaymentSelection = PaymentSelection.GooglePay,
        )

        assertThat(result).isEqualTo(
            FlowControllerPaymentOptionsResult.Canceled(
                paymentSelection = PaymentSelection.GooglePay,
                linkAccountInfo = LinkAccountUpdate.Value(null),
                paymentMethods = null,
            )
        )
    }

    @Test
    fun `cancelled result rebinds saved selection to current payment method`() {
        val initialPaymentMethod = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
        val currentPaymentMethod = initialPaymentMethod.copy(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(displayBrand = "visa")
        )
        val linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)

        val result = FlowControllerPaymentOptionsResult.from(
            result = cancelled(
                customerState = customerState(listOf(currentPaymentMethod)),
                linkAccountInfo = linkAccountInfo,
            ),
            initialPaymentSelection = PaymentSelection.Saved(initialPaymentMethod),
        ) as FlowControllerPaymentOptionsResult.Canceled

        assertThat(result.paymentSelection).isEqualTo(PaymentSelection.Saved(currentPaymentMethod))
        assertThat(result.paymentMethods).containsExactly(currentPaymentMethod)
        assertThat(result.linkAccountInfo).isEqualTo(linkAccountInfo)
    }

    @Test
    fun `cancelled result clears removed saved selection`() {
        val result = FlowControllerPaymentOptionsResult.from(
            result = cancelled(customerState = customerState(emptyList())),
            initialPaymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        ) as FlowControllerPaymentOptionsResult.Canceled

        assertThat(result.paymentSelection).isNull()
    }

    @Test
    fun `cancelled result restores initial saved selection`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val result = FlowControllerPaymentOptionsResult.from(
            result = cancelled(customerState = customerState(listOf(paymentMethod))),
            initialPaymentSelection = PaymentSelection.Saved(paymentMethod),
        ) as FlowControllerPaymentOptionsResult.Canceled

        assertThat(result.paymentSelection).isEqualTo(PaymentSelection.Saved(paymentMethod))
    }

    @Test
    fun `cancelled result restores initial new selection`() {
        val result = FlowControllerPaymentOptionsResult.from(
            result = cancelled(),
            initialPaymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
        ) as FlowControllerPaymentOptionsResult.Canceled

        assertThat(result.paymentSelection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `cancelled result preserves non-saved selection`() {
        val result = FlowControllerPaymentOptionsResult.from(
            result = cancelled(),
            initialPaymentSelection = PaymentSelection.GooglePay,
        ) as FlowControllerPaymentOptionsResult.Canceled

        assertThat(result.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `cancelled result clears initial Link payment method without Link account`() {
        val result = FlowControllerPaymentOptionsResult.from(
            result = cancelled(linkAccountInfo = LinkAccountUpdate.Value(null)),
            initialPaymentSelection = PaymentSelection.Link(
                brand = LinkBrand.Link,
                selectedPayment = linkPaymentMethod(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD),
            ),
        ) as FlowControllerPaymentOptionsResult.Canceled

        val resultSelection = result.paymentSelection as PaymentSelection.Link
        assertThat(resultSelection.selectedPayment).isNull()
    }

    @Test
    fun `cancelled result preserves null selection`() {
        val result = FlowControllerPaymentOptionsResult.from(
            result = cancelled(),
            initialPaymentSelection = null,
        ) as FlowControllerPaymentOptionsResult.Canceled

        assertThat(result.paymentSelection).isNull()
    }

    @Test
    fun `error result is canceled without a selection`() {
        val result = FlowControllerPaymentOptionsResult.from(
            result = EmbeddedActivityResult.Error(launchMode = LAUNCH_MODE),
            initialPaymentSelection = PaymentSelection.GooglePay,
        )

        assertThat(result).isEqualTo(
            FlowControllerPaymentOptionsResult.Canceled(
                paymentSelection = null,
                linkAccountInfo = null,
                paymentMethods = null,
            )
        )
    }

    private fun complete(
        selection: PaymentSelection?,
        customerState: CustomerState? = null,
        linkAccountInfo: LinkAccountUpdate.Value = LinkAccountUpdate.Value(null),
        autocompleteFilledAddress: Address? = null,
    ) = EmbeddedActivityResult.Complete(
        selection = selection,
        previousNewSelections = Bundle(),
        hasBeenConfirmed = false,
        customerState = customerState,
        linkAccountInfo = linkAccountInfo,
        checkoutSessionResponse = null,
        autocompleteFilledAddress = autocompleteFilledAddress,
        shouldInvokeSelectionCallback = false,
        launchMode = LAUNCH_MODE,
    )

    private fun cancelled(
        customerState: CustomerState? = null,
        linkAccountInfo: LinkAccountUpdate.Value = LinkAccountUpdate.Value(null),
    ) = EmbeddedActivityResult.Cancelled(
        customerState = customerState,
        linkAccountInfo = linkAccountInfo,
        launchMode = LAUNCH_MODE,
    )

    private fun customerState(paymentMethods: List<PaymentMethod>): CustomerState {
        return PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(paymentMethods = paymentMethods)
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

    private companion object {
        val LAUNCH_MODE = EmbeddedLaunchMode.PaymentOptions(
            flowController = EmbeddedLaunchMode.FlowControllerOptions(
                visibleWallets = emptyList(),
                googlePlacesApiKey = null,
            ),
        )
    }
}
