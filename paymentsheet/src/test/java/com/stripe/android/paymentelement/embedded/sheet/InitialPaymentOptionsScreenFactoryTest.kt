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
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.FakeSavedPaymentMethodConfirmInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
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
    fun `creates initial screen successfully with Google Pay ready`() = testScenario(
        isGooglePayReady = true,
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
    fun `horizontal layout stays a single screen even when a new selection would need a form`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
    ) {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        val screens = factory.createInitialScreen()

        assertThat(screens).hasSize(1)
        assertThat(screens.first()).isInstanceOf<EmbeddedNavigator.Screen.HorizontalPaymentOptions>()
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

    @Suppress("LongMethod")
    private fun testScenario(
        isGooglePayReady: Boolean = true,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            isGooglePayReady = isGooglePayReady,
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
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
        val eventReporter = FakeEventReporter()
        val testScope = TestScope(UnconfinedTestDispatcher())
        val sheetActivityStateHolder = FakeSheetActivityStateHolder()
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
                ),
                eventReporter = FakeEventReporter(),
                sheetActivityStateHolder = sheetActivityStateHolder,
                confirmationHelper = FakeSheetActivityConfirmationHelper(),
                embeddedSelectionHolder = selectionHolder,
                savedPaymentMethodConfirmInteractorFactory = FakeSavedPaymentMethodConfirmInteractor.Factory(),
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
        )

        Scenario(
            factory = factory,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            navigator = navigator,
            sheetActivityStateHolder = sheetActivityStateHolder,
        ).block()
        eventReporter.validate()
    }

    private class Scenario(
        val factory: InitialPaymentOptionsScreenFactory,
        val selectionHolder: EmbeddedSelectionHolder,
        val customerStateHolder: CustomerStateHolder,
        val navigator: EmbeddedNavigator,
        val sheetActivityStateHolder: FakeSheetActivityStateHolder,
    )
}

private class FakeEmbeddedUpdateScreenInteractorFactory : EmbeddedUpdateScreenInteractorFactory {
    override fun createUpdateScreenInteractor(
        displayableSavedPaymentMethod: com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod,
    ): com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor {
        return com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor()
    }
}
