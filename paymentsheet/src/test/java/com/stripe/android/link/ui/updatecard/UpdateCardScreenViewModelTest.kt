package com.stripe.android.link.ui.updatecard

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.RealLinkDismissalCoordinator
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
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
    fun `onCancelClicked navigates back`() = runTest(dispatcher) {
        val navigationManager = TestNavigationManager()
        val viewModel = createViewModel(navigationManager = navigationManager)

        viewModel.onCancelClicked()
        navigationManager.assertNavigatedBack()
    }

    private fun createViewModel(
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        navigationManager: NavigationManager = TestNavigationManager(),
        logger: Logger = FakeLogger(),
        dismissalCoordinator: LinkDismissalCoordinator = RealLinkDismissalCoordinator(),
        paymentDetailsId: String = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.id
    ): UpdateCardScreenViewModel {
        return UpdateCardScreenViewModel(
            logger = logger,
            linkAccountManager = linkAccountManager,
            navigationManager = navigationManager,
            dismissalCoordinator = dismissalCoordinator,
            paymentDetailsId = paymentDetailsId,
        )
    }
}
