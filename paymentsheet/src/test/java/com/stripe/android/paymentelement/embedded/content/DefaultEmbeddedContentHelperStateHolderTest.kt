package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class DefaultEmbeddedContentHelperStateHolderTest {
    @Test
    fun `dataLoaded stores state and reports onShowNewPaymentOptions`() = testScenario {
        stateHolder.state.test {
            assertThat(awaitItem()).isNull()
            val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
            val appearance = Embedded(Embedded.RowStyle.FlatWithRadio.default)
            val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
            stateHolder.dataLoaded(
                paymentMethodMetadata = paymentMethodMetadata,
                appearance = appearance,
                embeddedViewDisplaysMandateText = true,
                configuration = configuration,
            )
            val state = awaitItem()
            assertThat(state?.paymentMethodMetadata).isEqualTo(paymentMethodMetadata)
            assertThat(state?.appearance).isEqualTo(appearance)
            assertThat(state?.embeddedViewDisplaysMandateText).isTrue()
            assertThat(state?.configuration).isEqualTo(configuration)
        }
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `clearEmbeddedContent resets state to null`() = testScenario {
        stateHolder.dataLoaded(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            appearance = Embedded(Embedded.RowStyle.FlatWithRadio.default),
            embeddedViewDisplaysMandateText = true,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
        assertThat(stateHolder.state.value).isNotNull()

        stateHolder.clearEmbeddedContent()

        assertThat(stateHolder.state.value).isNull()
    }

    @Test
    fun `initial state is restored from savedStateHandle`() = testScenario(
        setup = {
            set(
                EmbeddedContentHelperStateHolder.STATE_KEY_EMBEDDED_CONTENT,
                EmbeddedContentHelperStateFactory.create(
                    appearance = Embedded(Embedded.RowStyle.FloatingButton.default),
                    embeddedViewDisplaysMandateText = false,
                )
            )
        }
    ) {
        assertThat(stateHolder.state.value).isNotNull()
    }

    private class Scenario(
        val stateHolder: EmbeddedContentHelperStateHolder,
        val eventReporter: FakeEventReporter,
    )

    private fun testScenario(
        setup: SavedStateHandle.() -> Unit = {},
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val savedStateHandle = SavedStateHandle().apply { setup() }
        val eventReporter = FakeEventReporter()
        val stateHolder = DefaultEmbeddedContentHelperStateHolder(
            savedStateHandle = savedStateHandle,
            eventReporter = eventReporter,
        )
        Scenario(
            stateHolder = stateHolder,
            eventReporter = eventReporter,
        ).block()
        eventReporter.validate()
    }
}
