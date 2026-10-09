package com.stripe.android.paymentsheet.addresselement

import androidx.navigation.NavHostController
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.addresselement.AddressElementResultStateHolder.State
import com.stripe.android.paymentsheet.injection.AutocompleteViewModelSubcomponent
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import javax.inject.Provider

internal class AddressElementViewModelTest {
    @Test
    fun `dismiss finishes canceled when confirmation is not required`() = runScenario {
        assertThat(viewModel.canDismiss(shouldConfirmDismissal = false)).isTrue()
        assertThat(resultStateHolder.state.value).isEqualTo(State.Idle)

        viewModel.dismiss(shouldConfirmDismissal = false)

        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
        assertThat(resultStateHolder.state.value)
            .isEqualTo(State.Finished(AddressElementActivityContract.Result.Canceled))
    }

    @Test
    fun `dismiss requests confirmation when required and keep editing can retry`() = runScenario {
        viewModel.showDiscardConfirmation.test {
            assertThat(awaitItem()).isFalse()

            viewModel.dismiss(shouldConfirmDismissal = true)

            assertThat(awaitItem()).isTrue()
            assertThat(resultStateHolder.state.value).isEqualTo(State.Idle)

            viewModel.keepEditing()

            assertThat(awaitItem()).isFalse()
            assertThat(resultStateHolder.state.value).isEqualTo(State.Idle)

            viewModel.dismiss(shouldConfirmDismissal = true)

            assertThat(awaitItem()).isTrue()
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `can dismiss requests confirmation when required`() = runScenario {
        viewModel.showDiscardConfirmation.test {
            assertThat(awaitItem()).isFalse()

            assertThat(viewModel.canDismiss(shouldConfirmDismissal = true)).isFalse()

            assertThat(awaitItem()).isTrue()
            assertThat(resultStateHolder.state.value).isEqualTo(State.Idle)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `discard closes confirmation and returns canceled`() = runScenario {
        viewModel.dismiss(shouldConfirmDismissal = true)
        assertThat(viewModel.showDiscardConfirmation.value).isTrue()

        viewModel.discardChanges()

        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
        assertThat(resultStateHolder.state.value)
            .isEqualTo(State.Finished(AddressElementActivityContract.Result.Canceled))
    }

    @Test
    fun `root back requests confirmation when required`() = runScenario {
        viewModel.onBack(shouldConfirmDismissal = true)

        assertThat(viewModel.showDiscardConfirmation.value).isTrue()
        assertThat(resultStateHolder.state.value).isEqualTo(State.Idle)
    }

    @Test
    fun `back from autocomplete navigates without confirmation`() = runScenario {
        whenever(navigationController.previousBackStackEntry).thenReturn(mock())
        whenever(navigationController.popBackStack()).thenReturn(true)

        viewModel.onBack(shouldConfirmDismissal = true)

        verify(navigationController).popBackStack()
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
        assertThat(resultStateHolder.state.value).isEqualTo(State.Idle)
    }

    @Test
    fun `saving blocks dismissal and back without showing confirmation`() = runScenario {
        assertThat(resultStateHolder.tryStartSaving()).isTrue()
        whenever(navigationController.previousBackStackEntry).thenReturn(mock())

        assertThat(viewModel.canDismiss(shouldConfirmDismissal = true)).isFalse()
        viewModel.dismiss(shouldConfirmDismissal = true)
        viewModel.onBack(shouldConfirmDismissal = true)

        verify(navigationController, never()).popBackStack()
        assertThat(resultStateHolder.state.value).isEqualTo(State.Saving)
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
    }

    @Test
    fun `finished result allows hide without replacing successful result`() = runScenario {
        val successfulResult = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())
        assertThat(resultStateHolder.tryStartSaving()).isTrue()
        resultStateHolder.onSaveCompleted(successfulResult)

        assertThat(viewModel.canDismiss(shouldConfirmDismissal = true)).isTrue()
        viewModel.dismiss(shouldConfirmDismissal = true)

        assertThat(resultStateHolder.state.value).isEqualTo(State.Finished(successfulResult))
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val navigationController = mock<NavHostController>()
        val navigator = NavHostAddressElementNavigator().apply {
            this.navigationController = navigationController
        }
        val resultStateHolder = AddressElementResultStateHolder()
        val viewModel = AddressElementViewModel(
            navigator = navigator,
            resultStateHolder = resultStateHolder,
            inputAddressViewModelSubcomponentFactoryProvider =
                mock<Provider<InputAddressViewModelSubcomponent.Factory>>(),
            autoCompleteViewModelSubcomponentFactoryProvider =
                mock<Provider<AutocompleteViewModelSubcomponent.Factory>>(),
        )

        Scenario(
            viewModel = viewModel,
            resultStateHolder = resultStateHolder,
            navigationController = navigationController,
        ).apply { block() }
    }

    private data class Scenario(
        val viewModel: AddressElementViewModel,
        val resultStateHolder: AddressElementResultStateHolder,
        val navigationController: NavHostController,
    )
}
