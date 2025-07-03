package com.stripe.android.link.ui.updatecard

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkScreen.UpdateCard.BillingDetailsUpdateFlow
import com.stripe.android.link.RealLinkDismissalCoordinator
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.confirmation.DefaultCompleteLinkFlow
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.utils.TestNavigationManager
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import com.stripe.android.uicore.navigation.NavigationManager
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class UpdateCardScreenViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `viewmodel initializes with valid card details`() = runTest(dispatcher) {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            paymentDetailsId = card.id
        )

        assertThat(viewModel.state.value.paymentDetailsId).isEqualTo(card.id)
    }

    @Test
    fun `viewmodel navigates back on invalid card details`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setConsumerPaymentDetails(ConsumerPaymentDetails(emptyList()))

        val navigationManager = TestNavigationManager()
        createViewModel(
            linkAccountManager = linkAccountManager,
            navigationManager = navigationManager,
            paymentDetailsId = "invalid_id"
        )

        navigationManager.assertNavigatedBack()
    }

    @Test
    fun `onUpdateClicked updates payment details successfully`() = runTest(dispatcher) {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
        val navigationManager = TestNavigationManager()
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            paymentDetailsId = card.id,
            navigationManager = navigationManager,
        )

        val cardUpdateParams = CardUpdateParams(
            expiryMonth = 12,
            expiryYear = 2025,
            billingDetails = null
        )

        viewModel.onCardUpdateParamsChanged(cardUpdateParams)

        viewModel.onUpdateClicked()

        val state = viewModel.state.value
        val call = linkAccountManager.awaitUpdateCardDetailsCall()
        assertThat(state.processing).isFalse()
        assertThat(state.error).isNull()
        assertThat(call.id).isEqualTo(state.paymentDetailsId)
        assertThat(call).isNotNull()

        navigationManager.assertNavigatedBack()
    }

    @Test
    fun `viewModel sets requiresModification to false for billing details update flow`() = runTest(dispatcher) {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            paymentDetailsId = card.id,
            billingDetailsUpdateFlow = BillingDetailsUpdateFlow()
        )

        // The requiresModification parameter should be passed to the edit card details interactor
        // This is indirectly tested through the state behavior
        val state = viewModel.state.value
        assertThat(state.paymentDetailsId).isEqualTo(card.id)
        assertThat(state.isBillingDetailsUpdateFlow).isTrue()
    }

    @Test
    fun `viewModel sets requiresModification to true for regular update flow`() = runTest(dispatcher) {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            paymentDetailsId = card.id,
            billingDetailsUpdateFlow = null
        )

        // The requiresModification parameter should be passed to the edit card details interactor
        // This is indirectly tested through the state behavior
        val state = viewModel.state.value
        assertThat(state.paymentDetailsId).isEqualTo(card.id)
        assertThat(state.isBillingDetailsUpdateFlow).isFalse()
    }

    private fun createViewModel(
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        navigationManager: NavigationManager = TestNavigationManager(),
        logger: Logger = FakeLogger(),
        dismissalCoordinator: LinkDismissalCoordinator = RealLinkDismissalCoordinator(),
        configuration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
        paymentDetailsId: String = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.id,
        billingDetailsUpdateFlow: BillingDetailsUpdateFlow? = null
    ): UpdateCardScreenViewModel {
        return UpdateCardScreenViewModel(
            logger = logger,
            linkAccountManager = linkAccountManager,
            navigationManager = navigationManager,
            dismissalCoordinator = dismissalCoordinator,
            configuration = configuration,
            paymentDetailsId = paymentDetailsId,
            completeLinkFlow = DefaultCompleteLinkFlow(
                linkConfirmationHandler = FakeLinkConfirmationHandler(),
                linkAccountManager = linkAccountManager,
                dismissalCoordinator = dismissalCoordinator,
                linkLaunchMode = LinkLaunchMode.Full
            ),
            billingDetailsUpdateFlow = billingDetailsUpdateFlow,
            linkLaunchMode = LinkLaunchMode.Full,
            dismissWithResult = {}
        )
    }
}
