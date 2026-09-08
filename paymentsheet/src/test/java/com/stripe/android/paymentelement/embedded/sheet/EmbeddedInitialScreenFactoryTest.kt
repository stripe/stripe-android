package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.FakeTapToAddHelper
import com.stripe.android.isInstanceOf
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.form.EmbeddedFormInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedManageScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedUpdateScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.InitialManageScreenFactory
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class EmbeddedInitialScreenFactoryTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `create returns form screen for form launch mode`() = runScenario(
        launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
    ) {
        assertThat(factory.create().single()).isInstanceOf<EmbeddedNavigator.Screen.Form>()
    }

    @Test
    fun `create returns manage screen for manage launch mode`() = runScenario(
        launchMode = EmbeddedLaunchMode.Manage,
    ) {
        assertThat(factory.create().single()).isInstanceOf<EmbeddedNavigator.Screen.ManageAll>()
        manageInteractorFactory.createCalls.awaitItem()
    }

    @Test
    fun `create returns update screen for manage launch mode with one payment method`() = runScenario(
        launchMode = EmbeddedLaunchMode.Manage,
        customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = PaymentMethodFixtures.createCards(1),
        ),
    ) {
        assertThat(factory.create().single()).isInstanceOf<EmbeddedNavigator.Screen.ManageUpdate>()
        updateInteractorFactory.createCalls.awaitItem()
    }

    @Test
    fun `create returns payment options screen for payment options launch mode`() = runScenario(
        launchMode = EmbeddedLaunchMode.PaymentOptions,
    ) {
        assertThat(factory.create().single()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalPaymentOptions>()
        assertThat(sheetActivityStateHolder.updateErrorTurbine.awaitItem()).isNull()
    }

    @Test
    fun `create returns vertical payment options screen for vertical layout`() = runScenario(
        launchMode = EmbeddedLaunchMode.PaymentOptions,
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
    ) {
        val screens = factory.create()

        assertThat(screens).hasSize(1)
        assertThat(screens.single()).isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
    }

    @Test
    fun `create returns vertical payment options and form screens when selected payment method requires a form`() =
        runScenario(
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
            ),
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
        ) {
            val screens = factory.create()

            assertThat(screens).hasSize(2)
            assertThat(screens[0]).isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
            assertThat(screens[1]).isInstanceOf<EmbeddedNavigator.Screen.Form>()
        }

    @Suppress("LongMethod")
    private fun runScenario(
        launchMode: EmbeddedLaunchMode,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        customerState: CustomerState? = null,
        selection: PaymentSelection? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        selection?.let(selectionHolder::setSelection)
        val customerStateHolder = FakeCustomerStateHolder(customerState = customerState)
        val eventReporter = FakeEventReporter()
        val viewModelScope = TestScope(UnconfinedTestDispatcher())
        val sheetActivityStateHolder = FakeSheetActivityStateHolder()
        val promotionsHelper = FakePaymentMethodMessagePromotionsHelper()
        val autocompleteAddressInteractorFactory = TestAutocompleteAddressInteractor.noOpFactory()
        val formHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            embeddedSelectionHolder = selectionHolder,
            savedStateHandle = savedStateHandle,
            isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
        )
        val formFactory = EmbeddedNavigator.Screen.Form.Factory(
            interactorFactory = EmbeddedFormInteractorFactory(
                paymentMethodMetadata = paymentMethodMetadata,
                embeddedSelectionHolder = selectionHolder,
                embeddedFormHelperFactory = formHelperFactory,
                viewModelScope = viewModelScope,
                sheetActivityStateHolder = sheetActivityStateHolder,
                tapToAddHelper = FakeTapToAddHelper.noOp(),
                eventReporter = eventReporter,
                paymentMethodMessagePromotionsHelper = promotionsHelper,
                autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
            ),
            sheetActivityStateHolder = sheetActivityStateHolder,
            confirmationHelper = FakeSheetActivityConfirmationHelper(),
            embeddedSelectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
        )
        val manageInteractorFactory = FakeInitialScreenManageInteractorFactory()
        val updateInteractorFactory = FakeInitialScreenUpdateInteractorFactory()
        val initialManageScreenFactory = InitialManageScreenFactory(
            customerStateHolder = customerStateHolder,
            paymentMethodMetadata = paymentMethodMetadata,
            updateScreenInteractorFactory = updateInteractorFactory,
            manageInteractorFactory = manageInteractorFactory,
        )
        val addPaymentMethodInteractorFactory = EmbeddedAddPaymentMethodInteractorFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            embeddedSelectionHolder = selectionHolder,
            embeddedFormHelperFactory = formHelperFactory,
            viewModelScope = viewModelScope,
            sheetActivityStateHolder = sheetActivityStateHolder,
            tapToAddHelper = FakeTapToAddHelper.noOp(),
            eventReporter = eventReporter,
            paymentMethodMessagePromotionsHelper = promotionsHelper,
            customerStateHolder = customerStateHolder,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        )
        val continueCoordinator = FakeSheetActivityContinueCoordinator()
        val navigatorEventReporter = FakeEventReporter()
        val navigator = EmbeddedNavigator(
            coroutineScope = viewModelScope,
            initialScreen = EmbeddedNavigator.Screen.ManageAll(FakeManageScreenInteractor()),
            eventReporter = navigatorEventReporter,
        )
        navigatorEventReporter.showManageSavedPaymentMethods.awaitItem()
        val initialPaymentOptionsScreenFactory = InitialPaymentOptionsScreenFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            eventReporter = eventReporter,
            embeddedNavigatorProvider = { navigator },
            embeddedFormHelperFactory = formHelperFactory,
            viewModelScope = viewModelScope,
            manageInteractorFactory = manageInteractorFactory,
            updateScreenInteractorFactory = updateInteractorFactory,
            paymentMethodMessagePromotionsHelper = promotionsHelper,
            sheetActivityStateHolder = sheetActivityStateHolder,
            formScreenFactory = DefaultEmbeddedFormScreenFactory(formFactory),
            linkAccountHolder = LinkAccountHolder(savedStateHandle),
            addPaymentMethodInteractorFactory = addPaymentMethodInteractorFactory,
            continueCoordinator = continueCoordinator,
        )
        val factory = EmbeddedInitialScreenFactory(
            launchMode = launchMode,
            formScreenFactory = formFactory,
            initialManageScreenFactory = initialManageScreenFactory,
            initialPaymentOptionsScreenFactory = initialPaymentOptionsScreenFactory,
        )

        Scenario(
            factory = factory,
            manageInteractorFactory = manageInteractorFactory,
            updateInteractorFactory = updateInteractorFactory,
            sheetActivityStateHolder = sheetActivityStateHolder,
        ).block()

        manageInteractorFactory.validate()
        updateInteractorFactory.validate()
        continueCoordinator.validate()
        sheetActivityStateHolder.validate()
        customerStateHolder.validate()
        promotionsHelper.validate()
        eventReporter.validate()
        navigatorEventReporter.validate()
    }

    private class Scenario(
        val factory: EmbeddedInitialScreenFactory,
        val manageInteractorFactory: FakeInitialScreenManageInteractorFactory,
        val updateInteractorFactory: FakeInitialScreenUpdateInteractorFactory,
        val sheetActivityStateHolder: FakeSheetActivityStateHolder,
    )
}

internal class FakeInitialScreenManageInteractorFactory : EmbeddedManageScreenInteractorFactory {
    val createCalls = Turbine<Unit>()

    override fun createManageScreenInteractor(): ManageScreenInteractor {
        createCalls.add(Unit)
        return FakeManageScreenInteractor()
    }

    fun validate() {
        createCalls.ensureAllEventsConsumed()
    }
}

internal class FakeInitialScreenUpdateInteractorFactory : EmbeddedUpdateScreenInteractorFactory {
    val createCalls = Turbine<DisplayableSavedPaymentMethod>()

    override fun createUpdateScreenInteractor(
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    ): UpdatePaymentMethodInteractor {
        createCalls.add(displayableSavedPaymentMethod)
        return FakeUpdatePaymentMethodInteractor()
    }

    fun validate() {
        createCalls.ensureAllEventsConsumed()
    }
}
