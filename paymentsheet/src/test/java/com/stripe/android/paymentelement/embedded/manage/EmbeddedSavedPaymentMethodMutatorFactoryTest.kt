package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.FakeSelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeSavedPaymentMethodRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import javax.inject.Provider

internal class EmbeddedSavedPaymentMethodMutatorFactoryTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `payment options includes available wallets`() = runScenario(
        initialScreen = { horizontalSavedOptionsScreen() },
        paymentMethodMetadata = metadataWithWallets(),
    ) {
        eventReporter.showExistingPaymentOptionsCalls.awaitItem()
        testScope.advanceUntilIdle()

        assertThat(mutator.paymentOptionsItems.value.map { it.viewType }).containsAtLeast(
            PaymentOptionsItem.ViewType.GooglePay,
            PaymentOptionsItem.ViewType.Link,
        )
    }

    @Test
    fun `manage mode excludes available wallets`() = runScenario(
        initialScreen = { EmbeddedNavigator.Screen.ManageAll(FakeManageScreenInteractor()) },
        paymentMethodMetadata = metadataWithWallets(),
        launchMode = EmbeddedLaunchMode.Manage,
    ) {
        eventReporter.showManageSavedPaymentMethods.awaitItem()
        testScope.advanceUntilIdle()

        assertThat(mutator.paymentOptionsItems.value.map { it.viewType }).containsNoneOf(
            PaymentOptionsItem.ViewType.GooglePay,
            PaymentOptionsItem.ViewType.Link,
        )
    }

    @Test
    fun `form mode excludes available wallets`() = runScenario(
        initialScreen = { EmbeddedNavigator.Screen.ManageAll(FakeManageScreenInteractor()) },
        paymentMethodMetadata = metadataWithWallets(),
        launchMode = EmbeddedLaunchMode.Form("card"),
    ) {
        eventReporter.showManageSavedPaymentMethods.awaitItem()
        testScope.advanceUntilIdle()

        assertThat(mutator.paymentOptionsItems.value.map { it.viewType }).containsNoneOf(
            PaymentOptionsItem.ViewType.GooglePay,
            PaymentOptionsItem.ViewType.Link,
        )
    }

    @Test
    fun `payment options removal from update screen navigates back before updating customer state`() = runScenario(
        initialScreen = { horizontalSavedOptionsScreen() },
    ) {
        eventReporter.showExistingPaymentOptionsCalls.awaitItem()
        navigator.performAction(
            EmbeddedNavigator.Action.GoToScreen(
                EmbeddedNavigator.Screen.ManageUpdate(
                    FakeUpdatePaymentMethodInteractor()
                )
            )
        )
        testScope.advanceUntilIdle()
        assertThat(navigator.screen.value).isInstanceOf(EmbeddedNavigator.Screen.ManageUpdate::class.java)
        eventReporter.showEditablePaymentOptionCalls.awaitItem()

        mutator.removePaymentMethodInEditScreen(paymentMethod)
        testScope.advanceUntilIdle()

        assertThat(navigator.screen.value).isEqualTo(rootScreen)
        assertThat(customerStateHolder.paymentMethods.value).isEmpty()
        assertThat(repository.detachRequests.awaitItem().paymentMethodId).isEqualTo(paymentMethod.id)
        assertThat(eventReporter.hideEditablePaymentOptionCalls.awaitItem()).isEqualTo(Unit)
        assertThat(eventReporter.removePaymentMethodCalls.awaitItem().code).isEqualTo("card")
    }

    private fun runScenario(
        initialScreen: () -> EmbeddedNavigator.Screen,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            hasCustomerConfiguration = true,
        ),
        launchMode: EmbeddedLaunchMode = EmbeddedLaunchMode.PaymentOptions,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = SavedStateHandle(),
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(paymentMethodMetadata.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
        ).apply {
            setCustomerState(
                PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                    paymentMethods = listOf(paymentMethod),
                )
            )
        }
        val eventReporter = FakeEventReporter()
        val rootScreen = initialScreen()
        val lifecycleScope = TestScope(testScheduler)
        val navigator = EmbeddedNavigator(
            coroutineScope = lifecycleScope,
            initialScreen = rootScreen,
            eventReporter = eventReporter,
        )
        val repository = FakeSavedPaymentMethodRepository(paymentMethods = listOf(paymentMethod))
        val factory = EmbeddedSavedPaymentMethodMutatorFactory(
            eventReporter = eventReporter,
            savedPaymentMethodRepository = repository,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            embeddedNavigatorProvider = Provider { navigator },
            paymentMethodMetadata = paymentMethodMetadata,
            workContext = coroutineContext,
            uiContext = coroutineContext,
            viewModelScope = lifecycleScope,
            updateScreenInteractorFactoryProvider = Provider { error("Not expected") },
            launchMode = launchMode,
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
        )

        Scenario(
            mutator = factory.createSavedPaymentMethodMutator(),
            navigator = navigator,
            rootScreen = rootScreen,
            paymentMethod = paymentMethod,
            customerStateHolder = customerStateHolder,
            repository = repository,
            eventReporter = eventReporter,
            testScope = this,
        ).block()

        lifecycleScope.cancel()
        repository.validate()
        eventReporter.validate()
    }

    private fun horizontalSavedOptionsScreen(): EmbeddedNavigator.Screen {
        return EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions(
            interactor = FakeSelectSavedPaymentMethodsInteractor(),
            sheetActivityState = stateFlowOf(
                SheetActivityStateHolder.State(
                    primaryButtonLabel = "".resolvableString,
                    isEnabled = false,
                    processingState = PrimaryButtonProcessingState.Idle(null),
                    isProcessing = false,
                    shouldDisplayLockIcon = true,
                )
            ),
            onContinueClick = {},
            onPrimaryButtonDisabledClick = {},
        )
    }

    private fun metadataWithWallets(): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            hasCustomerConfiguration = true,
            isGooglePayReady = true,
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            ),
        )
    }

    private data class Scenario(
        val mutator: SavedPaymentMethodMutator,
        val navigator: EmbeddedNavigator,
        val rootScreen: EmbeddedNavigator.Screen,
        val paymentMethod: PaymentMethod,
        val customerStateHolder: DefaultCustomerStateHolder,
        val repository: FakeSavedPaymentMethodRepository,
        val eventReporter: FakeEventReporter,
        val testScope: TestScope,
    )
}
