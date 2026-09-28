package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
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
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class DefaultEmbeddedPaymentOptionsPresenterTest {
    @Test
    fun `present reports error when state is null`() = runScenario {
        presenter.present()

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            "unexpected_error.embedded.present_payment_options.not_configured"
        )
    }

    @Test
    fun `present reports error when launcher is null`() = runScenario(
        initialState = EmbeddedContentHelperStateFactory.create(),
    ) {
        presenter.present()

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            "unexpected_error.embedded.present_payment_options.no_launcher"
        )
    }

    @Test
    fun `present launches with current state customer and selection`() {
        val metadata = PaymentMethodMetadataFactory.create()
        val customer = createCustomerState()
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        runScenario(
            initialState = EmbeddedContentHelperStateFactory.create(
                paymentMethodMetadata = metadata,
                configuration = configuration,
            ),
            customer = customer,
            selection = selection,
        ) {
            val launcher = FakeEmbeddedSheetLauncher()
            sheetStateHolder.sheetLauncher = launcher

            presenter.present()

            assertThat(launcher.paymentOptionsCalls.awaitItem()).isEqualTo(
                FakeEmbeddedSheetLauncher.PaymentOptionsCall(metadata, customer, selection, configuration)
            )
            assertThat(errorReporter.getLoggedErrors()).isEmpty()
            launcher.paymentOptionsCalls.ensureAllEventsConsumed()
        }
    }

    private fun runScenario(
        initialState: EmbeddedContentHelperStateHolder.State? = null,
        customer: CustomerState? = null,
        selection: PaymentSelection? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set(CustomerStateHolder.SAVED_CUSTOMER, customer)
            set(DefaultEmbeddedSelectionHolder.EMBEDDED_SELECTION_KEY, selection)
        }
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA),
            paymentMethodMetadataFlow = stateFlowOf(null),
        )
        val sheetStateHolder = SheetStateHolder(savedStateHandle)
        val errorReporter = FakeErrorReporter()
        val presenter = DefaultEmbeddedPaymentOptionsPresenter(
            state = MutableStateFlow(initialState),
            sheetStateHolder = sheetStateHolder,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            errorReporter = errorReporter,
        )

        Scenario(presenter, sheetStateHolder, errorReporter).block()
    }

    private data class Scenario(
        val presenter: DefaultEmbeddedPaymentOptionsPresenter,
        val sheetStateHolder: SheetStateHolder,
        val errorReporter: FakeErrorReporter,
    )

    private class FakeEmbeddedSheetLauncher : EmbeddedSheetLauncher {
        val paymentOptionsCalls = Turbine<PaymentOptionsCall>()

        override fun launchForm(
            code: String,
            paymentMethodMetadata: PaymentMethodMetadata,
            configuration: EmbeddedPaymentElement.Configuration?,
            customerState: CustomerState?,
            promotion: PaymentMethodMessagePromotion?,
        ) = error("Not expected")

        override fun launchManage(
            paymentMethodMetadata: PaymentMethodMetadata,
            customerState: CustomerState,
            selection: PaymentSelection?,
            configuration: EmbeddedPaymentElement.Configuration?,
        ) = error("Not expected")

        override fun launchPaymentOptions(
            paymentMethodMetadata: PaymentMethodMetadata,
            customerState: CustomerState?,
            selection: PaymentSelection?,
            configuration: EmbeddedPaymentElement.Configuration?,
        ) {
            paymentOptionsCalls.add(
                PaymentOptionsCall(paymentMethodMetadata, customerState, selection, configuration)
            )
        }

        data class PaymentOptionsCall(
            val paymentMethodMetadata: PaymentMethodMetadata,
            val customerState: CustomerState?,
            val selection: PaymentSelection?,
            val configuration: EmbeddedPaymentElement.Configuration?,
        )
    }
}
