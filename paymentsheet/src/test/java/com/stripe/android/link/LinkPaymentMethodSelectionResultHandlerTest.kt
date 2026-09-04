package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkActivityResult.Canceled.Reason
import com.stripe.android.link.LinkPaymentMethodSelectionResultHandler.Outcome
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal class LinkPaymentMethodSelectionResultHandlerTest {
    private val selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
        details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
        collectedCvc = null,
        billingPhone = null,
    )
    private val linkSelection = PaymentSelection.Link(
        brand = LinkBrand.Link,
        selectedPayment = selectedPayment,
    )

    @Test
    fun `completion updates Link selection`() {
        val updatedPayment = selectedPayment.copy(collectedCvc = "123")

        val outcomes = handle(
            LinkActivityResult.Completed(LinkAccountUpdate.None, selectedPayment = updatedPayment)
        )

        assertThat(outcomes).containsExactly(
            Outcome.UpdateSelection(
                selection = linkSelection.copy(selectedPayment = updatedPayment),
                isCanceled = false,
                showPaymentOptions = false,
            )
        )
    }

    @Test
    fun `logout falls back to current customer payment method and shows payment options`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val outcomes = handle(
            result = LinkActivityResult.Canceled(Reason.LoggedOut, LinkAccountUpdate.None),
            customerState = createCustomerState(paymentMethods = listOf(paymentMethod)),
        )

        assertThat(outcomes).containsExactly(
            Outcome.UpdateSelection(
                selection = PaymentSelection.Saved(paymentMethod),
                isCanceled = true,
                showPaymentOptions = true,
            )
        )
    }

    @Test
    fun `logout uses default payment method when enabled`() {
        val first = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val default = first.copy(id = "pm_default")
        val metadata = PaymentMethodMetadataFactory.create(
            hasCustomerConfiguration = true,
            isPaymentMethodSetAsDefaultEnabled = true,
        )

        val outcomes = handle(
            result = LinkActivityResult.Canceled(Reason.LoggedOut, LinkAccountUpdate.None),
            customerState = createCustomerState(
                paymentMethods = listOf(first, default),
                defaultPaymentMethodId = default.id,
            ),
            metadata = metadata,
        )

        assertThat((outcomes.single() as Outcome.UpdateSelection).selection)
            .isEqualTo(PaymentSelection.Saved(default))
    }

    @Test
    fun `back shows payment options when Link has no usable payment`() {
        val outcomes = handle(
            result = LinkActivityResult.Canceled(Reason.BackPressed, LinkAccountUpdate.None),
            selection = linkSelection.copy(selectedPayment = null),
        )

        assertThat(outcomes).containsExactly(Outcome.ShowPaymentOptions)
    }

    @Test
    fun `back dismisses when Link has a usable payment`() {
        val outcomes = handle(
            LinkActivityResult.Canceled(Reason.BackPressed, LinkAccountUpdate.None)
        )

        assertThat(outcomes).containsExactly(Outcome.Dismiss)
    }

    @Test
    fun `pay another way shows payment options`() {
        val outcomes = handle(
            LinkActivityResult.Canceled(Reason.PayAnotherWay, LinkAccountUpdate.None)
        )

        assertThat(outcomes).containsExactly(Outcome.ShowPaymentOptions)
    }

    @Test
    fun `failure dismisses`() {
        val outcomes = handle(LinkActivityResult.Failed(Throwable("failure"), LinkAccountUpdate.None))

        assertThat(outcomes).containsExactly(Outcome.Dismiss)
    }

    @Test
    fun `account update produces updated Link metadata`() {
        val metadata = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            )
        )
        val accountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)

        val outcomes = handle(
            result = LinkActivityResult.Failed(Throwable("failure"), accountInfo),
            metadata = metadata,
        )

        val update = outcomes.first() as Outcome.UpdatedLinkMetadata
        assertThat(update.linkAccountInfo).isEqualTo(accountInfo)
        assertThat(update.paymentMethodMetadata.linkState?.loginState)
            .isEqualTo(LinkState.LoginState.LoggedIn)
        assertThat(outcomes.last()).isEqualTo(Outcome.Dismiss)
    }

    @Test
    fun `back during verification suppresses future eager presentation`() {
        val account = mock<LinkAccount> {
            on { accountStatus } doReturn AccountStatus.VerificationStarted
        }

        val outcomes = handle(
            result = LinkActivityResult.Canceled(
                Reason.BackPressed,
                LinkAccountUpdate.Value(account),
            )
        )

        assertThat(outcomes).contains(Outcome.SuppressFutureEagerPresentation)
    }

    private fun handle(
        result: LinkActivityResult,
        selection: PaymentSelection? = linkSelection,
        customerState: CustomerState? = null,
        metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    ): List<Outcome> {
        return LinkPaymentMethodSelectionResultHandler().handle(
            result = result,
            selection = selection,
            customerState = customerState,
            paymentMethodMetadata = metadata,
            currentLinkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
        )
    }
}
