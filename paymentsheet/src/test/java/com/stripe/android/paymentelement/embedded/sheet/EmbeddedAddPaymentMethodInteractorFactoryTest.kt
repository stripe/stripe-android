package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.FakeTapToAddHelper
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
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

internal class EmbeddedAddPaymentMethodInteractorFactoryTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `created interactor exposes the sorted supported payment methods`() = runScenario {
        val interactor = factory.create()

        assertThat(interactor.state.value.supportedPaymentMethods)
            .isEqualTo(paymentMethodMetadata.sortedSupportedPaymentMethods())
    }

    @Test
    fun `created interactor is not live mode for a test-mode intent`() = runScenario {
        val interactor = factory.create()

        assertThat(interactor.isLiveMode).isFalse()
    }

    @Test
    fun `initially selected code defaults to the first supported payment method`() = runScenario {
        val interactor = factory.create()

        assertThat(interactor.state.value.selectedPaymentMethodCode)
            .isEqualTo(paymentMethodMetadata.supportedPaymentMethodTypes().first())
    }

    @Test
    fun `initially selected code is seeded from the current new selection`() = runScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

        val interactor = factory.create()

        assertThat(interactor.state.value.selectedPaymentMethodCode).isEqualTo("cashapp")
    }

    @Test
    fun `selecting a payment method updates the selected code`() = runScenario {
        val interactor = factory.create()
        assertThat(interactor.state.value.selectedPaymentMethodCode).isEqualTo("card")

        interactor.handleViewAction(
            AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected("cashapp")
        )

        assertThat(interactor.state.value.selectedPaymentMethodCode).isEqualTo("cashapp")
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
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
        // Separate scope so the interactor's long-lived collectors are not awaited by runTest.
        val testScope = TestScope(UnconfinedTestDispatcher())
        val formHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            embeddedSelectionHolder = selectionHolder,
            savedStateHandle = savedStateHandle,
            isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
        )
        val factory = EmbeddedAddPaymentMethodInteractorFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            embeddedSelectionHolder = selectionHolder,
            embeddedFormHelperFactory = formHelperFactory,
            viewModelScope = testScope,
            sheetActivityStateHolder = FakeSheetActivityStateHolder(),
            tapToAddHelper = FakeTapToAddHelper.noOp(),
            eventReporter = FakeEventReporter(),
            customerStateHolder = customerStateHolder,
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
        )

        Scenario(
            factory = factory,
            selectionHolder = selectionHolder,
            paymentMethodMetadata = paymentMethodMetadata,
        ).block()
    }

    private class Scenario(
        val factory: EmbeddedAddPaymentMethodInteractorFactory,
        val selectionHolder: EmbeddedSelectionHolder,
        val paymentMethodMetadata: PaymentMethodMetadata,
    )
}
