package com.stripe.android.paymentsheet

import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodCreateParamsFixtures.DEFAULT_CARD
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheetFixtures.updateState
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import com.stripe.android.R as PaymentsCoreR

@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val standardTestDispatcher = StandardTestDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val customerRepository = mock<CustomerRepository>()

    @Before
    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onUserSelection() when selection has been made should set the view state to process result`() =
        runTest {
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
                )
        }

    @Test
    fun `onUserSelection() when external payment method should set the view state to process result`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.paymentOptionResult.test {
                viewModel.updateSelection(EXTERNAL_PAYMENT_METHOD_PAYMENT_SELECTION)
                viewModel.onUserSelection()
                assertThat(awaitItem())
                    .isEqualTo(
                        PaymentOptionResult.Succeeded(
                            EXTERNAL_PAYMENT_METHOD_PAYMENT_SELECTION,
                            listOf()
                        )
                    )
                ensureAllEventsConsumed()
            }

            verify(eventReporter)
                .onSelectPaymentOption(
                    paymentSelection = EXTERNAL_PAYMENT_METHOD_PAYMENT_SELECTION
                )
        }

    @Test
    fun `onUserSelection() when custom payment method should set the view state to process result`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.paymentOptionResult.test {
                viewModel.updateSelection(CUSTOM_PAYMENT_METHOD_SELECTION)
                viewModel.onUserSelection()
                assertThat(awaitItem())
                    .isEqualTo(
                        PaymentOptionResult.Succeeded(
                            CUSTOM_PAYMENT_METHOD_SELECTION,
                            listOf()
                        )
                    )
                ensureAllEventsConsumed()
            }

            verify(eventReporter)
                .onSelectPaymentOption(
                    paymentSelection = CUSTOM_PAYMENT_METHOD_SELECTION
                )
        }

    @Test
    fun `onUserSelection() new card with save should complete with succeeded view state`() =
        runTest {
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
                    )
                ensureAllEventsConsumed()
            }
        }

    @Test
    fun `Opens saved payment methods if no new payment method was previously selected`() = runTest {
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(paymentSelection = null)
        )

        viewModel.navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()
        }
    }

    @Test
    fun `Restores backstack when user previously selected a new payment method`() = runTest {
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                paymentSelection = NEW_CARD_PAYMENT_SELECTION.copy(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
                ),
            )
        )

        viewModel.navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isInstanceOf<PaymentSheetScreen.AddAnotherPaymentMethod>()

            verify(eventReporter).onShowNewPaymentOptions()

            viewModel.handleBackPressed()
            assertThat(awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()
        }
    }

    @Test
    fun `when paymentMethods is empty, primary button and text below button are gone`() = runTest {
        val paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                paymentMethods = listOf(paymentMethod)
            )
        )

        viewModel.savedPaymentMethodMutator.removePaymentMethod(paymentMethod)

        assertThat(viewModel.customerStateHolder.paymentMethods.value).isEmpty()
        assertThat(viewModel.primaryButtonUiState.value).isNull()
        assertThat(viewModel.mandateHandler.mandateText.value?.text).isNull()
    }

    @Test
    fun `Does not select Link when user is logged out of their Link account`() = runTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = mock(),
                signupMode = null,
                loginState = LinkState.LoginState.LoggedOut,
            ),
        )

        assertThat(viewModel.selection.value).isNotEqualTo(PaymentSelection.Link())
        assertThat(viewModel.linkHandler.isLinkEnabled.value).isTrue()
    }

    @Test
    fun `Does not select Link when the Link state can't be determined`() = runTest {
        val viewModel = createViewModel(
            linkState = null,
        )

        assertThat(viewModel.selection.value).isNotEqualTo(PaymentSelection.Link())
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
    fun `transition target is AddFirstPaymentMethod if payment methods is empty`() = runTest {
        val viewModel = createViewModel(
            args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                paymentMethods = listOf(),
                isGooglePayReady = false,
                linkState = null
            )
        )

        viewModel.navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isInstanceOf<AddFirstPaymentMethod>()
        }
    }

    @Test
    fun `transition target is SelectSavedPaymentMethods if payment methods is not empty`() =
        runTest {
            val viewModel = createViewModel(
                args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                    isGooglePayReady = false,
                    linkState = null
                )
            )

            viewModel.navigationHandler.currentScreen.test {
                assertThat(awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()

                verify(eventReporter).onShowExistingPaymentOptions()
            }
        }

    @Test
    fun `currentScreen is Form if payment methods is empty and supportedPaymentMethods contains one`() =
        runTest {
            val viewModel = createViewModel(
                args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                    config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
                    ),
                    paymentMethods = listOf(),
                    isGooglePayReady = false,
                    linkState = null,
                    stripeIntent = PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!.copy(
                        paymentMethodTypes = listOf("card")
                    ),
                )
            )

            viewModel.navigationHandler.currentScreen.test {
                assertThat(awaitItem()).isInstanceOf<PaymentSheetScreen.VerticalModeForm>()
            }
        }

    @Test
    fun `currentScreen is VerticalMode if payment methods is not empty`() =
        runTest {
            val viewModel = createViewModel(
                args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                    config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
                    ),
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                    isGooglePayReady = false,
                    linkState = null,
                    stripeIntent = PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!.copy(
                        paymentMethodTypes = listOf("card")
                    ),
                )
            )

            viewModel.navigationHandler.currentScreen.test {
                assertThat(awaitItem()).isInstanceOf<PaymentSheetScreen.VerticalMode>()
            }
        }

    @Test
    fun `currentScreen is VerticalMode if supportedPaymentMethods is greater than 1`() =
        runTest {
            val viewModel = createViewModel(
                args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                    config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
                    ),
                    paymentMethods = listOf(),
                    isGooglePayReady = false,
                    linkState = null,
                    stripeIntent = PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!.copy(
                        paymentMethodTypes = listOf("card", "cashapp")
                    ),
                )
            )

            viewModel.navigationHandler.currentScreen.test {
                assertThat(awaitItem()).isInstanceOf<PaymentSheetScreen.VerticalMode>()
            }
        }

    @Test
    fun `currentScreen is Form if payment methods is empty and supportedPaymentMethods contains one in automatic`() =
        runTest {
            val viewModel = createViewModel(
                args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                    config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic,
                    ),
                    paymentMethods = listOf(),
                    isGooglePayReady = false,
                    linkState = null,
                    stripeIntent = PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!.copy(
                        paymentMethodTypes = listOf("card")
                    ),
                )
            )

            viewModel.navigationHandler.currentScreen.test {
                assertThat(awaitItem()).isInstanceOf<PaymentSheetScreen.VerticalModeForm>()
            }
        }

    @Test
    fun `currentScreen is VerticalMode if payment methods is not empty in automatic`() =
        runTest {
            val viewModel = createViewModel(
                args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                    config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic,
                    ),
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                    isGooglePayReady = false,
                    linkState = null,
                    stripeIntent = PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!.copy(
                        paymentMethodTypes = listOf("card")
                    ),
                )
            )

            viewModel.navigationHandler.currentScreen.test {
                assertThat(awaitItem()).isInstanceOf<PaymentSheetScreen.VerticalMode>()
            }
        }

    @Test
    fun `currentScreen is VerticalMode if supportedPaymentMethods is greater than 1 in Automatic`() =
        runTest {
            val viewModel = createViewModel(
                args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
                    config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic,
                    ),
                    paymentMethods = listOf(),
                    isGooglePayReady = false,
                    linkState = null,
                    stripeIntent = PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!.copy(
                        paymentMethodTypes = listOf("card", "cashapp")
                    ),
                )
            )

            viewModel.navigationHandler.currentScreen.test {
                assertThat(awaitItem()).isInstanceOf<PaymentSheetScreen.VerticalMode>()
            }
        }

    @Test
    fun `onError updates error`() = runTest {
        val viewModel = createViewModel()

        viewModel.error.test {
            assertThat(awaitItem())
                .isNull()
            viewModel.onError("some error".resolvableString)
            assertThat(awaitItem())
                .isEqualTo("some error".resolvableString)
        }
    }

    @Test
    fun `clearErrorMessages clears error`() = runTest {
        val viewModel = createViewModel()

        viewModel.error.test {
            assertThat(awaitItem())
                .isNull()
            viewModel.onError("some error".resolvableString)
            assertThat(awaitItem())
                .isEqualTo("some error".resolvableString)
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
            assertThat(viewModel.newPaymentSelection).isEqualTo(
                NewPaymentOptionSelection.New(
                    newSelection
                )
            )
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
    fun `Does not close the sheet if the selected payment method requires confirmation`() =
        runTest {
            val selection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT)

            val viewModel = createViewModel().apply { updateSelection(PaymentSelection.Link()) }

            viewModel.paymentOptionResult.test {
                viewModel.handlePaymentMethodSelected(selection)
                expectNoEvents()

                viewModel.handlePaymentMethodSelected(PaymentSelection.Link())

                val result = awaitItem() as? PaymentOptionResult.Succeeded
                assertThat(result?.paymentSelection).isEqualTo(PaymentSelection.Link())
            }
        }

    @Test
    fun `Falls back to initial saved payment selection if user cancels`() = runTest {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val selection = PaymentSelection.Saved(paymentMethod = paymentMethods.random())

        val args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
            paymentSelection = selection,
            paymentMethods = paymentMethods,
        )

        val viewModel = createViewModel(args)

        viewModel.paymentOptionResult.test {
            viewModel.transitionToAddPaymentScreen()

            // Simulate user filling out a different payment method, but not confirming it
            viewModel.updateSelection(
                PaymentSelection.New.Card(
                    paymentMethodCreateParams = DEFAULT_CARD,
                    brand = CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                )
            )

            viewModel.onUserCancel()

            assertThat(awaitItem()).isEqualTo(
                PaymentOptionResult.Canceled(
                    mostRecentError = null,
                    paymentSelection = selection,
                    paymentMethods = paymentMethods,
                )
            )
        }
    }

    @Test
    fun `Falls back to no payment selection if user cancels after deleting initial payment method`() = runTest {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val selection = PaymentSelection.Saved(paymentMethod = paymentMethods.random())

        val args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
            paymentSelection = selection,
            paymentMethods = paymentMethods,
        )

        val viewModel = createViewModel(args)

        viewModel.paymentOptionResult.test {
            // Simulate user removing the selected payment method
            viewModel.savedPaymentMethodMutator.removePaymentMethod(selection.paymentMethod)

            viewModel.onUserCancel()

            assertThat(awaitItem()).isEqualTo(
                PaymentOptionResult.Canceled(
                    mostRecentError = null,
                    paymentSelection = null,
                    paymentMethods = paymentMethods - selection.paymentMethod,
                )
            )
            verify(eventReporter, never()).onRemoveSavedPaymentMethod(any())
        }
    }

    @Test
    fun `Falls back to initial new payment selection if user cancels`() = runTest {
        val selection = PaymentSelection.New.Card(
            paymentMethodCreateParams = DEFAULT_CARD,
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        )

        val args = PAYMENT_OPTION_CONTRACT_ARGS.copy(
            state = PAYMENT_OPTION_CONTRACT_ARGS.state.copy(
                paymentSelection = selection,
            )
        )

        val viewModel = createViewModel(args)

        viewModel.paymentOptionResult.test {
            // Simulate user filling out a different payment method, but not confirming it
            viewModel.updateSelection(
                PaymentSelection.New.GenericPaymentMethod(
                    iconResource = 0,
                    label = "".resolvableString,
                    paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.US_BANK_ACCOUNT,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                )
            )

            viewModel.onUserCancel()

            assertThat(awaitItem()).isEqualTo(
                PaymentOptionResult.Canceled(
                    mostRecentError = null,
                    paymentSelection = selection,
                    paymentMethods = emptyList(),
                )
            )
        }
    }

    @Test
    fun `Sends dismiss event when the user cancels the flow with non-deferred intent`() = runTest {
        val viewModel = createViewModel()
        viewModel.onUserCancel()
        verify(eventReporter).onDismiss()
    }

    @Test
    fun `Sends dismiss event when the user cancels the flow with deferred intent`() = runTest {
        val deferredIntentArgs = PAYMENT_OPTION_CONTRACT_ARGS.copy(
            state = PAYMENT_OPTION_CONTRACT_ARGS.state.copy(
                paymentMethodMetadata = PAYMENT_OPTION_CONTRACT_ARGS.state.paymentMethodMetadata.copy(
                    stripeIntent = DEFERRED_PAYMENT_INTENT,
                )
            ),
        )

        val viewModel = createViewModel(args = deferredIntentArgs)
        viewModel.onUserCancel()
        verify(eventReporter).onDismiss()
    }

    @Test
    fun `Doesn't consider unsupported payment methods in header text creation`() = runTest {
        val args = PAYMENT_OPTION_CONTRACT_ARGS.copy(
            state = PAYMENT_OPTION_CONTRACT_ARGS.state.copy(
                config = PAYMENT_OPTION_CONTRACT_ARGS.state.config.copy(
                    allowsDelayedPaymentMethods = false,
                ),
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = PAYMENT_INTENT.copy(
                        paymentMethodTypes = listOf(
                            PaymentMethod.Type.Card.code,
                            PaymentMethod.Type.AuBecsDebit.code,
                        ),
                    ),
                    allowsDelayedPaymentMethods = false,
                )
            ),
        )

        val viewModel = createViewModel(args)

        viewModel.navigationHandler.currentScreen.test {
            awaitItem().title(isCompleteFlow = false, isWalletEnabled = false).test {
                assertThat(awaitItem()).isEqualTo(PaymentsCoreR.string.stripe_title_add_a_card.resolvableString)
            }
        }
    }

    @Test
    fun `Correctly updates state when removing payment method in edit screen succeeds`() = runTest(
        standardTestDispatcher
    ) {
        Dispatchers.setMain(standardTestDispatcher)

        val cards = PaymentMethodFactory.cards(3)
        val paymentMethodToRemove = cards.first()

        whenever(customerRepository.detachPaymentMethod(any(), eq(paymentMethodToRemove.id!!), eq(false))).thenReturn(
            Result.success(paymentMethodToRemove)
        )

        val args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
            paymentMethods = cards,
        )

        val viewModel = createViewModel(
            args = args,
            workContext = standardTestDispatcher,
        )

        turbineScope {
            val screenTurbine = viewModel.navigationHandler.currentScreen.testIn(this)
            val paymentMethodsTurbine = viewModel.customerStateHolder.paymentMethods.testIn(this)

            assertThat(screenTurbine.awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()

            assertThat(paymentMethodsTurbine.awaitItem()).containsExactlyElementsIn(cards).inOrder()

            viewModel.savedPaymentMethodMutator.updatePaymentMethod(
                paymentMethodToRemove.toDisplayableSavedPaymentMethod()
            )

            val editViewState = screenTurbine.awaitItem() as PaymentSheetScreen.UpdatePaymentMethod
            editViewState.interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod)

            assertThat(screenTurbine.awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()

            // The list of payment methods should not be updated until we're back on the SPM screen
            paymentMethodsTurbine.expectNoEvents()
            testScheduler.advanceUntilIdle()

            assertThat(paymentMethodsTurbine.awaitItem()).containsExactly(cards[1], cards[2]).inOrder()

            screenTurbine.ensureAllEventsConsumed()
            screenTurbine.cancelAndIgnoreRemainingEvents()

            paymentMethodsTurbine.ensureAllEventsConsumed()
            paymentMethodsTurbine.cancelAndIgnoreRemainingEvents()

            verify(eventReporter).onRemoveSavedPaymentMethod("card")
        }
    }

    @Test
    fun `Displays Link wallet button if customer has not saved PMs and Google Pay is not available`() = runTest {
        val args = PAYMENT_OPTION_CONTRACT_ARGS.updateState(
            linkState = LinkState(
                configuration = mock(),
                signupMode = null,
                loginState = LinkState.LoginState.NeedsVerification,
            ),
            isGooglePayReady = false,
        )

        val viewModel = createViewModel(args = args)

        viewModel.walletsState.test {
            val state = awaitItem()
            assertThat(state?.link).isEqualTo(WalletsState.Link(email = null))
            assertThat(state?.googlePay).isNull()
        }
    }

    @Test
    fun `On link selection with save requested, selection should be updated with saveable link selection`() =
        linkInlineSelectionTest(
            expectedCustomerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
        )

    @Test
    fun `On link selection with save not requested, selection should be updated with unsaveable link selection`() =
        linkInlineSelectionTest(
            expectedCustomerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        )

    private fun linkInlineSelectionTest(
        expectedCustomerRequestedSave: PaymentSelection.CustomerRequestedSave
    ) = runTest {
        Dispatchers.setMain(testDispatcher)

        val viewModel = createLinkViewModel()

        val linkInlineHandler = LinkInlineHandler.create()
        val formHelper = DefaultFormHelper.create(
            viewModel = viewModel,
            paymentMethodMetadata = requireNotNull(viewModel.paymentMethodMetadata.value),
            linkInlineHandler = linkInlineHandler,
        )

        viewModel.selection.test {
            assertThat(awaitItem()).isNull()

            formHelper.onFormFieldValuesChanged(
                formValues = FormFieldValues(
                    fieldValuePairs = mapOf(
                        IdentifierSpec.CardBrand to FormFieldEntry(CardBrand.Visa.code, true),
                    ),
                    userRequestedReuse = expectedCustomerRequestedSave,
                ),
                selectedPaymentMethodCode = "card",
            )

            assertThat(awaitItem()).isInstanceOf<PaymentSelection.New.Card>()

            val input = UserInput.SignUp(
                name = "John Doe",
                email = "johndoe@email.com",
                phone = "+15555555555",
                consentAction = SignUpConsentAction.CheckboxWithPrefilledEmailAndPhone,
                country = "US",
            )

            linkInlineHandler.onStateUpdated(
                InlineSignupViewState.create(
                    signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
                    config = TestFactory.LINK_CONFIGURATION,
                ).copy(
                    userInput = input,
                )
            )

            val selection = awaitItem()

            assertThat(selection).isInstanceOf<PaymentSelection.New.LinkInline>()

            val inlineSelection = selection as PaymentSelection.New.LinkInline

            assertThat(inlineSelection.input).isEqualTo(input)
            assertThat(inlineSelection.brand).isEqualTo(CardBrand.Visa)
            assertThat(inlineSelection.customerRequestedSave).isEqualTo(expectedCustomerRequestedSave)
        }
    }

    private fun createLinkViewModel(): PaymentOptionsViewModel {
        val linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(
            attachNewCardToAccountResult = Result.success(LinkTestUtils.LINK_NEW_PAYMENT_DETAILS),
            accountStatus = AccountStatus.Verified,
        )

        return createViewModel(
            linkState = LinkState(
                configuration = LinkTestUtils.createLinkConfiguration(),
                signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
                loginState = LinkState.LoginState.LoggedOut
            ),
            linkConfigurationCoordinator = linkConfigurationCoordinator,
        )
    }

    private fun createViewModel(
        args: PaymentOptionContract.Args = PAYMENT_OPTION_CONTRACT_ARGS,
        linkState: LinkState? = args.state.paymentMethodMetadata.linkState,
        linkConfigurationCoordinator: LinkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        workContext: CoroutineContext = testDispatcher,
    ) = TestViewModelFactory.create(linkConfigurationCoordinator) { linkHandler, savedStateHandle ->
        PaymentOptionsViewModel(
            args = args.copy(
                state = args.state.copy(
                    paymentMethodMetadata = args.state.paymentMethodMetadata.copy(
                        linkState = linkState
                    )
                )
            ),
            eventReporter = eventReporter,
            customerRepository = customerRepository,
            workContext = workContext,
            savedStateHandle = savedStateHandle,
            linkHandler = linkHandler,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
        )
    }

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFactory.create()
        private val DEFERRED_PAYMENT_INTENT = PAYMENT_INTENT.copy(clientSecret = null)
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
        private val EXTERNAL_PAYMENT_METHOD_PAYMENT_SELECTION =
            PaymentMethodFixtures.createExternalPaymentMethod(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
        private val CUSTOM_PAYMENT_METHOD_SELECTION =
            PaymentMethodFixtures.createCustomPaymentMethod(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD)
        private val NEW_CARD_PAYMENT_SELECTION = PaymentSelection.New.Card(
            DEFAULT_CARD,
            CardBrand.Discover,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )
        private val PAYMENT_OPTION_CONTRACT_ARGS = PaymentOptionContract.Args(
            state = PaymentSheetState.Full(
                customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
                config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.asCommonConfiguration(),
                paymentSelection = null,
                validationError = null,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = PAYMENT_INTENT,
                    isGooglePayReady = true,
                ),
            ),
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            enableLogging = false,
            productUsage = mock(),
            paymentElementCallbackIdentifier = "PaymentOptionsViewModelTestCallbackIdentifier",
        )
    }

    private class MyHostActivity : AppCompatActivity()
}
