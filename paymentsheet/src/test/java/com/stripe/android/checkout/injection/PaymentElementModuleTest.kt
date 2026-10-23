package com.stripe.android.checkout.injection

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutControllerState
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelperStateHolder
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class PaymentElementModuleTest {
    @Test
    fun `embedded content is null when checkout state is not loaded`() = runScenario(state = null) {
        assertThat(embeddedContentState.value).isNull()
    }

    @Test
    fun `embedded content is null when there is no embedded configuration`() = runScenario(
        state = CheckoutControllerStateFactory.create(embeddedConfiguration = null),
    ) {
        assertThat(embeddedContentState.value).isNull()
    }

    @Test
    fun `embedded content is null when there is no payment method metadata`() = runScenario(
        state = CheckoutControllerStateFactory.create(paymentMethodMetadata = null),
    ) {
        assertThat(embeddedContentState.value).isNull()
    }

    @Test
    fun `embedded content uses the configured metadata and mandate setting`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .embeddedViewDisplaysMandateText(true)
                .build(),
        ),
    ) {
        val content = requireNotNull(embeddedContentState.value)
        val state = requireNotNull(stateHolder.state)
        assertThat(content.paymentMethodMetadata).isSameInstanceAs(state.paymentMethodMetadata)
        assertThat(content.configuration).isSameInstanceAs(state.embeddedConfiguration)
        assertThat(content.embeddedViewDisplaysMandateText).isTrue()
    }

    @Test
    fun `embedded content omits mandate text when configured`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .embeddedViewDisplaysMandateText(false)
                .build(),
        ),
    ) {
        assertThat(requireNotNull(embeddedContentState.value).embeddedViewDisplaysMandateText).isFalse()
    }

    @Test
    fun `embedded content emits when an embedded configuration becomes available`() = runScenario(
        state = CheckoutControllerStateFactory.create(embeddedConfiguration = null),
    ) {
        embeddedContentState.test {
            assertThat(awaitItem()).isNull()

            val state = CheckoutControllerStateFactory.create()
            stateHolder.state = state

            val content = requireNotNull(awaitItem())
            assertThat(content.paymentMethodMetadata).isSameInstanceAs(state.paymentMethodMetadata)
            assertThat(content.configuration).isSameInstanceAs(state.embeddedConfiguration)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `embedded content is cleared when the embedded configuration is removed`() = runScenario {
        embeddedContentState.test {
            assertThat(awaitItem()).isNotNull()

            stateHolder.state = requireNotNull(stateHolder.state).copy(embeddedConfiguration = null)

            assertThat(awaitItem()).isNull()
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `embedded content is cleared when checkout state is cleared`() = runScenario {
        embeddedContentState.test {
            assertThat(awaitItem()).isNotNull()

            stateHolder.state = null

            assertThat(awaitItem()).isNull()
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `embedded content emits when payment method metadata becomes available`() = runScenario(
        state = CheckoutControllerStateFactory.create(paymentMethodMetadata = null),
    ) {
        embeddedContentState.test {
            assertThat(awaitItem()).isNull()

            val state = CheckoutControllerStateFactory.create()
            stateHolder.state = state

            val content = requireNotNull(awaitItem())
            assertThat(content.paymentMethodMetadata).isSameInstanceAs(state.paymentMethodMetadata)
            assertThat(content.configuration).isSameInstanceAs(state.embeddedConfiguration)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `embedded content is cleared when payment method metadata is removed`() = runScenario {
        embeddedContentState.test {
            assertThat(awaitItem()).isNotNull()

            stateHolder.state = requireNotNull(stateHolder.state).copy(paymentMethodMetadata = null)

            assertThat(awaitItem()).isNull()
            ensureAllEventsConsumed()
        }
    }

    private fun runScenario(
        state: CheckoutControllerState? = CheckoutControllerStateFactory.create(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val errorReporter = FakeErrorReporter()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(
            savedStateHandle = SavedStateHandle(),
            errorReporter = errorReporter,
        ).apply { this.state = state }

        Scenario(
            stateHolder = stateHolder,
            embeddedContentState = PaymentElementModule.provideEmbeddedContentState(stateHolder),
        ).block()

        errorReporter.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val stateHolder: CheckoutControllerStateHolder,
        val embeddedContentState: StateFlow<EmbeddedContentHelperStateHolder.State?>,
    )
}
