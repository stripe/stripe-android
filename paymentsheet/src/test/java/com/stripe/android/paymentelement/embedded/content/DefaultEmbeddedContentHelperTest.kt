package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
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
    fun `embeddedContent delegates to the data source`() = testScenario {
        assertThat(embeddedContentHelper.embeddedContent).isSameInstanceAs(dataSource.embeddedContent)
    }

    @Test
    fun `walletButtonsContent delegates to the data source`() = testScenario {
        assertThat(embeddedContentHelper.walletButtonsContent).isSameInstanceAs(dataSource.walletButtonsContent)
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

    @Test
    fun `clearSheetLauncher stops launches so presentPaymentOptions reports no launcher`() = testScenario(
        initialConfirmationState = confirmationState(),
    ) {
        embeddedContentHelper.setSheetLauncher(RecordingEmbeddedSheetLauncher())
        embeddedContentHelper.clearSheetLauncher()

        embeddedContentHelper.presentPaymentOptions()
        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            "unexpected_error.embedded.present_payment_options.no_launcher"
        )
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
        val dataSource: FakeEmbeddedContentHelperDataSource,
        val errorReporter: FakeErrorReporter,
    )

    private fun testScenario(
        initialConfirmationState: EmbeddedConfirmationStateHolder.State? = null,
        setup: SavedStateHandle.() -> Unit = {},
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val savedStateHandle = SavedStateHandle().apply { setup() }
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val errorReporter = FakeErrorReporter()
        val dataSource = FakeEmbeddedContentHelperDataSource(
            embeddedConfirmationState = MutableStateFlow(initialConfirmationState),
        )
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA),
            paymentMethodMetadataFlow = stateFlowOf(null),
        )
        val embeddedContentHelper = DefaultEmbeddedContentHelper(
            dataSource = dataSource,
            errorReporter = errorReporter,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            sheetLauncherHolder = EmbeddedSheetLauncherHolder(),
        )
        Scenario(
            embeddedContentHelper = embeddedContentHelper,
            dataSource = dataSource,
            errorReporter = errorReporter,
        ).block()
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
