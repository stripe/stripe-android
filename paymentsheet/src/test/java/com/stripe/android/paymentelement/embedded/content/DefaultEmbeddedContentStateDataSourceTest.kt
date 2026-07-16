package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedContentStateDataSource.Companion.STATE_KEY_EMBEDDED_CONTENT
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class DefaultEmbeddedContentStateDataSourceTest {
    @Test
    fun `dataLoaded stores state and reports onShowNewPaymentOptions`() = runScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val appearance = Embedded(Embedded.RowStyle.FlatWithRadio.default)

        dataSource.embeddedContentState.test {
            assertThat(awaitItem()).isNull()
            dataSource.dataLoaded(paymentMethodMetadata, appearance, embeddedViewDisplaysMandateText = true)
            assertThat(awaitItem()).isEqualTo(
                EmbeddedContentState(paymentMethodMetadata, appearance, embeddedViewDisplaysMandateText = true)
            )
        }
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `clear resets state to null`() = runScenario {
        dataSource.embeddedContentState.test {
            assertThat(awaitItem()).isNull()
            dataSource.dataLoaded(
                PaymentMethodMetadataFactory.create(),
                Embedded(Embedded.RowStyle.FlatWithRadio.default),
                embeddedViewDisplaysMandateText = true,
            )
            assertThat(awaitItem()).isNotNull()
            dataSource.clear()
            assertThat(awaitItem()).isNull()
        }
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `restores persisted state from savedStateHandle`() {
        val state = EmbeddedContentState(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            appearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            embeddedViewDisplaysMandateText = true,
        )
        runScenario(setup = { set(STATE_KEY_EMBEDDED_CONTENT, state) }) {
            assertThat(dataSource.embeddedContentState.value).isEqualTo(state)
        }
    }

    private class Scenario(
        val dataSource: DefaultEmbeddedContentStateDataSource,
        val eventReporter: FakeEventReporter,
    )

    private fun runScenario(
        setup: SavedStateHandle.() -> Unit = {},
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle().apply { setup() }
        val eventReporter = FakeEventReporter()
        val dataSource = DefaultEmbeddedContentStateDataSource(
            savedStateHandle = savedStateHandle,
            eventReporter = eventReporter,
        )
        Scenario(dataSource, eventReporter).block()
        eventReporter.validate()
    }
}
