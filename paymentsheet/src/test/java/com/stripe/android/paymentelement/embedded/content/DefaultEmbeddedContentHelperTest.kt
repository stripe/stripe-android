package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.utils.stateFlowOf
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
    fun `clearing content closes the current interactor and emits null`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            state.value = EmbeddedContentHelperStateHolder.State(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                appearance = Embedded(Embedded.RowStyle.FlatWithRadio.default),
                embeddedViewDisplaysMandateText = true,
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            )
            assertThat(awaitItem()).isNotNull()
            val interactor = verticalLayoutInteractors.single()

            state.value = null

            assertThat(awaitItem()).isNull()
            interactor.closeCalls.awaitItem()
        }
    }

    @Test
    fun `replacing content closes only the previous interactor`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            state.value = EmbeddedContentHelperStateFactory.create(
                appearance = Embedded(Embedded.RowStyle.FlatWithRadio.default),
            )
            assertThat(awaitItem()).isNotNull()
            val previousInteractor = verticalLayoutInteractors.single()

            state.value = EmbeddedContentHelperStateFactory.create(
                appearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            )

            assertThat(awaitItem()).isNotNull()
            val replacementInteractor = verticalLayoutInteractors.last()
            previousInteractor.closeCalls.awaitItem()
            replacementInteractor.closeCalls.expectNoEvents()
        }
    }

    @Test
    fun `initializing embeddedContentHelper with paymentMethodMetadata emits correct initial event`() = testScenario(
        initialState = EmbeddedContentHelperStateFactory.create(
            appearance = Embedded(Embedded.RowStyle.FloatingButton.default),
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
        initialState = EmbeddedContentHelperStateFactory.create()
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
            initialState = EmbeddedContentHelperStateFactory.create(
                paymentMethodMetadata = paymentMethodMetadata,
                configuration = configuration,
            ),
            setup = {
                set(CustomerStateHolder.SAVED_CUSTOMER, customerState)
                set(DefaultEmbeddedSelectionHolder.EMBEDDED_SELECTION_KEY, selection)
            }
        ) {
            val fakeLauncher = RecordingEmbeddedSheetLauncher()
            sheetStateHolder.sheetLauncher = fakeLauncher
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
        val sheetStateHolder: SheetStateHolder,
        val errorReporter: FakeErrorReporter,
        val verticalLayoutInteractors: List<FakePaymentMethodVerticalLayoutInteractor>,
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
        val errorReporter = FakeErrorReporter()
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(
                PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA
            ),
            paymentMethodMetadataFlow = stateFlowOf(null),
        )
        val sheetStateHolder = SheetStateHolder(savedStateHandle)

        val state = MutableStateFlow(initialState)
        val verticalLayoutInteractors = mutableListOf<FakePaymentMethodVerticalLayoutInteractor>()
        val verticalLayoutInteractorFactory = EmbeddedPaymentMethodVerticalLayoutInteractorFactory {
            paymentMethodMetadata, _, _, _, _ ->
            FakePaymentMethodVerticalLayoutInteractor.create(paymentMethodMetadata)
                .also(verticalLayoutInteractors::add)
        }

        val embeddedContentHelper = DefaultEmbeddedContentHelper(
            coroutineScope = backgroundScope,
            state = state,
            verticalLayoutInteractorFactory = verticalLayoutInteractorFactory,
            sheetStateHolder = sheetStateHolder,
            embeddedWalletsHelper = { stateFlowOf(null) },
            internalRowSelectionCallback = { null },
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            errorReporter = errorReporter,
        )
        Scenario(
            embeddedContentHelper = embeddedContentHelper,
            state = state,
            sheetStateHolder = sheetStateHolder,
            errorReporter = errorReporter,
            verticalLayoutInteractors = verticalLayoutInteractors,
        ).block()
        verticalLayoutInteractors.forEach(FakePaymentMethodVerticalLayoutInteractor::validate)
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
