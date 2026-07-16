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
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
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
    fun `embeddedContent emits content when state is present`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            confirmationStateHolder.state = confirmationState()
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `walletButtonsContent emits content when state is present`() = testScenario {
        embeddedContentHelper.walletButtonsContent.test {
            assertThat(awaitItem()).isNull()
            confirmationStateHolder.state = confirmationState()
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `embeddedContent emits null when state is cleared`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            confirmationStateHolder.state = confirmationState()
            assertThat(awaitItem()).isNotNull()
            confirmationStateHolder.state = null
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `walletButtonsContent emits null when state is cleared`() = testScenario {
        embeddedContentHelper.walletButtonsContent.test {
            assertThat(awaitItem()).isNull()
            confirmationStateHolder.state = confirmationState()
            assertThat(awaitItem()).isNotNull()
            confirmationStateHolder.state = null
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `initializing with existing state emits content`() = testScenario(
        initialConfirmationState = confirmationState(),
    ) {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `embeddedContent is not rebuilt on a selection-only confirmation state change`() = testScenario(
        initialConfirmationState = confirmationState(selection = null),
    ) {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNotNull()

            // A selection-only change projects to an equal EmbeddedContentState, so the content flow
            // dedupes via mapAsStateFlow's distinctUntilChanged and doesn't rebuild the payment method list.
            confirmationStateHolder.state = confirmationStateHolder.state?.copy(
                selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            )

            expectNoEvents()
            // The underlying state genuinely changed, so the absence of a re-emission is meaningful.
            assertThat(confirmationStateHolder.state?.selection)
                .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
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
        initialConfirmationState = confirmationState(),
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
        val confirmationState = confirmationState(
            paymentMethodMetadata = paymentMethodMetadata,
            selection = selection,
        )
        testScenario(
            initialConfirmationState = confirmationState,
            setup = {
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
                    embeddedConfirmationState = confirmationState,
                )
            )
            assertThat(errorReporter.getLoggedErrors()).isEmpty()
        }
    }

    private fun confirmationState(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        selection: PaymentSelection? = null,
        configuration: EmbeddedPaymentElement.Configuration =
            EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
    ) = EmbeddedConfirmationStateHolder.State(
        paymentMethodMetadata = paymentMethodMetadata,
        selection = selection,
        configuration = configuration,
    )

    private class Scenario(
        val embeddedContentHelper: DefaultEmbeddedContentHelper,
        val confirmationStateHolder: EmbeddedConfirmationStateHolder,
        val errorReporter: FakeErrorReporter,
    )

    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    private fun testScenario(
        initialConfirmationState: EmbeddedConfirmationStateHolder.State? = null,
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
        val confirmationStateHolder = EmbeddedConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = backgroundScope,
        )
        confirmationStateHolder.state = initialConfirmationState
        val immediateActionHandler = DefaultEmbeddedRowSelectionImmediateActionHandler(
            coroutineScope = backgroundScope,
            internalRowSelectionCallback = { null }
        )

        val embeddedContentHelper = DefaultEmbeddedContentHelper(
            coroutineScope = backgroundScope,
            eventReporter = eventReporter,
            workContext = Dispatchers.Unconfined,
            uiContext = Dispatchers.Unconfined,
            savedPaymentMethodRepository = FakeSavedPaymentMethodRepository(),
            selectionHolder = selectionHolder,
            embeddedLinkHelper = object : EmbeddedLinkHelper {
                override val linkEmail: StateFlow<String?> = stateFlowOf(null)
            },
            embeddedWalletsHelper = { stateFlowOf(null) },
            customerStateHolder = DefaultCustomerStateHolder(
                savedStateHandle = savedStateHandle,
                selection = selectionHolder.selection,
                customerMetadata = stateFlowOf(PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA),
                paymentMethodMetadataFlow = stateFlowOf(null),
            ),
            embeddedFormHelperFactory = embeddedFormHelperFactory,
            confirmationHandler = confirmationHandler,
            confirmationStateDataSource = confirmationStateHolder,
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
            confirmationStateHolder = confirmationStateHolder,
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
