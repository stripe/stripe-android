package com.stripe.android.paymentsheet

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures.DEFAULT_CARD
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsViewModel.TransitionTarget
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsViewModelTest {
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
        var paymentOptionResult: PaymentOptionResult? = null
        viewModel.paymentOptionResult.observeForever {
            paymentOptionResult = it
        }
        viewModel.updateSelection(SELECTION_SAVED_PAYMENT_METHOD)

        viewModel.onUserSelection()

        assertThat(paymentOptionResult).isEqualTo(
            PaymentOptionResult.Succeeded(
                SELECTION_SAVED_PAYMENT_METHOD
            )
        )
        verify(eventReporter).onSelectPaymentOption(SELECTION_SAVED_PAYMENT_METHOD)
    }

    @Test
    fun `onUserSelection() when new card selection with no save should set the view state to process result`() =
        testDispatcher.runBlockingTest {
            var paymentOptionResult: PaymentOptionResult? = null
            viewModel.paymentOptionResult.observeForever {
                paymentOptionResult = it
            }
            viewModel.updateSelection(NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION)

            viewModel.onUserSelection()

            assertThat(paymentOptionResult)
                .isEqualTo(
                    PaymentOptionResult.Succeeded(
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

            var paymentOptionResult: PaymentOptionResult? = null
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
        val viewModel = PaymentOptionsViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.copy(newCard = null),
            prefsRepository = FakePrefsRepository(),
            eventReporter = eventReporter,
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext()
        )

        var transitionTarget: BaseSheetViewModel.Event<TransitionTarget?>? = null
        viewModel.transition.observeForever {
            transitionTarget = it
        }

        // no customer, no new card, no paymentMethods
        val fragmentConfig = FragmentConfigFixtures.DEFAULT
        viewModel.resolveTransitionTarget(fragmentConfig)

        assertThat(transitionTarget!!.peekContent()).isNull()
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
            eventReporter = eventReporter,
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext()
        )

        val transitionTarget = mutableListOf<BaseSheetViewModel.Event<TransitionTarget?>>()
        viewModel.transition.observeForever {
            transitionTarget.add(it)
        }

        val fragmentConfig = FragmentConfigFixtures.DEFAULT
        viewModel.resolveTransitionTarget(fragmentConfig)

        assertThat(transitionTarget).hasSize(1)
        assertThat(transitionTarget[0].peekContent()).isNull()
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
            eventReporter = eventReporter,
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext()
        )

        val transitionTarget = mutableListOf<BaseSheetViewModel.Event<TransitionTarget?>>()
        viewModel.transition.observeForever {
            transitionTarget.add(it)
        }

        val fragmentConfig = FragmentConfigFixtures.DEFAULT
        viewModel.resolveTransitionTarget(fragmentConfig)
        assertThat(transitionTarget).hasSize(2)
        assertThat(transitionTarget[1].peekContent())
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
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentMethods = emptyList(),
            config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            isGooglePayReady = true,
            newCard = null,
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR
        )
        private val PAYMENT_METHOD_REPOSITORY_PARAMS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }
}
