package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.inject.Provider

internal class AddressElementViewModelTest {
    @Test
    fun `back does not navigate or cancel when form is disabled`() = runScenario {
        resultStateHolder.setFormEnabled(false)

        viewModel.onBackPressed(onCancel = onCancel)

        onBackCalls.expectNoEvents()
        onCancelCalls.expectNoEvents()
    }

    @Test
    fun `back cancels when navigation cannot go back`() = runScenario {
        viewModel.onBackPressed(onCancel = onCancel)

        assertThat(onBackCalls.awaitItem()).isEqualTo(Unit)
        assertThat(onCancelCalls.awaitItem()).isEqualTo(Unit)
        assertThat(resultStateHolder.result.value).isNull()
    }

    @Test
    fun `back navigates without cancellation when navigation can go back`() = runScenario(
        canNavigateBack = true,
    ) {
        viewModel.onBackPressed(onCancel = onCancel)

        assertThat(onBackCalls.awaitItem()).isEqualTo(Unit)
        onCancelCalls.expectNoEvents()
    }

    @Test
    fun `back can navigate after form is enabled again`() = runScenario(
        canNavigateBack = true,
    ) {
        resultStateHolder.setFormEnabled(false)

        viewModel.onBackPressed(onCancel = onCancel)

        onBackCalls.expectNoEvents()
        onCancelCalls.expectNoEvents()

        resultStateHolder.setFormEnabled(true)
        viewModel.onBackPressed(onCancel = onCancel)

        assertThat(onBackCalls.awaitItem()).isEqualTo(Unit)
        onCancelCalls.expectNoEvents()
    }

    private fun runScenario(
        canNavigateBack: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val onBackCalls = Turbine<Unit>()
        val onCancelCalls = Turbine<Unit>()
        val navigator = mock<NavHostAddressElementNavigator>()
        whenever(navigator.onBack()).thenAnswer {
            onBackCalls.add(Unit)
            canNavigateBack
        }
        val resultStateHolder = AddressElementResultStateHolder()
        val viewModel = AddressElementViewModel(
            navigator = navigator,
            resultStateHolder = resultStateHolder,
            inputAddressViewModelSubcomponentFactoryProvider = Provider { error("Not expected") },
            autoCompleteViewModelSubcomponentFactoryProvider = Provider { error("Not expected") },
        )

        Scenario(
            viewModel = viewModel,
            resultStateHolder = resultStateHolder,
            onBackCalls = onBackCalls,
            onCancel = { onCancelCalls.add(Unit) },
            onCancelCalls = onCancelCalls,
        ).block()

        onBackCalls.ensureAllEventsConsumed()
        onCancelCalls.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val viewModel: AddressElementViewModel,
        val resultStateHolder: AddressElementResultStateHolder,
        val onBackCalls: Turbine<Unit>,
        val onCancel: () -> Unit,
        val onCancelCalls: Turbine<Unit>,
    )
}
