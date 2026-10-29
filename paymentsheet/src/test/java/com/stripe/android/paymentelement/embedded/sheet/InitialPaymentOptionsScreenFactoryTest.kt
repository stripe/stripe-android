package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.FakeTapToAddHelper
import com.stripe.android.isInstanceOf
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.EmbeddedFormInteractorFactory
import com.stripe.android.paymentelement.embedded.form.OnClickDelegateOverrideImpl
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
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.FakeSavedPaymentMethodRepository
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
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
    fun `vertical layout with one payment method and a saved method opens payment options`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        ),
    ) {
        val screens = factory.createInitialScreen()
        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
    }

    @Test
    fun `creates initial screen successfully without wallets`() = testScenario(
        isGooglePayReady = false,
    ) {
        val screens = factory.createInitialScreen()
        assertThat(screens).hasSize(1)
    }

    @Test
    fun `screen isPerformingNetworkOperation returns false`() = testScenario {
        val screen = factory.createInitialScreen().first()
        assertThat(screen.isPerformingNetworkOperation().value).isFalse()
    }

    @Test
    fun `regular embedded keeps payment options beneath restored single method form`() = testScenario(
        launchMode = EmbeddedLaunchMode.Form("card"),
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
    ) {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(2)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
        assertThat(screens[1]).isInstanceOf<EmbeddedNavigator.Screen.Form>()
    }

    @Test
    fun `present reopens restored single payment method directly on its form`() = testScenario(
        launchMode = EmbeddedLaunchMode.PaymentOptions(visibleWallets = WalletType.entries),
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
    ) {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        val screen = factory.createInitialScreen().single()

        assertThat(screen).isInstanceOf<EmbeddedNavigator.Screen.Form>()
        assertThat((screen as EmbeddedNavigator.Screen.Form).formInteractor.state.value.selectedPaymentMethodCode)
            .isEqualTo("card")
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        continueCoordinator.onContinueCalls.expectNoEvents()
    }

    @Test
    fun `present with saved payment methods retains payment options beneath restored form`() = testScenario(
        launchMode = EmbeddedLaunchMode.PaymentOptions(visibleWallets = WalletType.entries),
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        ),
    ) {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(2)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
        assertThat(screens.last()).isInstanceOf<EmbeddedNavigator.Screen.Form>()
    }

    @Test
    fun `present with multiple payment methods retains payment options beneath restored form`() = testScenario(
        launchMode = EmbeddedLaunchMode.PaymentOptions(visibleWallets = WalletType.entries),
    ) {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(2)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
        assertThat(screens.last()).isInstanceOf<EmbeddedNavigator.Screen.Form>()
    }

    @Test
    fun `multiple vertical payment methods with a saved method create a list`() = testScenario(
        customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = PaymentMethodFixtures.createCards(1),
        ),
    ) {
        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
    }

    @Test
    fun `multiple vertical payment methods create a list`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
    ) {
        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
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
    fun `horizontal layout without saved methods or wallets creates payment method options screen`() = testScenario(
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
    fun `horizontal layout with multiple methods and wallets creates saved payment options screen`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
            isGooglePayReady = true,
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
    ) {
        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions>()
    }

    @Test
    fun `single horizontal method with Google Pay opens the form directly`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            isGooglePayReady = true,
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
    ) {
        assertThat(factory.createInitialScreen().single())
            .isInstanceOf<EmbeddedNavigator.Screen.HorizontalPaymentOptions>()
    }

    @Test
    fun `horizontal layout with only Link creates payment method options screen`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
    ) {
        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalPaymentOptions>()
    }

    @Test
    fun `horizontal layout restores new selection over saved payment options screen`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = PaymentMethodFixtures.createCards(1),
        ),
        // Match the activity, which collects navigator state after creating the initial screens.
        testDispatcher = StandardTestDispatcher(),
    ) {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        val screens = factory.createInitialScreen()

        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(screens).hasSize(2)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions>()
        assertThat(screens[1]).isInstanceOf<EmbeddedNavigator.Screen.HorizontalPaymentOptions>()

        navigator.performAction(EmbeddedNavigator.Action.ReplaceCurrentScreen(screens.last()))
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
        testScope.runCurrent()

        assertThat(navigator.screen.value).isSameInstanceAs(screens.last())
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `horizontal layout restores new selection with Google Pay and no saved methods`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
            isGooglePayReady = true,
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        // The activity queues navigator collection until screen construction finishes.
        testDispatcher = StandardTestDispatcher(),
    ) {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        val screens = factory.createInitialScreen()

        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(screens).hasSize(2)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions>()
        assertThat(screens[1]).isInstanceOf<EmbeddedNavigator.Screen.HorizontalPaymentOptions>()
    }

    @Test
    fun `horizontal layout restores custom selection over saved payment options screen`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = PaymentMethodFixtures.createCards(1),
        ),
        testDispatcher = StandardTestDispatcher(),
    ) {
        val selection = PaymentMethodFixtures.createCustomPaymentMethod(
            PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD,
        )
        selectionHolder.setSelection(selection)

        val screens = factory.createInitialScreen()

        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(screens).hasSize(2)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions>()
        assertThat(screens[1]).isInstanceOf<EmbeddedNavigator.Screen.HorizontalPaymentOptions>()

        navigator.performAction(EmbeddedNavigator.Action.ReplaceCurrentScreen(screens.last()))
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
        testScope.runCurrent()

        assertThat(navigator.screen.value).isSameInstanceAs(screens.last())
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
    }

    @Test
    fun `horizontal layout restores external selection over saved payment options screen`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = PaymentMethodFixtures.createCards(1),
        ),
        testDispatcher = StandardTestDispatcher(),
    ) {
        val selection = PaymentMethodFixtures.createExternalPaymentMethod(
            PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC,
        )
        selectionHolder.setSelection(selection)

        val screens = factory.createInitialScreen()

        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(screens).hasSize(2)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions>()
        assertThat(screens[1]).isInstanceOf<EmbeddedNavigator.Screen.HorizontalPaymentOptions>()

        navigator.performAction(EmbeddedNavigator.Action.ReplaceCurrentScreen(screens.last()))
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
        testScope.runCurrent()

        assertThat(navigator.screen.value).isSameInstanceAs(screens.last())
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
    }

    @Test
    fun `horizontal layout restores custom selection with Google Pay and no saved methods`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
            isGooglePayReady = true,
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        testDispatcher = StandardTestDispatcher(),
    ) {
        val selection = PaymentMethodFixtures.createCustomPaymentMethod(
            PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD,
        )
        selectionHolder.setSelection(selection)

        val screens = factory.createInitialScreen()

        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(screens).hasSize(2)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions>()
        assertThat(screens[1]).isInstanceOf<EmbeddedNavigator.Screen.HorizontalPaymentOptions>()
    }

    @Test
    fun `horizontal layout restores external selection with Google Pay and no saved methods`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
            isGooglePayReady = true,
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        testDispatcher = StandardTestDispatcher(),
    ) {
        val selection = PaymentMethodFixtures.createExternalPaymentMethod(
            PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC,
        )
        selectionHolder.setSelection(selection)

        val screens = factory.createInitialScreen()

        assertThat(selectionHolder.selection.value).isEqualTo(selection)
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

    @Test
    fun `continue invokes override once without completing for an existing selection`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        onClickOverrideDelegate.set { onClickOverrideCalls.add(Unit) }

        factory.onContinueClick()

        assertThat(onClickOverrideCalls.awaitItem()).isEqualTo(Unit)
        onClickOverrideCalls.expectNoEvents()
        continueCoordinator.onContinueCalls.expectNoEvents()
    }

    @Test
    fun `continue invokes override once without a payment selection`() = testScenario {
        assertThat(selectionHolder.selection.value).isNull()
        onClickOverrideDelegate.set { onClickOverrideCalls.add(Unit) }

        factory.onContinueClick()

        assertThat(onClickOverrideCalls.awaitItem()).isEqualTo(Unit)
        onClickOverrideCalls.expectNoEvents()
        continueCoordinator.onContinueCalls.expectNoEvents()
    }

    @Test
    fun `clearing override restores normal continuation`() = testScenario {
        onClickOverrideDelegate.set { onClickOverrideCalls.add(Unit) }
        factory.onContinueClick()
        assertThat(onClickOverrideCalls.awaitItem()).isEqualTo(Unit)
        continueCoordinator.onContinueCalls.expectNoEvents()

        onClickOverrideDelegate.clear()
        factory.onContinueClick()

        assertThat(continueCoordinator.onContinueCalls.awaitItem()).isEqualTo(Unit)
        onClickOverrideCalls.expectNoEvents()
    }

    @Suppress("LongMethod")
    private fun testScenario(
        launchMode: EmbeddedLaunchMode = EmbeddedLaunchMode.PaymentOptions(visibleWallets = WalletType.entries),
        isGooglePayReady: Boolean = true,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
            isGooglePayReady = isGooglePayReady,
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        customerState: CustomerState? = null,
        testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
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
        val testScope = TestScope(testDispatcher)
        val sheetActivityStateHolder = FakeSheetActivityStateHolder()
        val continueCoordinator = FakeSheetActivityContinueCoordinator()
        val onClickOverrideDelegate = OnClickDelegateOverrideImpl()
        val onClickOverrideCalls = Turbine<Unit>()
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
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
        val walletsInteractor = PaymentOptionsWalletsInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            launchMode = launchMode,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            linkAccountHolder = linkAccountHolder,
            continueCoordinator = continueCoordinator,
            sheetActivityStateHolder = sheetActivityStateHolder,
        )
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
                linkAccountHolder = linkAccountHolder,
            ),
        )

        val fakeInteractor =
            com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor.create()
        val initialScreen = EmbeddedNavigator.Screen.VerticalPaymentOptions(
            interactor = fakeInteractor,
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
            launchMode = launchMode,
            savedStateHandle = savedStateHandle,
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
            linkAccountHolder = linkAccountHolder,
            addPaymentMethodInteractorFactory = addPaymentMethodInteractorFactory,
            continueCoordinator = continueCoordinator,
            onClickOverrideDelegate = onClickOverrideDelegate,
            savedPaymentMethodMutator = savedPaymentMethodMutator,
            walletsInteractor = walletsInteractor,
        )

        Scenario(
            factory = factory,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            navigator = navigator,
            continueCoordinator = continueCoordinator,
            onClickOverrideDelegate = onClickOverrideDelegate,
            onClickOverrideCalls = onClickOverrideCalls,
            eventReporter = eventReporter,
            testScope = testScope,
        ).block()
        eventReporter.validate()
        continueCoordinator.validate()
        onClickOverrideCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val factory: InitialPaymentOptionsScreenFactory,
        val selectionHolder: EmbeddedSelectionHolder,
        val customerStateHolder: CustomerStateHolder,
        val navigator: EmbeddedNavigator,
        val continueCoordinator: FakeSheetActivityContinueCoordinator,
        val onClickOverrideDelegate: OnClickDelegateOverrideImpl,
        val onClickOverrideCalls: Turbine<Unit>,
        val eventReporter: FakeEventReporter,
        val testScope: TestScope,
    )
}

private class FakeEmbeddedUpdateScreenInteractorFactory : EmbeddedUpdateScreenInteractorFactory {
    override fun createUpdateScreenInteractor(
        displayableSavedPaymentMethod: com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod,
    ): com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor {
        return com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor()
    }
}
