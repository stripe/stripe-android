package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.verticalmode.FakeLayoutCoordinatesFixtures
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.Selection
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.ViewAction
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

internal class EmbeddedVerticalLayoutInteractorFactoryTest {

    private val closeInteractorRule = CleanupTestRule(PaymentMethodVerticalLayoutInteractor::close)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.emptyRuleChain()
        .around(CoroutineTestRule())
        .around(closeInteractorRule)

    @Test
    fun `confirm flow with user-interaction-required payment method does not overwrite vertical selection`() =
        runScenario(
            formSheetAction = { EmbeddedPaymentElement.FormSheetAction.Confirm },
        ) {
            // A card requires a form, so in the confirm flow selecting it should route to the form screen rather
            // than immediately updating the vertical mode selection.
            selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

            assertThat(interactor.state.value.selection).isNull()
        }

    @Test
    fun `continue flow with user-interaction-required payment method overwrites vertical selection`() =
        runScenario(
            formSheetAction = { EmbeddedPaymentElement.FormSheetAction.Continue },
        ) {
            selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

            assertThat((interactor.state.value.selection as Selection.New).code).isEqualTo("card")
        }

    @Test
    fun `embedded content configuration invokes the row selection callback when selecting a payment method`() {
        var rowSelectionCallbackInvoked = false
        runScenario(
            paymentMethodMetadata = cardAndCashAppMetadata(),
            invokeRowSelectionCallback = { rowSelectionCallbackInvoked = true },
        ) {
            // cashapp has no form fields, so selecting it flows through the form helper's selectionUpdater, which
            // is where the row selection callback is wired.
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("cashapp"))

            assertThat(rowSelectionCallbackInvoked).isTrue()
        }
    }

    @Test
    fun `payment options sheet configuration has no row selection callback when selecting a payment method`() =
        runScenario(
            paymentMethodMetadata = cardAndCashAppMetadata(),
            invokeRowSelectionCallback = null,
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("cashapp"))

            // The selection still flows through the form helper (proving the path ran); the null callback is simply
            // not invoked.
            assertThat(selectionHolder.selection.value).isInstanceOf(PaymentSelection.New::class.java)
        }

    @Test
    fun `embedded content configuration reports current wallets state in visibility snapshot`() {
        val reportedWalletsState = CompletableDeferred<WalletsState?>()
        val currentWalletsState = walletsStateWithoutInlineWallets()
        val walletsStateFlow = stateFlowOf<WalletsState?>(currentWalletsState)
        runScenario(
            paymentMethodMetadata = cardOnlyMetadata(),
            walletsState = walletsStateFlow,
            walletsStateForAnalytics = { walletsStateFlow.value },
            eventReporter = snapshotRecordingEventReporter(reportedWalletsState),
        ) {
            reportInitiallyVisible("card")

            assertThat(reportedWalletsState.await()).isSameInstanceAs(currentWalletsState)
        }
    }

    @Test
    fun `payment options sheet configuration reports null wallets state in visibility snapshot`() {
        val reportedWalletsState = CompletableDeferred<WalletsState?>()
        runScenario(
            paymentMethodMetadata = cardOnlyMetadata(),
            // The sheet still supplies a wallets state to the interactor, but reports null to analytics.
            walletsState = stateFlowOf(walletsStateWithoutInlineWallets()),
            walletsStateForAnalytics = { null },
            eventReporter = snapshotRecordingEventReporter(reportedWalletsState),
        ) {
            reportInitiallyVisible("card")

            assertThat(reportedWalletsState.await()).isNull()
        }
    }

    private fun snapshotRecordingEventReporter(
        reportedWalletsState: CompletableDeferred<WalletsState?>,
    ): EventReporter {
        val delegate = FakeEventReporter()
        return object : EventReporter by delegate {
            override fun onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
                visiblePaymentMethods: List<String>,
                hiddenPaymentMethods: List<String>,
                walletsState: WalletsState?,
                isVerticalLayout: Boolean,
            ) {
                reportedWalletsState.complete(walletsState)
            }
        }
    }

    private fun cardOnlyMetadata(): PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card"),
        ),
    )

    private fun cardAndCashAppMetadata(): PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "cashapp"),
        ),
    )

    private fun walletsStateWithoutInlineWallets(): WalletsState = WalletsState(
        link = null,
        googlePay = null,
        walletsAllowedInHeader = emptyList(),
        buttonsEnabled = true,
        dividerTextResource = 0,
        cardFundingFilter = DefaultCardFundingFilter,
        cardBrandFilter = DefaultCardBrandFilter,
        onGooglePayPressed = {},
        onLinkPressed = {},
    )

    @Suppress("LongMethod")
    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        processing: StateFlow<Boolean> = stateFlowOf(false),
        temporarySelection: StateFlow<PaymentMethodCode?> = stateFlowOf(null),
        walletsState: StateFlow<WalletsState?> = stateFlowOf(null),
        walletsStateForAnalytics: () -> WalletsState? = { null },
        formSheetAction: () -> EmbeddedPaymentElement.FormSheetAction? = {
            EmbeddedPaymentElement.FormSheetAction.Continue
        },
        invokeRowSelectionCallback: (() -> Unit)? = null,
        displaysMandatesInFormScreen: Boolean = false,
        eventReporter: EventReporter = FakeEventReporter(),
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(paymentMethodMetadata.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
        )
        val embeddedFormHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            embeddedSelectionHolder = selectionHolder,
            savedStateHandle = savedStateHandle,
        )
        val factory = EmbeddedVerticalLayoutInteractorFactory(
            coroutineScope = backgroundScope,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            eventReporter = eventReporter,
            embeddedFormHelperFactory = embeddedFormHelperFactory,
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
        )
        val interactor = closeInteractorRule.track(
            factory.create(
                paymentMethodMetadata = paymentMethodMetadata,
                processing = processing,
                temporarySelection = temporarySelection,
                walletsState = walletsState,
                walletsStateForAnalytics = walletsStateForAnalytics,
                formSheetAction = formSheetAction,
                invokeRowSelectionCallback = invokeRowSelectionCallback,
                displaysMandatesInFormScreen = displaysMandatesInFormScreen,
                transitionToManageScreen = {},
                transitionToFormScreen = {},
                onUpdatePaymentMethod = {},
            )
        )

        Scenario(
            interactor = interactor,
            selectionHolder = selectionHolder,
        ).block()
    }

    private class Scenario(
        val interactor: PaymentMethodVerticalLayoutInteractor,
        val selectionHolder: EmbeddedSelectionHolder,
    ) {
        // onGloballyPositioned reports the same coordinates twice for a stable item, so mirror that here to let the
        // visibility tracker reach a stable, dispatchable state.
        fun reportInitiallyVisible(code: String) {
            repeat(2) {
                interactor.handleViewAction(
                    ViewAction.UpdatePaymentMethodVisibility(
                        itemCode = code,
                        coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES,
                    )
                )
            }
        }
    }
}
