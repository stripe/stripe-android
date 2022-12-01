package com.stripe.android.paymentsheet

import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures.DEFAULT_CARD
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsViewModel.TransitionTarget
import com.stripe.android.paymentsheet.PaymentSheetFixtures.updateState
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.forms.resources.StaticLpmResourceRepository
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()
    private val testDispatcher = StandardTestDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val prefsRepository = FakePrefsRepository()
    private val customerRepository = FakeCustomerRepository()
    private val paymentMethodRepository = FakeCustomerRepository(PAYMENT_METHOD_REPOSITORY_PARAMS)
    private val lpmResourceRepository = StaticLpmResourceRepository(mock())
    private val linkLauncher = mock<LinkPaymentLauncher>()

    @Test
    fun `onUserSelection() when selection has been made should set the view state to process result`() {
        var paymentOptionResult: PaymentOptionResult? = null

        val viewModel = createViewModel()
        viewModel.paymentOptionResult.observeForever {
            paymentOptionResult = it
        }
        viewModel.updateSelection(SELECTION_SAVED_PAYMENT_METHOD)
        viewModel.onUserSelection()

        assertThat(paymentOptionResult).isEqualTo(
            PaymentOptionResult.Succeeded(
                SELECTION_SAVED_PAYMENT_METHOD,
                listOf()
            )
        )
        verify(eventReporter).onSelectPaymentOption(SELECTION_SAVED_PAYMENT_METHOD)
    }

    @Test
    fun `onUserSelection() when new card selection with no save should set the view state to process result`() =
        runTest {
            var paymentOptionResult: PaymentOptionResult? = null

            val viewModel = createViewModel()
            viewModel.paymentOptionResult.observeForever {
                paymentOptionResult = it
            }
            viewModel.updateSelection(NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION)
            viewModel.onUserSelection()

            assertThat(paymentOptionResult)
                .isEqualTo(
                    PaymentOptionResult.Succeeded(
                        NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION,
                        listOf()
                    )
                )
            verify(eventReporter).onSelectPaymentOption(NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION)

            assertThat(prefsRepository.getSavedSelection(true, true))
                .isEqualTo(SavedSelection.None)
        }

    @Test
    fun `onUserSelection() new card with save should complete with succeeded view state`() =
        runTest {
            paymentMethodRepository.savedPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

            var paymentOptionResult: PaymentOptionResult? = null

            val viewModel = createViewModel()
            viewModel.paymentOptionResult.observeForever {
                paymentOptionResult = it
            }
            viewModel.updateSelection(NEW_REQUEST_SAVE_PAYMENT_SELECTION)
            viewModel.onUserSelection()

            val paymentOptionResultSucceeded =
                paymentOptionResult as PaymentOptionResult.Succeeded
            assertThat((paymentOptionResultSucceeded).paymentSelection)
                .isEqualTo(NEW_REQUEST_SAVE_PAYMENT_SELECTION)
            verify(eventReporter).onSelectPaymentOption(paymentOptionResultSucceeded.paymentSelection)
        }

    @Test
    fun `resolveTransitionTarget no new card`() {
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(newPaymentSelection = null)
        )

        var transitionTarget: BaseSheetViewModel.Event<TransitionTarget?>? = null
        viewModel.transition.observeForever {
            transitionTarget = it
        }

        // no customer, no new card, no paymentMethods
        viewModel.resolveTransitionTarget()

        assertThat(transitionTarget!!.peekContent()).isNull()
    }

    @Test
    fun `resolveTransitionTarget new card saved`() {
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                newPaymentSelection = NEW_CARD_PAYMENT_SELECTION.copy(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
                ),
            )
        )

        val transitionTarget = mutableListOf<BaseSheetViewModel.Event<TransitionTarget?>>()
        viewModel.transition.observeForever {
            transitionTarget.add(it)
        }

        viewModel.resolveTransitionTarget()

        assertThat(transitionTarget).hasSize(2)
        assertThat(transitionTarget[0].peekContent()).isNull()
        assertThat(transitionTarget[1].peekContent()).isInstanceOf(TransitionTarget.AddPaymentMethodFull::class.java)
    }

    @Test
    fun `resolveTransitionTarget new card NOT saved`() {
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                newPaymentSelection = NEW_CARD_PAYMENT_SELECTION.copy(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                )
            )
        )

        val transitionTarget = mutableListOf<BaseSheetViewModel.Event<TransitionTarget?>>()
        viewModel.transition.observeForever {
            transitionTarget.add(it)
        }

        viewModel.resolveTransitionTarget()
        assertThat(transitionTarget).hasSize(2)
        assertThat(transitionTarget[1].peekContent())
            .isInstanceOf(TransitionTarget.AddPaymentMethodFull::class.java)

        viewModel.resolveTransitionTarget()
        assertThat(transitionTarget).hasSize(2)
    }

    @Test
    fun `removePaymentMethod removes it from payment methods list`() = runTest {
        val cards = PaymentMethodFixtures.createCards(3)
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(paymentMethods = cards)
        )

        viewModel.removePaymentMethod(cards[1])
        idleLooper()

        assertThat(viewModel.paymentMethods.value)
            .containsExactly(cards[0], cards[2])
    }

    @Test
    fun `Removing selected payment method clears selection`() = runTest {
        val cards = PaymentMethodFixtures.createCards(3)
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(paymentMethods = cards)
        )

        val selection = PaymentSelection.Saved(cards[1])
        viewModel.updateSelection(selection)
        assertThat(viewModel.selection.value).isEqualTo(selection)

        viewModel.removePaymentMethod(selection.paymentMethod)
        idleLooper()

        assertThat(viewModel.selection.value).isNull()
    }

    @Test
    fun `when paymentMethods is empty, primary button and text below button are gone`() = runTest {
        val paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                paymentMethods = listOf(paymentMethod)
            )
        )

        viewModel.removePaymentMethod(paymentMethod)
        idleLooper()

        assertThat(viewModel.paymentMethods.value)
            .isEmpty()
        assertThat(viewModel.primaryButtonUIState.value).isNull()
        assertThat(viewModel.notesText.value).isNull()
    }

    @Test
    fun `Selects Link when user is logged in to their Link account`() = runTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.LoggedIn,
            ),
        )

        assertThat(viewModel.selection.value).isEqualTo(PaymentSelection.Link)
        assertThat(viewModel.activeLinkSession.value).isTrue()
        assertThat(viewModel.isLinkEnabled.value).isTrue()
    }

    @Test
    fun `Selects Link when user needs to verify their Link account`() = runTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.NeedsVerification,
            ),
        )

        assertThat(viewModel.selection.value).isEqualTo(PaymentSelection.Link)
        assertThat(viewModel.activeLinkSession.value).isFalse()
        assertThat(viewModel.isLinkEnabled.value).isTrue()
    }

    @Test
    fun `Does not select Link when user is logged out of their Link account`() = runTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.LoggedOut,
            ),
        )

        assertThat(viewModel.selection.value).isNotEqualTo(PaymentSelection.Link)
        assertThat(viewModel.activeLinkSession.value).isFalse()
        assertThat(viewModel.isLinkEnabled.value).isTrue()
    }

    @Test
    fun `Does not select Link when the Link state can't be determined`() = runTest {
        val viewModel = createViewModel(
            linkState = null,
        )

        assertThat(viewModel.selection.value).isNotEqualTo(PaymentSelection.Link)
        assertThat(viewModel.activeLinkSession.value).isFalse()
        assertThat(viewModel.isLinkEnabled.value).isFalse()
    }

    private fun createViewModel(
        args: PaymentOptionContract.Args = PAYMENT_OPTION_CONTRACT_ARGS,
        linkState: LinkState? = args.state.linkState,
    ) = PaymentOptionsViewModel(
        args = args.copy(state = args.state.copy(linkState = linkState)),
        prefsRepositoryFactory = { prefsRepository },
        eventReporter = eventReporter,
        customerRepository = customerRepository,
        workContext = testDispatcher,
        application = ApplicationProvider.getApplicationContext(),
        logger = Logger.noop(),
        injectorKey = DUMMY_INJECTOR_KEY,
        lpmResourceRepository = lpmResourceRepository,
        addressResourceRepository = mock(),
        savedStateHandle = SavedStateHandle(),
        linkLauncher = linkLauncher
    )

    private companion object {
        private val SELECTION_SAVED_PAYMENT_METHOD = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        private val DEFAULT_PAYMENT_METHOD_CREATE_PARAMS: PaymentMethodCreateParams =
            DEFAULT_CARD

        private val NEW_REQUEST_SAVE_PAYMENT_SELECTION = PaymentSelection.New.Card(
            DEFAULT_PAYMENT_METHOD_CREATE_PARAMS,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        private val NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION = PaymentSelection.New.Card(
            DEFAULT_PAYMENT_METHOD_CREATE_PARAMS,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )
        private val NEW_CARD_PAYMENT_SELECTION = PaymentSelection.New.Card(
            DEFAULT_CARD,
            CardBrand.Discover,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )
        private val PAYMENT_OPTION_CONTRACT_ARGS = PaymentOptionContract.Args(
            state = PaymentSheetState.Full(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                clientSecret = PaymentIntentClientSecret("very secret stuff"),
                customerPaymentMethods = emptyList(),
                config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                isGooglePayReady = true,
                newPaymentSelection = null,
                linkState = null,
                savedSelection = SavedSelection.None,
            ),
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR,
            injectorKey = DUMMY_INJECTOR_KEY,
            enableLogging = false,
            productUsage = mock()
        )
        private val PAYMENT_METHOD_REPOSITORY_PARAMS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }

    private class MyHostActivity : AppCompatActivity()
}
