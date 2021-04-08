package com.stripe.android.paymentsheet

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures.DEFAULT_CARD
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsViewModel.TransitionTarget
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentOptionsViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()
    private val testDispatcher = TestCoroutineDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val prefsRepository = FakePrefsRepository()
    private val paymentMethodRepository =
        FakePaymentMethodsRepository(PAYMENT_METHOD_REPOSITORY_PARAMS)

    private val viewModel = PaymentOptionsViewModel(
        args = PAYMENT_OPTION_CONTRACT_ARGS,
        prefsRepository = prefsRepository,
        paymentMethodsRepository = paymentMethodRepository,
        eventReporter = eventReporter,
        workContext = testDispatcher,
        application = ApplicationProvider.getApplicationContext()
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `onUserSelection() when selection has been made should set the view state to process result`() {
        var viewState: ViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }
        viewModel.updateSelection(SELECTION_SAVED_PAYMENT_METHOD)

        viewModel.onUserSelection()

        assertThat((viewState as ViewState.PaymentOptions.ProcessResult).result)
            .isEqualTo(PaymentOptionResult.Succeeded.Existing(SELECTION_SAVED_PAYMENT_METHOD))
        verify(eventReporter).onSelectPaymentOption(SELECTION_SAVED_PAYMENT_METHOD)
    }

    @Test
    fun `onUserSelection() when new card selection with no save should set the view state to process result`() =
        testDispatcher.runBlockingTest {
            var viewState: ViewState? = null
            viewModel.viewState.observeForever {
                viewState = it
            }
            viewModel.updateSelection(NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION)

            viewModel.onUserSelection()

            assertThat((viewState as ViewState.PaymentOptions.ProcessResult).result)
                .isEqualTo(
                    PaymentOptionResult.Succeeded.Unsaved(
                        NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION
                    )
                )
            verify(eventReporter).onSelectPaymentOption(NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION)

            assertThat(prefsRepository.getSavedSelection())
                .isEqualTo(SavedSelection.None)
        }

    @Test
    fun `onUserSelection() new card with save should complete with succeeded view state`() =
        testDispatcher.runBlockingTest {
            paymentMethodRepository.savedPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

            val viewState: MutableList<ViewState?> = mutableListOf()
            viewModel.viewState.observeForever {
                viewState.add(it)
            }

            viewModel.updateSelection(NEW_REQUEST_SAVE_PAYMENT_SELECTION)

            viewModel.onUserSelection()

            assertThat(viewState[0])
                .isInstanceOf(ViewState.PaymentOptions.Ready::class.java)
            assertThat(viewState[1])
                .isInstanceOf(ViewState.PaymentOptions.StartProcessing::class.java)

            assertThat(viewState[2])
                .isInstanceOf(ViewState.PaymentOptions.FinishProcessing::class.java)

            (viewState[2] as ViewState.PaymentOptions.FinishProcessing).onComplete()

            val paymentOptionResultSucceeded =
                (viewState[3] as ViewState.PaymentOptions.ProcessResult)
                    .result as PaymentOptionResult.Succeeded.NewlySaved
            assertThat((paymentOptionResultSucceeded).paymentSelection)
                .isEqualTo(NEW_REQUEST_SAVE_PAYMENT_SELECTION)
            assertThat((paymentOptionResultSucceeded).newSavedPaymentMethod)
                .isEqualTo(paymentMethodRepository.savedPaymentMethod)
            verify(eventReporter).onSelectPaymentOption(paymentOptionResultSucceeded.paymentSelection)

            assertThat((prefsRepository.getSavedSelection() as SavedSelection.PaymentMethod).id)
                .isEqualTo(paymentMethodRepository.savedPaymentMethod.id!!)
        }

    @Test
    fun `onUserSelection() when selection has not been made should not emit`() {
        var viewState: ViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }
        viewModel.onUserSelection()

        assertThat(viewState)
            .isInstanceOf(ViewState.PaymentOptions.Ready::class.java)
    }

    @Test
    fun `onUserSelection() when save fails error is reported and in ready state`() {
        val exceptionMessage = "Card not valid."
        paymentMethodRepository.error = Exception(exceptionMessage)

        val viewStates: MutableList<ViewState> = mutableListOf()
        viewModel.viewState.observeForever {
            viewStates.add(it)
        }

        var userMessage: BaseSheetViewModel.UserMessage? = null
        viewModel.userMessage.observeForever {
            userMessage = it
        }

        viewModel.updateSelection(NEW_REQUEST_SAVE_PAYMENT_SELECTION)

        viewModel.onUserSelection()

        verify(eventReporter).onSelectPaymentOption(NEW_REQUEST_SAVE_PAYMENT_SELECTION)
        assertThat(viewStates.size).isEqualTo(3)
        assertThat(viewStates[0]).isInstanceOf(ViewState.PaymentOptions.Ready::class.java)
        assertThat(viewStates[1]).isInstanceOf(ViewState.PaymentOptions.StartProcessing::class.java)
        assertThat(viewStates[2]).isInstanceOf(ViewState.PaymentOptions.Ready::class.java)
        assertThat(userMessage).isEqualTo(BaseSheetViewModel.UserMessage.Error(exceptionMessage))
    }

    @Test
    fun `getPaymentOptionResult() after selection is set should return Succeeded`() {
        viewModel.updateSelection(SELECTION_SAVED_PAYMENT_METHOD)

        assertThat(
            viewModel.getPaymentOptionResult()
        ).isEqualTo(
            PaymentOptionResult.Succeeded.Existing(SELECTION_SAVED_PAYMENT_METHOD)
        )
    }

    @Test
    fun `getPaymentOptionResult() when selection is not set should return Canceled`() {
        assertThat(
            viewModel.getPaymentOptionResult()
        ).isEqualTo(
            PaymentOptionResult.Canceled(null)
        )
    }

    @Test
    fun `resolveTransitionTarget no new card`() {
        val viewModel = PaymentOptionsViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.copy(newCard = null),
            prefsRepository = FakePrefsRepository(),
            paymentMethodsRepository = FakePaymentMethodsRepository(emptyList()),
            eventReporter = eventReporter,
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext()
        )

        var transitionTarget: TransitionTarget? = null
        viewModel.transition.observeForever {
            transitionTarget = it
        }

        // no customer, no new card, no paymentMethods
        val fragmentConfig = FragmentConfigFixtures.DEFAULT
        viewModel.resolveTransitionTarget(fragmentConfig)

        assertThat(transitionTarget).isNull()
    }

    @Test
    fun `resolveTransitionTarget new card saved`() {
        val viewModel = PaymentOptionsViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.copy(
                newCard = NEW_CARD_PAYMENT_SELECTION.copy(
                    shouldSavePaymentMethod = true
                )
            ),
            prefsRepository = FakePrefsRepository(),
            paymentMethodsRepository = FakePaymentMethodsRepository(emptyList()),
            eventReporter = eventReporter,
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext()
        )

        val transitionTarget: MutableList<TransitionTarget?> = mutableListOf()
        viewModel.transition.observeForever {
            transitionTarget.add(it)
        }

        val fragmentConfig = FragmentConfigFixtures.DEFAULT
        viewModel.resolveTransitionTarget(fragmentConfig)

        assertThat(transitionTarget).containsExactly(null)
    }

    @Test
    fun `resolveTransitionTarget new card NOT saved`() {
        val viewModel = PaymentOptionsViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.copy(
                newCard = NEW_CARD_PAYMENT_SELECTION.copy(
                    shouldSavePaymentMethod = false
                )
            ),
            prefsRepository = FakePrefsRepository(),
            paymentMethodsRepository = FakePaymentMethodsRepository(emptyList()),
            eventReporter = eventReporter,
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext()
        )

        val transitionTarget: MutableList<TransitionTarget?> = mutableListOf()
        viewModel.transition.observeForever {
            transitionTarget.add(it)
        }

        val fragmentConfig = FragmentConfigFixtures.DEFAULT
        viewModel.resolveTransitionTarget(fragmentConfig)
        assertThat(transitionTarget).hasSize(2)
        assertThat(transitionTarget[1])
            .isInstanceOf(TransitionTarget.AddPaymentMethodFull::class.java)

        viewModel.resolveTransitionTarget(fragmentConfig)
        assertThat(transitionTarget).hasSize(2)
    }

    private companion object {
        private val SELECTION_SAVED_PAYMENT_METHOD = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        private val DEFAULT_PAYMENT_METHOD_CREATE_PARAMS: PaymentMethodCreateParams =
            DEFAULT_CARD

        private val NEW_REQUEST_SAVE_PAYMENT_SELECTION = PaymentSelection.New.Card(
            DEFAULT_PAYMENT_METHOD_CREATE_PARAMS,
            CardBrand.Visa,
            true,
        )
        private val NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION = PaymentSelection.New.Card(
            DEFAULT_PAYMENT_METHOD_CREATE_PARAMS,
            CardBrand.Visa,
            false,
        )
        private val NEW_CARD_PAYMENT_SELECTION = PaymentSelection.New.Card(
            DEFAULT_CARD,
            CardBrand.Discover,
            false
        )
        private val PAYMENT_OPTION_CONTRACT_ARGS = PaymentOptionContract.Args(
            paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentMethods = emptyList(),
            sessionId = SessionId(),
            config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            isGooglePayReady = true,
            newCard = null,
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR
        )
        private val PAYMENT_METHOD_REPOSITORY_PARAMS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }
}
