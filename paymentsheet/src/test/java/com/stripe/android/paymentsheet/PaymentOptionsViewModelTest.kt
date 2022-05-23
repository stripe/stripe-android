package com.stripe.android.paymentsheet

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
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
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import com.stripe.android.ui.core.elements.LpmRepository
import com.stripe.android.ui.core.forms.resources.StaticResourceRepository
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
    private val resourceRepository =
        StaticResourceRepository(
            LpmRepository(
                ApplicationProvider.getApplicationContext<Context>().resources
            ),
            AddressFieldElementRepository(
                ApplicationProvider.getApplicationContext<Context>().resources
            )
        )

    private val viewModel = PaymentOptionsViewModel(
        args = PAYMENT_OPTION_CONTRACT_ARGS,
        prefsRepositoryFactory = { prefsRepository },
        eventReporter = eventReporter,
        customerRepository = customerRepository,
        workContext = testDispatcher,
        application = ApplicationProvider.getApplicationContext(),
        logger = Logger.noop(),
        injectorKey = DUMMY_INJECTOR_KEY,
        resourceRepository = resourceRepository,
        savedStateHandle = SavedStateHandle(),
        linkPaymentLauncherFactory = mock()
    )

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

            assertThat(prefsRepository.getSavedSelection(true))
                .isEqualTo(SavedSelection.None)
        }

    @Test
    fun `onUserSelection() new card with save should complete with succeeded view state`() =
        runTest {
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
            args = PAYMENT_OPTION_CONTRACT_ARGS.copy(newLpm = null),
            prefsRepositoryFactory = { prefsRepository },
            eventReporter = eventReporter,
            customerRepository = customerRepository,
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext(),
            logger = Logger.noop(),
            injectorKey = DUMMY_INJECTOR_KEY,
            resourceRepository = resourceRepository,
            savedStateHandle = SavedStateHandle(),
            linkPaymentLauncherFactory = mock()
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
                newLpm = NEW_CARD_PAYMENT_SELECTION.copy(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
                )
            ),
            prefsRepositoryFactory = { prefsRepository },
            eventReporter = eventReporter,
            customerRepository = customerRepository,
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext(),
            logger = Logger.noop(),
            injectorKey = DUMMY_INJECTOR_KEY,
            resourceRepository = resourceRepository,
            savedStateHandle = SavedStateHandle(),
            linkPaymentLauncherFactory = mock()
        )

        val transitionTarget = mutableListOf<BaseSheetViewModel.Event<TransitionTarget?>>()
        viewModel.transition.observeForever {
            transitionTarget.add(it)
        }

        val fragmentConfig = FragmentConfigFixtures.DEFAULT
        viewModel.resolveTransitionTarget(fragmentConfig)

        assertThat(transitionTarget).hasSize(2)
        assertThat(transitionTarget[0].peekContent()).isNull()
        assertThat(transitionTarget[1].peekContent()).isInstanceOf(TransitionTarget.AddPaymentMethodFull::class.java)
    }

    @Test
    fun `resolveTransitionTarget new card NOT saved`() {
        val viewModel = PaymentOptionsViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.copy(
                newLpm = NEW_CARD_PAYMENT_SELECTION.copy(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                )
            ),
            prefsRepositoryFactory = { prefsRepository },
            eventReporter = eventReporter,
            customerRepository = customerRepository,
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext(),
            logger = Logger.noop(),
            injectorKey = DUMMY_INJECTOR_KEY,
            resourceRepository = resourceRepository,
            savedStateHandle = SavedStateHandle(),
            linkPaymentLauncherFactory = mock()
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

    @Test
    fun `removePaymentMethod removes it from payment methods list`() = runTest {
        val cards = PaymentMethodFixtures.createCards(3)
        val viewModel = PaymentOptionsViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.copy(paymentMethods = cards),
            prefsRepositoryFactory = { FakePrefsRepository() },
            eventReporter = eventReporter,
            customerRepository = customerRepository,
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext(),
            logger = Logger.noop(),
            injectorKey = DUMMY_INJECTOR_KEY,
            resourceRepository = resourceRepository,
            savedStateHandle = SavedStateHandle(),
            linkPaymentLauncherFactory = mock()
        )

        viewModel.removePaymentMethod(cards[1])
        idleLooper()

        assertThat(viewModel.paymentMethods.value)
            .containsExactly(cards[0], cards[2])
    }

    @Test
    fun `when paymentMethods is empty, primary button and text below button are gone`() = runTest {
        val paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT
        val viewModel = PaymentOptionsViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.copy(
                paymentMethods = listOf(paymentMethod)
            ),
            prefsRepositoryFactory = { FakePrefsRepository() },
            eventReporter = eventReporter,
            customerRepository = customerRepository,
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext(),
            logger = Logger.noop(),
            injectorKey = DUMMY_INJECTOR_KEY,
            resourceRepository = resourceRepository,
            savedStateHandle = SavedStateHandle(),
            linkPaymentLauncherFactory = mock()
        )

        viewModel.removePaymentMethod(paymentMethod)
        idleLooper()

        assertThat(viewModel.paymentMethods.value)
            .isEmpty()
        assertThat(viewModel.primaryButtonUIState.value).isNull()
        assertThat(viewModel.notesText.value).isNull()
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
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
        )
        private val NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION = PaymentSelection.New.Card(
            DEFAULT_PAYMENT_METHOD_CREATE_PARAMS,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        )
        private val NEW_CARD_PAYMENT_SELECTION = PaymentSelection.New.Card(
            DEFAULT_CARD,
            CardBrand.Discover,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )
        private val PAYMENT_OPTION_CONTRACT_ARGS = PaymentOptionContract.Args(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentMethods = emptyList(),
            config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            isGooglePayReady = true,
            newLpm = null,
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
