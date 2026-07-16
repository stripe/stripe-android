package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedVerticalLayoutInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedManageScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedUpdateScreenInteractorFactory
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
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
        val screen = factory.createInitialScreen()
        assertThat(screen).isNotNull()
    }

    @Test
    fun `creates initial screen successfully without wallets`() = testScenario(
        isGooglePayReady = false,
    ) {
        val screen = factory.createInitialScreen()
        assertThat(screen).isNotNull()
    }

    @Test
    fun `screen is created with correct isLiveMode`() = testScenario {
        val screen = factory.createInitialScreen()
        val topBarState = screen.topBarState().value!!
        assertThat(topBarState.showTestModeLabel).isTrue()
    }

    @Test
    fun `screen isPerformingNetworkOperation returns false`() = testScenario {
        val screen = factory.createInitialScreen()
        assertThat(screen.isPerformingNetworkOperation().value).isFalse()
    }

    @Suppress("LongMethod")
    private fun testScenario(
        isGooglePayReady: Boolean = true,
        configuration: EmbeddedPaymentElement.Configuration = EmbeddedPaymentElement.Configuration
            .Builder("Example, Inc.")
            .build(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            isGooglePayReady = isGooglePayReady,
        )
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
        )
        val updateScreenInteractorFactory = FakeEmbeddedUpdateScreenInteractorFactory()
        val manageInteractorFactory = EmbeddedManageScreenInteractorFactory {
            FakeManageScreenInteractor()
        }
        val formScreenFactory = EmbeddedFormScreenFactory { code ->
            error("Form screen creation not expected in this test for code: $code")
        }

        val fakeInteractor =
            com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor.create()
        val initialScreen = EmbeddedNavigator.Screen.PaymentOptions(
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

        val embeddedVerticalLayoutInteractorFactory = EmbeddedVerticalLayoutInteractorFactory(
            coroutineScope = testScope,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            eventReporter = eventReporter,
            embeddedFormHelperFactory = formHelperFactory,
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
        )

        val factory = InitialPaymentOptionsScreenFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            embeddedNavigatorProvider = Provider { navigator },
            embeddedVerticalLayoutInteractorFactory = embeddedVerticalLayoutInteractorFactory,
            configuration = configuration,
            manageInteractorFactory = manageInteractorFactory,
            updateScreenInteractorFactory = updateScreenInteractorFactory,
            sheetActivityStateHolder = sheetActivityStateHolder,
            formScreenFactory = formScreenFactory,
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
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
