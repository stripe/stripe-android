package com.stripe.android.paymentsheet

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures.DEFAULT_CARD
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.payments.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.Injector
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import com.stripe.android.paymentsheet.PaymentOptionsViewModel.TransitionTarget
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.elements.BankRepository
import com.stripe.android.paymentsheet.forms.resources.StaticResourceRepository
import com.stripe.android.paymentsheet.injection.PaymentOptionsViewModelSubcomponent
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()
    private val testDispatcher = TestCoroutineDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val prefsRepository = FakePrefsRepository()
    private val customerRepository = FakeCustomerRepository()
    private val paymentMethodRepository = FakeCustomerRepository(PAYMENT_METHOD_REPOSITORY_PARAMS)
    private val resourceRepository =
        StaticResourceRepository(
            BankRepository(
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
        resourceRepository = resourceRepository
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

            assertThat(prefsRepository.getSavedSelection(true))
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
            prefsRepositoryFactory = { prefsRepository },
            eventReporter = eventReporter,
            customerRepository = customerRepository,
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext(),
            logger = Logger.noop(),
            injectorKey = DUMMY_INJECTOR_KEY,
            resourceRepository = resourceRepository
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
            resourceRepository = resourceRepository
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
            resourceRepository = resourceRepository
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
    fun `removePaymentMethod removes it from payment methods list`() = runBlockingTest {
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
            resourceRepository = resourceRepository
        )

        viewModel.removePaymentMethod(cards[1])
        idleLooper()

        assertThat(viewModel.paymentMethods.value)
            .containsExactly(cards[0], cards[2])
    }

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        val mockBuilder = mock<PaymentOptionsViewModelSubcomponent.Builder>()
        val mockSubcomponent = mock<PaymentOptionsViewModelSubcomponent>()
        val mockViewModel = mock<PaymentOptionsViewModel>()

        whenever(mockBuilder.build()).thenReturn(mockSubcomponent)
        whenever(mockBuilder.application(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.args(any())).thenReturn(mockBuilder)
        whenever(mockSubcomponent.viewModel).thenReturn(mockViewModel)

        val injector = object : Injector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as PaymentOptionsViewModel.Factory
                factory.subComponentBuilderProvider = Provider { mockBuilder }
            }
        }
        val injectorKey = WeakMapInjectorRegistry.nextKey("testKey")
        WeakMapInjectorRegistry.register(injector, injectorKey)
        val factory = PaymentOptionsViewModel.Factory(
            { ApplicationProvider.getApplicationContext() },
            {
                PaymentOptionContract.Args(
                    mock(),
                    mock(),
                    null,
                    false,
                    null,
                    null,
                    injectorKey,
                    false,
                    mock()
                )
            }
        )
        val factorySpy = spy(factory)
        val createdViewModel = factorySpy.create(PaymentOptionsViewModel::class.java)
        verify(factorySpy, times(0)).fallbackInitialize(any())
        assertThat(createdViewModel).isEqualTo(mockViewModel)

        WeakMapInjectorRegistry.staticCacheMap.clear()
    }

    @Test
    fun `Factory gets initialized with fallback when no Injector is available`() = runBlockingTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val productUsage = setOf("TestProductUsage")
        PaymentConfiguration.init(context, "testKey")
        val factory = PaymentOptionsViewModel.Factory(
            { context },
            {
                PaymentOptionContract.Args(
                    mock(),
                    mock(),
                    null,
                    false,
                    null,
                    null,
                    DUMMY_INJECTOR_KEY,
                    false,
                    productUsage
                )
            }
        )
        val factorySpy = spy(factory)
        assertNotNull(factorySpy.create(PaymentOptionsViewModel::class.java))
        verify(factorySpy).fallbackInitialize(
            argWhere {
                it.application == context && it.productUsage == productUsage
            }
        )
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
            newCard = null,
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR,
            injectorKey = DUMMY_INJECTOR_KEY,
            enableLogging = false,
            productUsage = mock()
        )
        private val PAYMENT_METHOD_REPOSITORY_PARAMS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }
}
