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
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.FakeSavedPaymentMethodRepository
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

internal class DefaultEmbeddedContentHelperTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `embeddedContent is populated when state is set`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            state.value = EmbeddedContentHelperStateHolder.State(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                appearance = Embedded(Embedded.RowStyle.FlatWithRadio.default),
                embeddedViewDisplaysMandateText = true,
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            )
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `embeddedContent emits null when state is set to null`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            state.value = EmbeddedContentHelperStateHolder.State(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                appearance = Embedded(Embedded.RowStyle.FlatWithRadio.default),
                embeddedViewDisplaysMandateText = true,
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            )
            assertThat(awaitItem()).isNotNull()
            state.value = null
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `initializing embeddedContentHelper with paymentMethodMetadata emits correct initial event`() = testScenario(
        initialState = EmbeddedContentHelperStateHolder.State(
            PaymentMethodMetadataFactory.create(),
            Embedded(Embedded.RowStyle.FloatingButton.default),
            embeddedViewDisplaysMandateText = true,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
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
        initialState = EmbeddedContentHelperStateHolder.State(
            PaymentMethodMetadataFactory.create(),
            Embedded(Embedded.RowStyle.FlatWithRadio.default),
            embeddedViewDisplaysMandateText = true,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
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
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        testScenario(
            initialState = EmbeddedContentHelperStateHolder.State(
                paymentMethodMetadata,
                Embedded(Embedded.RowStyle.FlatWithRadio.default),
                embeddedViewDisplaysMandateText = true,
                configuration = configuration,
            ),
            setup = {
                set(CustomerStateHolder.SAVED_CUSTOMER, customerState)
                set(DefaultEmbeddedSelectionHolder.EMBEDDED_SELECTION_KEY, selection)
            }
        ) {
            val fakeLauncher = RecordingEmbeddedSheetLauncher()
            sheetLauncherHolder.sheetLauncher = fakeLauncher
            embeddedContentHelper.presentPaymentOptions()

            assertThat(fakeLauncher.launchPaymentOptionsCalls.single()).isEqualTo(
                RecordingEmbeddedSheetLauncher.LaunchPaymentOptionsCall(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = customerState,
                    selection = selection,
                    configuration = configuration,
                )
            )
            assertThat(errorReporter.getLoggedErrors()).isEmpty()
        }
    }

    private class Scenario(
        val embeddedContentHelper: DefaultEmbeddedContentHelper,
        val state: MutableStateFlow<EmbeddedContentHelperStateHolder.State?>,
        val sheetLauncherHolder: EmbeddedSheetLauncherHolder,
        val errorReporter: FakeErrorReporter,
    )

    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    @Suppress("LongMethod")
    private fun testScenario(
        initialState: EmbeddedContentHelperStateHolder.State? = null,
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
            isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
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
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
        val sheetLauncherHolder = EmbeddedSheetLauncherHolder()

        val state = MutableStateFlow(initialState)
        val savedPaymentMethodMutatorFactory = EmbeddedContentSavedPaymentMethodMutatorFactory(
            eventReporter = eventReporter,
            workContext = Dispatchers.Unconfined,
            uiContext = Dispatchers.Unconfined,
            savedPaymentMethodRepository = FakeSavedPaymentMethodRepository(),
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            linkAccountHolder = linkAccountHolder,
            coroutineScope = backgroundScope,
            sheetLauncherHolder = sheetLauncherHolder,
        )
        val verticalLayoutInteractorFactory = DefaultEmbeddedPaymentMethodVerticalLayoutInteractorFactory(
            eventReporter = eventReporter,
            embeddedFormHelperFactory = embeddedFormHelperFactory,
            confirmationHandler = confirmationHandler,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
            rowSelectionImmediateActionHandler = immediateActionHandler,
            coroutineScope = backgroundScope,
            sheetLauncherHolder = sheetLauncherHolder,
            savedPaymentMethodMutatorFactory = savedPaymentMethodMutatorFactory,
        )

        val embeddedContentHelper = DefaultEmbeddedContentHelper(
            coroutineScope = backgroundScope,
            state = state,
            verticalLayoutInteractorFactory = verticalLayoutInteractorFactory,
            sheetLauncherHolder = sheetLauncherHolder,
            embeddedWalletsHelper = { stateFlowOf(null) },
            internalRowSelectionCallback = { null },
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            errorReporter = errorReporter,
        )
        Scenario(
            embeddedContentHelper = embeddedContentHelper,
            state = state,
            sheetLauncherHolder = sheetLauncherHolder,
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
            configuration: EmbeddedPaymentElement.Configuration?,
            customerState: CustomerState?,
            promotion: PaymentMethodMessagePromotion?,
        ) = error("Not expected.")

        override fun launchManage(
            paymentMethodMetadata: PaymentMethodMetadata,
            customerState: CustomerState,
            selection: PaymentSelection?,
            configuration: EmbeddedPaymentElement.Configuration?,
        ) = error("Not expected.")

        override fun launchPaymentOptions(
            paymentMethodMetadata: PaymentMethodMetadata,
            customerState: CustomerState?,
            selection: PaymentSelection?,
            configuration: EmbeddedPaymentElement.Configuration?,
        ) {
            launchPaymentOptionsCalls.add(
                LaunchPaymentOptionsCall(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = customerState,
                    selection = selection,
                    configuration = configuration,
                )
            )
        }

        data class LaunchPaymentOptionsCall(
            val paymentMethodMetadata: PaymentMethodMetadata,
            val customerState: CustomerState?,
            val selection: PaymentSelection?,
            val configuration: EmbeddedPaymentElement.Configuration?,
        )
    }
}
