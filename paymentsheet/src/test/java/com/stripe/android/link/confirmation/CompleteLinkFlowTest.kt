package com.stripe.android.link.confirmation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.LinkPaymentMethod.ConsumerPaymentDetails
import com.stripe.android.link.LinkPaymentMethod.LinkPaymentDetails
import com.stripe.android.link.RealLinkDismissalCoordinator
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.confirmation.CompleteLinkFlow.Result
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

internal class CompleteLinkFlowTest {

    private val linkAccount = TestFactory.LINK_ACCOUNT
    private val consumerPaymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
    private val linkPaymentDetails = TestFactory.LINK_NEW_PAYMENT_DETAILS
    private val cvc = "123"

    val consumerPaymentMethod = ConsumerPaymentDetails(
        consumerPaymentDetails,
        collectedCvc = cvc,
        billingPhone = null
    )

    val linkPaymentMethod = LinkPaymentDetails(
        linkPaymentDetails,
        collectedCvc = cvc,
        billingPhone = null
    )

    @Test
    fun `invoke with ConsumerPaymentDetails on Full mode - success`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler().apply {
            confirmResult = LinkConfirmationResult.Succeeded
        }
        val linkAccountManager = FakeLinkAccountManager()

        val completeLinkFlow = DefaultCompleteLinkFlow(
            linkConfirmationHandler = linkConfirmationHandler,
            linkAccountManager = linkAccountManager,
            dismissalCoordinator = RealLinkDismissalCoordinator(),
            linkLaunchMode = LinkLaunchMode.Full
        )

        val result = completeLinkFlow(
            selectedPaymentDetails = consumerPaymentMethod,
            linkAccount = linkAccount
        )

        // Verify confirmation was called correctly
        val calls = linkConfirmationHandler.calls
        assertThat(calls).hasSize(1)
        assertThat(calls[0].paymentDetails).isEqualTo(consumerPaymentDetails)
        assertThat(calls[0].linkAccount).isEqualTo(linkAccount)
        assertThat(calls[0].cvc).isEqualTo(cvc)

        // Verify result
        val completedResult = result as Result.Completed
        val activityResult = completedResult.linkActivityResult as LinkActivityResult.Completed
        assertThat(activityResult.selectedPayment).isNull()
        assertThat(activityResult.linkAccountUpdate).isInstanceOf(LinkAccountUpdate.Value::class.java)
    }

    @Test
    fun `invoke with ConsumerPaymentDetails on Confirmation mode - success`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler().apply {
            confirmResult = LinkConfirmationResult.Succeeded
        }

        val completeLinkFlow = DefaultCompleteLinkFlow(
            linkConfirmationHandler = linkConfirmationHandler,
            linkAccountManager = FakeLinkAccountManager(),
            dismissalCoordinator = RealLinkDismissalCoordinator(),
            linkLaunchMode = LinkLaunchMode.Confirmation(
                selectedPayment = ConsumerPaymentDetails(
                    details = consumerPaymentDetails,
                    collectedCvc = cvc,
                    billingPhone = null
                )
            )
        )

        val result = completeLinkFlow(
            selectedPaymentDetails = consumerPaymentMethod,
            linkAccount = linkAccount
        )

        assertThat(linkConfirmationHandler.calls).hasSize(1)
        assertThat(result).isInstanceOf(Result.Completed::class.java)
    }

    @Test
    fun `invoke with ConsumerPaymentDetails on PaymentMethodSelection mode - success`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler()
        val linkAccountManager = FakeLinkAccountManager().apply {
            setLinkAccount(LinkAccountUpdate.Value(linkAccount))
        }
        val completeLinkFlow = DefaultCompleteLinkFlow(
            linkConfirmationHandler = linkConfirmationHandler,
            linkAccountManager = linkAccountManager,
            dismissalCoordinator = RealLinkDismissalCoordinator(),
            linkLaunchMode = LinkLaunchMode.PaymentMethodSelection(selectedPayment = null)
        )

        val result = completeLinkFlow(
            selectedPaymentDetails = consumerPaymentMethod,
            linkAccount = linkAccount
        )

        // Verify no confirmation was called
        assertThat(linkConfirmationHandler.calls).isEmpty()

        // Verify result
        val completedResult = result as Result.Completed
        val activityResult = completedResult.linkActivityResult as LinkActivityResult.Completed
        val selectedPayment = activityResult.selectedPayment as ConsumerPaymentDetails
        assertThat(selectedPayment.details).isEqualTo(consumerPaymentDetails)
        assertThat(selectedPayment.collectedCvc).isEqualTo(cvc)
    }

    @Test
    fun `invoke with ConsumerPaymentDetails on Full mode - failure`() = runTest {
        val errorMessage = "Payment failed".resolvableString
        val linkConfirmationHandler = FakeLinkConfirmationHandler().apply {
            confirmResult = LinkConfirmationResult.Failed(errorMessage)
        }
        val linkAccountManager = FakeLinkAccountManager()

        val completeLinkFlow = DefaultCompleteLinkFlow(
            linkConfirmationHandler = linkConfirmationHandler,
            linkAccountManager = linkAccountManager,
            dismissalCoordinator = RealLinkDismissalCoordinator(),
            linkLaunchMode = LinkLaunchMode.Full
        )

        val result = completeLinkFlow(
            selectedPaymentDetails = consumerPaymentMethod,
            linkAccount = linkAccount
        )

        // Verify confirmation was called
        assertThat(linkConfirmationHandler.calls).hasSize(1)

        // Verify result
        val failedResult = result as Result.Failed
        assertThat(failedResult.error).isEqualTo(errorMessage)
    }

    @Test
    fun `invoke with ConsumerPaymentDetails on Full mode - canceled`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler().apply {
            confirmResult = LinkConfirmationResult.Canceled
        }
        val linkAccountManager = FakeLinkAccountManager()

        val completeLinkFlow = DefaultCompleteLinkFlow(
            linkConfirmationHandler = linkConfirmationHandler,
            linkAccountManager = linkAccountManager,
            dismissalCoordinator = RealLinkDismissalCoordinator(),
            linkLaunchMode = LinkLaunchMode.Full
        )

        val result = completeLinkFlow(
            selectedPaymentDetails = consumerPaymentMethod,
            linkAccount = linkAccount
        )

        // Verify confirmation was called
        assertThat(linkConfirmationHandler.calls).hasSize(1)

        assertThat(result).isEqualTo(Result.Canceled)
    }

    @Test
    fun `invoke with LinkPaymentDetails on Full mode - success`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler().apply {
            confirmWithLinkPaymentDetailsResult = LinkConfirmationResult.Succeeded
        }
        val linkAccountManager = FakeLinkAccountManager()

        val completeLinkFlow = DefaultCompleteLinkFlow(
            linkConfirmationHandler = linkConfirmationHandler,
            linkAccountManager = linkAccountManager,
            dismissalCoordinator = RealLinkDismissalCoordinator(),
            linkLaunchMode = LinkLaunchMode.Full
        )

        val result = completeLinkFlow(
            selectedPaymentDetails = linkPaymentMethod,
            linkAccount = linkAccount
        )

        // Verify confirmation was called
        val confirmCalls = linkConfirmationHandler.confirmWithLinkPaymentDetailsCall
        assertThat(confirmCalls).hasSize(1)
        assertThat(confirmCalls[0].paymentDetails).isEqualTo(linkPaymentDetails)
        assertThat(confirmCalls[0].linkAccount).isEqualTo(linkAccount)
        assertThat(confirmCalls[0].cvc).isEqualTo(cvc)

        // Verify result
        assertThat(result).isInstanceOf(Result.Completed::class.java)
    }

    @Test
    fun `invoke with LinkPaymentDetails on PaymentMethodSelection mode - success`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler()
        val linkAccountManager = FakeLinkAccountManager().apply {
            setLinkAccount(LinkAccountUpdate.Value(linkAccount))
        }

        val completeLinkFlow = DefaultCompleteLinkFlow(
            linkConfirmationHandler = linkConfirmationHandler,
            linkAccountManager = linkAccountManager,
            dismissalCoordinator = RealLinkDismissalCoordinator(),
            linkLaunchMode = LinkLaunchMode.PaymentMethodSelection(selectedPayment = null)
        )

        val result = completeLinkFlow(
            selectedPaymentDetails = linkPaymentMethod,
            linkAccount = linkAccount
        )

        // Verify no confirmation was called
        assertThat(linkConfirmationHandler.confirmWithLinkPaymentDetailsCall).isEmpty()

        // Verify result
        val completedResult = result as Result.Completed
        val activityResult = completedResult.linkActivityResult as LinkActivityResult.Completed
        val selectedPayment = activityResult.selectedPayment as LinkPaymentMethod.LinkPaymentDetails
        assertThat(selectedPayment.linkPaymentDetails).isEqualTo(linkPaymentDetails)
        assertThat(selectedPayment.collectedCvc).isEqualTo(cvc)
    }

    @Test
    fun `invoke with null cvc works correctly`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler().apply {
            confirmResult = LinkConfirmationResult.Succeeded
        }
        val linkAccountManager = FakeLinkAccountManager()

        val completeLinkFlow = DefaultCompleteLinkFlow(
            linkConfirmationHandler = linkConfirmationHandler,
            linkAccountManager = linkAccountManager,
            dismissalCoordinator = RealLinkDismissalCoordinator(),
            linkLaunchMode = LinkLaunchMode.Full
        )

        val result = completeLinkFlow(
            selectedPaymentDetails = linkPaymentMethod.copy(
                collectedCvc = null // Pass null cvc
            ),
            linkAccount = linkAccount
        )

        // Verify confirmation was called with null cvc
        assertThat(linkConfirmationHandler.confirmWithLinkPaymentDetailsCall).hasSize(1)
        assertThat(linkConfirmationHandler.confirmWithLinkPaymentDetailsCall[0].cvc).isNull()

        // Verify result is successful
        assertThat(result).isInstanceOf(Result.Completed::class.java)
    }
}
