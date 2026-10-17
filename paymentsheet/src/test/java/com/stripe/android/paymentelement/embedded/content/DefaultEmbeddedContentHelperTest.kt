package com.stripe.android.paymentelement.embedded.content

import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.testing.CoroutineTestRule
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
            state.value = EmbeddedContentHelperStateFactory.create()
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `clearing content closes the current interactor and emits null`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            state.value = EmbeddedContentHelperStateFactory.create()
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
                embeddedAppearance = Embedded(Embedded.RowStyle.FlatWithRadio.default),
            )
            assertThat(awaitItem()).isNotNull()
            val previousInteractor = verticalLayoutInteractors.single()

            state.value = EmbeddedContentHelperStateFactory.create(
                embeddedAppearance = Embedded(Embedded.RowStyle.FloatingButton.default),
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
            embeddedAppearance = Embedded(Embedded.RowStyle.FloatingButton.default),
        )
    ) {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `presentPaymentOptions delegates to presenter`() = testScenario {
        embeddedContentHelper.presentPaymentOptions()
        presenter.presentCalls.awaitItem()
    }

    private class Scenario(
        val embeddedContentHelper: DefaultEmbeddedContentHelper,
        val state: MutableStateFlow<EmbeddedContentHelperStateHolder.State?>,
        val presenter: FakeEmbeddedPaymentOptionsPresenter,
        val verticalLayoutInteractors: List<FakePaymentMethodVerticalLayoutInteractor>,
    )

    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    @Suppress("LongMethod")
    private fun testScenario(
        initialState: EmbeddedContentHelperStateHolder.State? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val state = MutableStateFlow(initialState)
        val presenter = FakeEmbeddedPaymentOptionsPresenter()
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
            embeddedWalletsHelper = { stateFlowOf(null) },
            internalRowSelectionCallback = { null },
            paymentOptionsPresenter = presenter,
        )
        Scenario(
            embeddedContentHelper = embeddedContentHelper,
            state = state,
            presenter = presenter,
            verticalLayoutInteractors = verticalLayoutInteractors,
        ).block()
        verticalLayoutInteractors.forEach(FakePaymentMethodVerticalLayoutInteractor::validate)
        presenter.presentCalls.ensureAllEventsConsumed()
    }

    private class FakeEmbeddedPaymentOptionsPresenter : EmbeddedPaymentOptionsPresenter {
        val presentCalls = Turbine<Unit>()

        override fun present() {
            presentCalls.add(Unit)
        }
    }
}
