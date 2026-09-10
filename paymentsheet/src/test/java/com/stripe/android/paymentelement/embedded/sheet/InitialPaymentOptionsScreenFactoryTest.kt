package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.FakeTapToAddHelper
import com.stripe.android.isInstanceOf
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.EmbeddedFormInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedManageScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedUpdateScreenInteractorFactory
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.FakeSavedPaymentMethodRepository
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import javax.inject.Provider

internal class InitialPaymentOptionsScreenFactoryTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `vertical layout with one payment method and no saved methods opens form directly`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            isGooglePayReady = true,
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
    ) {
        val screens = factory.createInitialScreen()
        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.Form>()
    }

    @Test
    fun `creates initial screen successfully without wallets`() = testScenario(
        isGooglePayReady = false,
    ) {
        val screens = factory.createInitialScreen()
        assertThat(screens).hasSize(1)
    }

    @Test
    fun `screen is created with correct isLiveMode`() = testScenario {
        val screen = factory.createInitialScreen().first()
        val topBarState = screen.topBarState().value!!
        assertThat(topBarState.showTestModeLabel).isTrue()
    }

    @Test
    fun `screen isPerformingNetworkOperation returns false`() = testScenario {
        val screen = factory.createInitialScreen().first()
        assertThat(screen.isPerformingNetworkOperation().value).isFalse()
    }

    @Test
    fun `no payment selection creates a single payment options screen`() = testScenario {
        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
    }

    @Test
    fun `new selection requiring a form starts with the form on top of the back stack`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(2)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
        assertThat(screens[1]).isInstanceOf<EmbeddedNavigator.Screen.Form>()
    }

    @Test
    fun `new selection without a required form does not add a form screen`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
    ) {
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
    }

    @Test
    fun `horizontal layout creates a single horizontal payment options screen`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
    ) {
        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalPaymentOptions>()
    }

    @Test
    fun `horizontal layout with saved methods creates saved payment options screen`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = PaymentMethodFixtures.createCards(1),
        ),
    ) {
        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions>()
    }

    @Test
    fun `horizontal layout with wallets creates saved payment options screen`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            isGooglePayReady = true,
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
    ) {
        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions>()
    }

    @Test
    fun `horizontal layout restores new selection over saved payment options screen`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = PaymentMethodFixtures.createCards(1),
        ),
    ) {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(2)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions>()
        assertThat(screens[1]).isInstanceOf<EmbeddedNavigator.Screen.HorizontalPaymentOptions>()
    }

    @Test
    fun `automatic layout with two payment methods resolves to horizontal`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic,
        ),
    ) {
        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalPaymentOptions>()
    }

    @Test
    fun `automatic layout with three payment methods resolves to vertical`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp", "klarna"),
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic,
        ),
    ) {
        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
    }

    @Test
    fun `continue click delegates to continue coordinator`() = testScenario {
        factory.onContinueClick()

        assertThat(continueCoordinator.onContinueCalls.awaitItem()).isEqualTo(Unit)
    }

    @Suppress("LongMethod")
    private fun testScenario(
        isGooglePayReady: Boolean = true,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
            isGooglePayReady = isGooglePayReady,
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        customerState: CustomerState? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(paymentMethodMetadata.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
        )
        customerStateHolder.setCustomerState(customerState)
        val eventReporter = FakeEventReporter()
        val testScope = TestScope(UnconfinedTestDispatcher())
        val sheetActivityStateHolder = FakeSheetActivityStateHolder()
        val continueCoordinator = FakeSheetActivityContinueCoordinator()
        val autocompleteAddressInteractorFactory = TestAutocompleteAddressInteractor.noOpFactory()
        val formHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            embeddedSelectionHolder = selectionHolder,
            savedStateHandle = savedStateHandle,
            isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
        )
        val updateScreenInteractorFactory = FakeEmbeddedUpdateScreenInteractorFactory()
        val manageInteractorFactory = EmbeddedManageScreenInteractorFactory {
            FakeManageScreenInteractor()
        }
        val formScreenFactory = DefaultEmbeddedFormScreenFactory(
            formFactory = EmbeddedNavigator.Screen.Form.Factory(
                interactorFactory = EmbeddedFormInteractorFactory(
                    paymentMethodMetadata = paymentMethodMetadata,
                    embeddedSelectionHolder = selectionHolder,
                    embeddedFormHelperFactory = formHelperFactory,
                    viewModelScope = testScope,
                    sheetActivityStateHolder = sheetActivityStateHolder,
                    tapToAddHelper = FakeTapToAddHelper.noOp(),
                    eventReporter = FakeEventReporter(),
                    paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
                    autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
                ),
                sheetActivityStateHolder = sheetActivityStateHolder,
                confirmationHelper = FakeSheetActivityConfirmationHelper(),
                embeddedSelectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder,
            ),
        )

        val fakeInteractor =
            com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor.create()
        val initialScreen = EmbeddedNavigator.Screen.VerticalPaymentOptions(
            interactor = fakeInteractor,
            isLiveMode = true,
            sheetActivityState = sheetActivityStateHolder.state,
            onContinueClick = {},
            onPrimaryButtonDisabledClick = {},
        )
        val navigator = EmbeddedNavigator(
            coroutineScope = testScope,
            eventReporter = eventReporter,
            initialScreen = initialScreen,
        )
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)

        val addPaymentMethodInteractorFactory = EmbeddedAddPaymentMethodInteractorFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            embeddedSelectionHolder = selectionHolder,
            embeddedFormHelperFactory = formHelperFactory,
            viewModelScope = testScope,
            sheetActivityStateHolder = sheetActivityStateHolder,
            tapToAddHelper = FakeTapToAddHelper.noOp(),
            eventReporter = FakeEventReporter(),
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
            customerStateHolder = customerStateHolder,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        )
        val savedPaymentMethodMutator = SavedPaymentMethodMutator(
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
            eventReporter = eventReporter,
            coroutineScope = testScope,
            workContext = testScope.coroutineContext,
            uiContext = testScope.coroutineContext,
            savedPaymentMethodRepository = FakeSavedPaymentMethodRepository(),
            selection = selectionHolder.selection,
            setSelection = selectionHolder::setSelection,
            customerStateHolder = customerStateHolder,
            prePaymentMethodRemoveActions = {},
            postPaymentMethodRemoveActions = {},
            onUpdatePaymentMethod = { _, _, _, _, _ -> },
            isLinkEnabled = stateFlowOf(paymentMethodMetadata.shouldShowLinkButton),
            isNotPaymentFlow = true,
            linkAccount = stateFlowOf(null),
        )

        val factory = InitialPaymentOptionsScreenFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            eventReporter = eventReporter,
            embeddedNavigatorProvider = Provider { navigator },
            embeddedFormHelperFactory = formHelperFactory,
            viewModelScope = testScope,
            manageInteractorFactory = manageInteractorFactory,
            updateScreenInteractorFactory = updateScreenInteractorFactory,
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
            sheetActivityStateHolder = sheetActivityStateHolder,
            formScreenFactory = formScreenFactory,
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            addPaymentMethodInteractorFactory = addPaymentMethodInteractorFactory,
            continueCoordinator = continueCoordinator,
            savedPaymentMethodMutator = savedPaymentMethodMutator,
        )

        Scenario(
            factory = factory,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            navigator = navigator,
            sheetActivityStateHolder = sheetActivityStateHolder,
            continueCoordinator = continueCoordinator,
        ).block()
        eventReporter.validate()
        continueCoordinator.validate()
    }

    private class Scenario(
        val factory: InitialPaymentOptionsScreenFactory,
        val selectionHolder: EmbeddedSelectionHolder,
        val customerStateHolder: CustomerStateHolder,
        val navigator: EmbeddedNavigator,
        val sheetActivityStateHolder: FakeSheetActivityStateHolder,
        val continueCoordinator: FakeSheetActivityContinueCoordinator,
    )
}

private class FakeEmbeddedUpdateScreenInteractorFactory : EmbeddedUpdateScreenInteractorFactory {
    override fun createUpdateScreenInteractor(
        displayableSavedPaymentMethod: com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod,
    ): com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor {
        return com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor()
    }
}
