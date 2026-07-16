package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedVerticalLayoutInteractorFactory
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedContentHelper.Companion.STATE_KEY_EMBEDDED_CONTENT
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.AnalyticEventCallbackRule
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.FakeSavedPaymentMethodRepository
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import com.stripe.android.utils.RecordingLinkPaymentLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

internal class DefaultEmbeddedContentHelperTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `dataLoaded updates savedStateHandle with paymentMethodMetadata`() = testScenario {
        assertThat(savedStateHandle.get<PaymentMethodMetadata?>(STATE_KEY_EMBEDDED_CONTENT))
            .isNull()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val appearance = Embedded(Embedded.RowStyle.FlatWithRadio.default)
        embeddedContentHelper.dataLoaded(paymentMethodMetadata, appearance, embeddedViewDisplaysMandateText = true)
        val state = savedStateHandle.get<DefaultEmbeddedContentHelper.State?>(STATE_KEY_EMBEDDED_CONTENT)
        assertThat(state?.paymentMethodMetadata).isEqualTo(paymentMethodMetadata)
        assertThat(state?.appearance).isEqualTo(appearance)
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `dataLoaded emits embeddedContent event`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            embeddedContentHelper.dataLoaded(
                PaymentMethodMetadataFactory.create(),
                Embedded(Embedded.RowStyle.FlatWithRadio.default),
                embeddedViewDisplaysMandateText = true,
            )
            assertThat(awaitItem()).isNotNull()
        }
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `dataLoaded emits walletButtonsContent event`() = testScenario {
        embeddedContentHelper.walletButtonsContent.test {
            assertThat(awaitItem()).isNull()
            embeddedContentHelper.dataLoaded(
                PaymentMethodMetadataFactory.create(),
                Embedded(Embedded.RowStyle.FlatWithRadio.default),
                embeddedViewDisplaysMandateText = true,
            )
            assertThat(awaitItem()).isNotNull()
        }
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `embeddedContent emits null when clearEmbeddedContent is called`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            embeddedContentHelper.dataLoaded(
                PaymentMethodMetadataFactory.create(),
                Embedded(Embedded.RowStyle.FlatWithRadio.default),
                embeddedViewDisplaysMandateText = true,
            )
            assertThat(awaitItem()).isNotNull()
            embeddedContentHelper.clearEmbeddedContent()
            assertThat(awaitItem()).isNull()
        }
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `walletButtonsContent emits null when clearEmbeddedContent is called`() = testScenario {
        embeddedContentHelper.walletButtonsContent.test {
            assertThat(awaitItem()).isNull()
            embeddedContentHelper.dataLoaded(
                PaymentMethodMetadataFactory.create(),
                Embedded(Embedded.RowStyle.FlatWithRadio.default),
                embeddedViewDisplaysMandateText = true,
            )
            assertThat(awaitItem()).isNotNull()
            embeddedContentHelper.clearEmbeddedContent()
            assertThat(awaitItem()).isNull()
        }
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `initializing embeddedContentHelper with paymentMethodMetadata emits correct initial event`() = testScenario(
        setup = {
            set(
                STATE_KEY_EMBEDDED_CONTENT,
                DefaultEmbeddedContentHelper.State(
                    PaymentMethodMetadataFactory.create(),
                    Embedded(Embedded.RowStyle.FloatingButton.default),
                    embeddedViewDisplaysMandateText = true,
                )
            )
        }
    ) {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `presentPaymentOptions reports error when state is null`() = testScenario {
        embeddedContentHelper.presentPaymentOptions()
        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            "unexpected_error.embedded.present_payment_options.not_configured"
        )
    }

    @Test
    fun `presentPaymentOptions reports error when launcher is null`() = testScenario(
        setup = {
            set(
                STATE_KEY_EMBEDDED_CONTENT,
                DefaultEmbeddedContentHelper.State(
                    PaymentMethodMetadataFactory.create(),
                    Embedded(Embedded.RowStyle.FlatWithRadio.default),
                    embeddedViewDisplaysMandateText = true,
                )
            )
        }
    ) {
        embeddedContentHelper.presentPaymentOptions()
        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            "unexpected_error.embedded.present_payment_options.no_launcher"
        )
    }

    @Test
    fun `presentPaymentOptions launches with the current state, customer, and selection`() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = createCustomerState()
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        testScenario(
            setup = {
                set(
                    STATE_KEY_EMBEDDED_CONTENT,
                    DefaultEmbeddedContentHelper.State(
                        paymentMethodMetadata,
                        Embedded(Embedded.RowStyle.FlatWithRadio.default),
                        embeddedViewDisplaysMandateText = true,
                    )
                )
                set(CustomerStateHolder.SAVED_CUSTOMER, customerState)
                set(DefaultEmbeddedSelectionHolder.EMBEDDED_SELECTION_KEY, selection)
            }
        ) {
            val fakeLauncher = RecordingEmbeddedSheetLauncher()
            embeddedContentHelper.setSheetLauncher(fakeLauncher)
            embeddedContentHelper.presentPaymentOptions()

            assertThat(fakeLauncher.launchPaymentOptionsCalls.single()).isEqualTo(
                RecordingEmbeddedSheetLauncher.LaunchPaymentOptionsCall(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = customerState,
                    selection = selection,
                    embeddedConfirmationState = null,
                )
            )
            assertThat(errorReporter.getLoggedErrors()).isEmpty()
        }
    }

    private class Scenario(
        val embeddedContentHelper: DefaultEmbeddedContentHelper,
        val savedStateHandle: SavedStateHandle,
        val eventReporter: FakeEventReporter,
        val errorReporter: FakeErrorReporter,
    )

    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    @Suppress("LongMethod")
    private fun testScenario(
        setup: SavedStateHandle.() -> Unit = {},
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val savedStateHandle = SavedStateHandle().apply { setup() }
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val embeddedFormHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            embeddedSelectionHolder = selectionHolder,
            savedStateHandle = savedStateHandle,
        )
        val confirmationHandler = FakeConfirmationHandler()
        val eventReporter = FakeEventReporter()
        val errorReporter = FakeErrorReporter()
        val immediateActionHandler = DefaultEmbeddedRowSelectionImmediateActionHandler(
            coroutineScope = backgroundScope,
            internalRowSelectionCallback = { null }
        )
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(
                PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA
            ),
            paymentMethodMetadataFlow = stateFlowOf(null),
        )
        val embeddedVerticalLayoutInteractorFactory = EmbeddedVerticalLayoutInteractorFactory(
            coroutineScope = backgroundScope,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            eventReporter = eventReporter,
            embeddedFormHelperFactory = embeddedFormHelperFactory,
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
        )

        val embeddedContentHelper = DefaultEmbeddedContentHelper(
            coroutineScope = backgroundScope,
            savedStateHandle = savedStateHandle,
            eventReporter = eventReporter,
            workContext = Dispatchers.Unconfined,
            uiContext = Dispatchers.Unconfined,
            savedPaymentMethodRepository = FakeSavedPaymentMethodRepository(),
            selectionHolder = selectionHolder,
            embeddedLinkHelper = object : EmbeddedLinkHelper {
                override val linkEmail: StateFlow<String?> = stateFlowOf(null)
            },
            embeddedWalletsHelper = { stateFlowOf(null) },
            customerStateHolder = customerStateHolder,
            embeddedVerticalLayoutInteractorFactory = embeddedVerticalLayoutInteractorFactory,
            confirmationHandler = confirmationHandler,
            confirmationStateHolder = EmbeddedConfirmationStateHolder(
                savedStateHandle = savedStateHandle,
                selectionHolder = selectionHolder,
                coroutineScope = backgroundScope,
            ),
            rowSelectionImmediateActionHandler = immediateActionHandler,
            errorReporter = errorReporter,
            internalRowSelectionCallback = { null },
            linkPaymentLauncher = RecordingLinkPaymentLauncher.noOp(),
            analyticsCallbackProvider = { AnalyticEventCallbackRule() },
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper()
        )
        Scenario(
            embeddedContentHelper = embeddedContentHelper,
            savedStateHandle = savedStateHandle,
            eventReporter = eventReporter,
            errorReporter = errorReporter,
        ).block()
        confirmationHandler.validate()
        eventReporter.validate()
    }

    private class RecordingEmbeddedSheetLauncher : EmbeddedSheetLauncher {
        val launchPaymentOptionsCalls = mutableListOf<LaunchPaymentOptionsCall>()

        override fun launchForm(
            code: String,
            paymentMethodMetadata: PaymentMethodMetadata,
            embeddedConfirmationState: EmbeddedConfirmationStateHolder.State?,
            customerState: CustomerState?,
            promotion: PaymentMethodMessagePromotion?,
        ) = error("Not expected.")

        override fun launchManage(
            paymentMethodMetadata: PaymentMethodMetadata,
            customerState: CustomerState,
            selection: PaymentSelection?,
            embeddedConfirmationState: EmbeddedConfirmationStateHolder.State?,
        ) = error("Not expected.")

        override fun launchPaymentOptions(
            paymentMethodMetadata: PaymentMethodMetadata,
            customerState: CustomerState?,
            selection: PaymentSelection?,
            embeddedConfirmationState: EmbeddedConfirmationStateHolder.State?,
        ) {
            launchPaymentOptionsCalls.add(
                LaunchPaymentOptionsCall(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = customerState,
                    selection = selection,
                    embeddedConfirmationState = embeddedConfirmationState,
                )
            )
        }

        data class LaunchPaymentOptionsCall(
            val paymentMethodMetadata: PaymentMethodMetadata,
            val customerState: CustomerState?,
            val selection: PaymentSelection?,
            val embeddedConfirmationState: EmbeddedConfirmationStateHolder.State?,
        )
    }
}
