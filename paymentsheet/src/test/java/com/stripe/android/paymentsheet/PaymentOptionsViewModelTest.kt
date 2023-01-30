package com.stripe.android.paymentsheet

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures.DEFAULT_CARD
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures.updateState
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.Loading
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import com.stripe.android.ui.core.forms.resources.StaticLpmResourceRepository
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.PaymentIntentFactory
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()
    private val testDispatcher = StandardTestDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val prefsRepository = FakePrefsRepository()
    private val customerRepository = FakeCustomerRepository()
    private val paymentMethodRepository = FakeCustomerRepository(PAYMENT_METHOD_REPOSITORY_PARAMS)

    @Test
    fun `onUserSelection() when selection has been made should set the view state to process result`() = runTest {
        val viewModel = createViewModel()
        viewModel.paymentOptionResult.test {
            viewModel.updateSelection(SELECTION_SAVED_PAYMENT_METHOD)
            viewModel.onUserSelection()
            assertThat(awaitItem()).isEqualTo(
                PaymentOptionResult.Succeeded(
                    SELECTION_SAVED_PAYMENT_METHOD,
                    listOf()
                )
            )
            ensureAllEventsConsumed()
        }

        verify(eventReporter)
            .onSelectPaymentOption(
                paymentSelection = SELECTION_SAVED_PAYMENT_METHOD,
                currency = "usd"
            )
    }

    @Test
    fun `onUserSelection() when new card selection with no save should set the view state to process result`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.paymentOptionResult.test {
                viewModel.updateSelection(NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION)
                viewModel.onUserSelection()
                assertThat(awaitItem())
                    .isEqualTo(
                        PaymentOptionResult.Succeeded(
                            NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION,
                            listOf()
                        )
                    )
                ensureAllEventsConsumed()
            }

            verify(eventReporter)
                .onSelectPaymentOption(
                    paymentSelection = NEW_REQUEST_DONT_SAVE_PAYMENT_SELECTION,
                    currency = "usd"
                )
            assertThat(prefsRepository.getSavedSelection(true, true))
                .isEqualTo(SavedSelection.None)
        }

    @Test
    fun `onUserSelection() new card with save should complete with succeeded view state`() =
        runTest {
            paymentMethodRepository.savedPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            val viewModel = createViewModel()
            viewModel.paymentOptionResult.test {
                viewModel.updateSelection(NEW_REQUEST_SAVE_PAYMENT_SELECTION)
                viewModel.onUserSelection()
                val paymentOptionResultSucceeded =
                    awaitItem() as PaymentOptionResult.Succeeded
                assertThat((paymentOptionResultSucceeded).paymentSelection)
                    .isEqualTo(NEW_REQUEST_SAVE_PAYMENT_SELECTION)
                verify(eventReporter)
                    .onSelectPaymentOption(
                        paymentSelection = paymentOptionResultSucceeded.paymentSelection,
                        currency = "usd"
                    )
                ensureAllEventsConsumed()
            }
        }

    @Test
    fun `Opens saved payment methods if no new payment method was previously selected`() = runTest {
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(newPaymentSelection = null)
        )

        viewModel.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(Loading)
            viewModel.transitionToFirstScreen()
            assertThat(awaitItem()).isEqualTo(SelectSavedPaymentMethods)
        }
    }

    @Test
    fun `Restores backstack when user previously selected a new payment method`() = runTest {
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                newPaymentSelection = NEW_CARD_PAYMENT_SELECTION.copy(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
                ),
            )
        )

        viewModel.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(Loading)
            viewModel.transitionToFirstScreen()
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.AddAnotherPaymentMethod)

            viewModel.handleBackPressed()
            assertThat(awaitItem()).isEqualTo(SelectSavedPaymentMethods)
        }
    }

    @Test
    fun `removePaymentMethod removes it from payment methods list`() = runTest {
        val cards = PaymentMethodFixtures.createCards(3)
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(paymentMethods = cards)
        )

        viewModel.removePaymentMethod(cards[1])

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
        assertThat(viewModel.linkHandler.activeLinkSession.value).isTrue()
        assertThat(viewModel.linkHandler.isLinkEnabled.value).isTrue()
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
        assertThat(viewModel.linkHandler.activeLinkSession.value).isFalse()
        assertThat(viewModel.linkHandler.isLinkEnabled.value).isTrue()
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
        assertThat(viewModel.linkHandler.activeLinkSession.value).isFalse()
        assertThat(viewModel.linkHandler.isLinkEnabled.value).isTrue()
    }

    @Test
    fun `Does not select Link when the Link state can't be determined`() = runTest {
        val viewModel = createViewModel(
            linkState = null,
        )

        assertThat(viewModel.selection.value).isNotEqualTo(PaymentSelection.Link)
        assertThat(viewModel.linkHandler.activeLinkSession.value).isFalse()
        assertThat(viewModel.linkHandler.isLinkEnabled.value).isFalse()
    }

    @Test
    fun `updatePrimaryButtonState updates the primary button state`() = runTest {
        val viewModel = createViewModel()

        viewModel.primaryButtonState.test {
            assertThat(awaitItem()).isNull()

            viewModel.updatePrimaryButtonState(PrimaryButton.State.Ready)

            assertThat(awaitItem()).isEqualTo(PrimaryButton.State.Ready)
        }
    }

    @Test
    fun `paymentMethods is not empty if customer has payment methods`() = runTest {
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
        )

        viewModel.paymentMethods.test {
            assertThat(awaitItem()).isNotEmpty()
        }
    }

    @Test
    fun `paymentMethods is empty if customer has no payment methods`() = runTest {
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                paymentMethods = listOf()
            )
        )

        viewModel.paymentMethods.test {
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun `transition target is AddFirstPaymentMethod if payment methods is empty`() = runTest {
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                paymentMethods = listOf(),
                isGooglePayReady = false,
                linkState = null
            )
        )

        viewModel.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(Loading)
            viewModel.transitionToFirstScreen()
            assertThat(awaitItem()).isEqualTo(AddFirstPaymentMethod)
        }
    }

    @Test
    fun `transition target is SelectSavedPaymentMethods if payment methods is not empty`() = runTest {
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                isGooglePayReady = false,
                linkState = null
            )
        )

        viewModel.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(Loading)
            viewModel.transitionToFirstScreen()
            assertThat(awaitItem()).isEqualTo(SelectSavedPaymentMethods)
        }
    }

    @Test
    fun `onError updates error`() = runTest {
        val viewModel = createViewModel()

        viewModel.error.test {
            assertThat(awaitItem())
                .isNull()
            viewModel.onError("some error")
            assertThat(awaitItem())
                .isEqualTo("some error")
        }
    }

    @Test
    fun `clearErrorMessages clears error`() = runTest {
        val viewModel = createViewModel()

        viewModel.error.test {
            assertThat(awaitItem())
                .isNull()
            viewModel.onError("some error")
            assertThat(awaitItem())
                .isEqualTo("some error")
            viewModel.clearErrorMessages()
            assertThat(awaitItem())
                .isNull()
        }
    }

    @Test
    fun `updateSelection with new payment method updates the current selection`() = runTest {
        val viewModel = createViewModel()

        viewModel.selection.test {
            val newSelection = PaymentSelection.New.Card(
                DEFAULT_CARD,
                CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
            )
            assertThat(awaitItem()).isNull()
            viewModel.updateSelection(newSelection)
            assertThat(awaitItem()).isEqualTo(newSelection)
            assertThat(viewModel.newPaymentSelection).isEqualTo(newSelection)
        }
    }

    @Test
    fun `updateSelection with saved payment method updates the current selection`() = runTest {
        val viewModel = createViewModel()

        viewModel.selection.test {
            val savedSelection = PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
            assertThat(awaitItem()).isNull()
            viewModel.updateSelection(savedSelection)
            assertThat(awaitItem()).isEqualTo(savedSelection)
            assertThat(viewModel.newPaymentSelection).isEqualTo(null)
        }
    }

    @Test
    fun `Ignores payment selection while in edit mode`() = runTest {
        val viewModel = createViewModel().apply {
            updateSelection(PaymentSelection.Link)
        }

        viewModel.toggleEditing()
        viewModel.handlePaymentMethodSelected(PaymentSelection.GooglePay)

        assertThat(viewModel.selection.value).isEqualTo(PaymentSelection.Link)

        viewModel.toggleEditing()
        viewModel.handlePaymentMethodSelected(PaymentSelection.GooglePay)
        assertThat(viewModel.selection.value).isEqualTo(PaymentSelection.GooglePay)
    }

    private fun createViewModel(
        args: PaymentOptionContract.Args = PAYMENT_OPTION_CONTRACT_ARGS,
        linkState: LinkState? = args.state.linkState,
        lpmResourceRepository: ResourceRepository<LpmRepository> = createLpmResourceRepository()
    ) = TestViewModelFactory.create { linkHandler, savedStateHandle ->
        PaymentOptionsViewModel(
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
            savedStateHandle = savedStateHandle,
            linkHandler = linkHandler
        )
    }

    private fun createLpmResourceRepository(
        paymentIntent: PaymentIntent = PAYMENT_INTENT
    ) = StaticLpmResourceRepository(
        LpmRepository(
            LpmRepository.LpmRepositoryArguments(
                ApplicationProvider.getApplicationContext<Application>().resources
            )
        ).apply {
            this.update(paymentIntent, null)
        }
    )

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFactory.create()
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
                stripeIntent = PAYMENT_INTENT,
                clientSecret = PaymentIntentClientSecret("secret"),
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
