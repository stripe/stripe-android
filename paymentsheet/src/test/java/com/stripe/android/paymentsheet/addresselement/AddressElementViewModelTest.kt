package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.Turbine
import app.cash.turbine.test
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

        resultStateHolder.result.test {
            assertThat(awaitItem()).isNull()

            viewModel.onBackPressed()

            onBackCalls.expectNoEvents()
            expectNoEvents()
        }
    }

    @Test
    fun `back cancels when navigation cannot go back`() = runScenario {
        resultStateHolder.result.test {
            assertThat(awaitItem()).isNull()

            viewModel.onBackPressed()

            assertThat(onBackCalls.awaitItem()).isEqualTo(Unit)
            assertThat(awaitItem()).isEqualTo(AddressElementActivityContract.Result.Canceled)
        }
    }

    @Test
    fun `back navigates without cancellation when navigation can go back`() = runScenario(
        canNavigateBack = true,
    ) {
        resultStateHolder.result.test {
            assertThat(awaitItem()).isNull()

            viewModel.onBackPressed()

            assertThat(onBackCalls.awaitItem()).isEqualTo(Unit)
            expectNoEvents()
        }
    }

    @Test
    fun `back can navigate after form is enabled again`() = runScenario(
        canNavigateBack = true,
    ) {
        resultStateHolder.setFormEnabled(false)

        resultStateHolder.result.test {
            assertThat(awaitItem()).isNull()

            viewModel.onBackPressed()

            onBackCalls.expectNoEvents()
            expectNoEvents()

            resultStateHolder.setFormEnabled(true)
            viewModel.onBackPressed()

            assertThat(onBackCalls.awaitItem()).isEqualTo(Unit)
            expectNoEvents()
        }
    }

    private fun runScenario(
        canNavigateBack: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val onBackCalls = Turbine<Unit>()
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
        ).block()

        onBackCalls.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val viewModel: AddressElementViewModel,
        val resultStateHolder: AddressElementResultStateHolder,
        val onBackCalls: Turbine<Unit>,
    )
}
