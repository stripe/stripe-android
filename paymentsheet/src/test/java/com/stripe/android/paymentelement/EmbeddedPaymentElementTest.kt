package com.stripe.android.paymentelement

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfigurationCoordinator
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationHelper
import com.stripe.android.paymentelement.embedded.content.FakeEmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.FakeEmbeddedStateHelper
import com.stripe.android.paymentelement.embedded.content.PaymentOptionDisplayDataHolder
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class EmbeddedPaymentElementTest {
    @Test
    fun `clearPaymentOption clears the active bank and all remembered drafts`() = runScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        selectionHolder.setSelection(PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION)
        assertThat(selectionHolder.previousNewSelections.size()).isEqualTo(2)

        selectionHolder.selection.test {
            assertThat(awaitItem()).isEqualTo(PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION)

            embeddedPaymentElement.clearPaymentOption()

            assertThat(awaitItem()).isNull()
        }
        assertThat(selectionHolder.getPreviousNewSelection("us_bank_account")).isNull()
        assertThat(selectionHolder.getPreviousNewSelection("card")).isNull()
        val restoredHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        assertThat(restoredHolder.selection.value).isNull()
        assertThat(restoredHolder.previousNewSelections.isEmpty).isTrue()
    }

    @Test
    fun `clearPaymentOption clears a remembered bank when no option is selected`() = runScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION)
        selectionHolder.setSelection(null)
        assertThat(selectionHolder.getPreviousNewSelection("us_bank_account")).isNotNull()

        embeddedPaymentElement.clearPaymentOption()

        assertThat(selectionHolder.selection.value).isNull()
        assertThat(selectionHolder.getPreviousNewSelection("us_bank_account")).isNull()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val confirmationHelper = FakeEmbeddedConfirmationHelper()
        val contentHelper = FakeEmbeddedContentHelper()
        val configurationCoordinator = FakeEmbeddedConfigurationCoordinator()
        val stateHelper = FakeEmbeddedStateHelper()
        val embeddedPaymentElement = EmbeddedPaymentElement(
            confirmationHelper = confirmationHelper,
            contentHelper = contentHelper,
            selectionHolder = selectionHolder,
            paymentOptionDisplayDataHolder = FakePaymentOptionDisplayDataHolder(),
            configurationCoordinator = configurationCoordinator,
            stateHelper = stateHelper,
        )

        Scenario(
            embeddedPaymentElement = embeddedPaymentElement,
            selectionHolder = selectionHolder,
            savedStateHandle = savedStateHandle,
        ).block()

        confirmationHelper.confirmCalls.ensureAllEventsConsumed()
        contentHelper.presentPaymentOptionsCalls.ensureAllEventsConsumed()
        configurationCoordinator.configureCalls.ensureAllEventsConsumed()
        stateHelper.validate()
    }

    private class Scenario(
        val embeddedPaymentElement: EmbeddedPaymentElement,
        val selectionHolder: DefaultEmbeddedSelectionHolder,
        val savedStateHandle: SavedStateHandle,
    )

    internal class FakeEmbeddedConfirmationHelper : EmbeddedConfirmationHelper {
        val confirmCalls = Turbine<Unit>()

        override fun confirm() {
            confirmCalls.add(Unit)
        }
    }

    internal class FakeEmbeddedConfigurationCoordinator : EmbeddedConfigurationCoordinator {
        val configureCalls = Turbine<ConfigureCall>()

        override suspend fun configure(
            configuration: EmbeddedPaymentElement.Configuration,
            initializationMode: PaymentElementLoader.InitializationMode,
        ): EmbeddedPaymentElement.ConfigureResult {
            configureCalls.add(ConfigureCall(configuration, initializationMode))
            return EmbeddedPaymentElement.ConfigureResult.Succeeded()
        }

        data class ConfigureCall(
            val configuration: EmbeddedPaymentElement.Configuration,
            val initializationMode: PaymentElementLoader.InitializationMode,
        )
    }

    internal class FakePaymentOptionDisplayDataHolder : PaymentOptionDisplayDataHolder {
        override val paymentOption = stateFlowOf<EmbeddedPaymentElement.PaymentOptionDisplayData?>(null)
    }
}
