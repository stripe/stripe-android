package com.stripe.android.paymentsheet.addresselement

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.injection.AutocompleteViewModelSubcomponent
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.FeatureFlagTestRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import javax.inject.Provider

internal class AddressElementViewModelTest {
    @get:Rule
    val unsavedChangesFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enableAddressElementUnsavedChanges,
        isEnabled = true,
    )

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @Test
    fun `clean dismissal exits immediately`() = runScenario {
        viewModel.dismiss()

        assertThat(resultStateHolder.result.value).isEqualTo(AddressElementActivityContract.Result.Canceled)
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
    }

    @Test
    fun `dirty dismissal shows confirmation without returning a result`() = runScenario {
        coordinator.observeChanges(MutableStateFlow(true))

        viewModel.showDiscardConfirmation.test {
            assertThat(awaitItem()).isFalse()

            viewModel.dismiss()

            assertThat(awaitItem()).isTrue()
            assertThat(resultStateHolder.result.value).isNull()
        }
    }

    @Test
    fun `dirty dismissal exits immediately when feature flag is disabled`() = runScenario {
        unsavedChangesFeatureFlagTestRule.setEnabled(false)
        coordinator.observeChanges(MutableStateFlow(true))

        viewModel.dismiss()

        assertThat(resultStateHolder.result.value).isEqualTo(AddressElementActivityContract.Result.Canceled)
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
    }

    @Test
    fun `root back shows confirmation for changed input`() = runScenario {
        val navigationController = mock<NavHostController>()
        navigator.navigationController = navigationController
        coordinator.observeChanges(MutableStateFlow(true))

        viewModel.onBack()

        verify(navigationController, never()).popBackStack()
        assertThat(viewModel.showDiscardConfirmation.value).isTrue()
        assertThat(resultStateHolder.result.value).isNull()
    }

    @Test
    fun `root back exits immediately for unchanged input`() = runScenario {
        val navigationController = mock<NavHostController>()
        navigator.navigationController = navigationController

        viewModel.onBack()

        verify(navigationController, never()).popBackStack()
        assertThat(resultStateHolder.result.value).isEqualTo(AddressElementActivityContract.Result.Canceled)
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
    }

    @Test
    fun `keep editing closes dialog and preserves dirty state`() = runScenario {
        coordinator.observeChanges(MutableStateFlow(true))
        viewModel.dismiss()
        assertThat(viewModel.showDiscardConfirmation.value).isTrue()

        viewModel.keepEditing()

        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
        assertThat(coordinator.isDirty.value).isTrue()
        assertThat(resultStateHolder.result.value).isNull()

        viewModel.dismiss()

        assertThat(viewModel.showDiscardConfirmation.value).isTrue()
    }

    @Test
    fun `discarding changes returns canceled`() = runScenario {
        coordinator.observeChanges(MutableStateFlow(true))
        viewModel.dismiss()
        assertThat(viewModel.showDiscardConfirmation.value).isTrue()

        viewModel.discardChanges()

        assertThat(resultStateHolder.result.value).isEqualTo(AddressElementActivityContract.Result.Canceled)
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
        assertThat(coordinator.isDirty.value).isFalse()
    }

    @Test
    fun `repeated dismissal requests do not duplicate confirmation state`() = runScenario {
        coordinator.observeChanges(MutableStateFlow(true))

        viewModel.showDiscardConfirmation.test {
            assertThat(awaitItem()).isFalse()

            viewModel.dismiss()
            assertThat(awaitItem()).isTrue()

            viewModel.dismiss()
            expectNoEvents()
        }
        assertThat(resultStateHolder.result.value).isNull()
    }

    @Test
    fun `back from autocomplete pops screen without requesting dismissal`() = runScenario {
        val navigationController = mock<NavHostController>()
        whenever(navigationController.previousBackStackEntry).thenReturn(mock<NavBackStackEntry>())
        whenever(navigationController.popBackStack()).thenReturn(true)
        navigator.navigationController = navigationController
        coordinator.observeChanges(MutableStateFlow(true))

        viewModel.onBack()

        verify(navigationController).popBackStack()
        assertThat(resultStateHolder.result.value).isNull()
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
    }

    @Test
    fun `canDismiss prevents the sheet from hiding when input changed`() = runScenario {
        coordinator.observeChanges(MutableStateFlow(true))

        assertThat(viewModel.canDismiss()).isFalse()
        assertThat(viewModel.showDiscardConfirmation.value).isTrue()
        assertThat(resultStateHolder.result.value).isNull()
    }

    @Test
    fun `canDismiss allows the sheet to hide when input is unchanged`() = runScenario {
        assertThat(viewModel.canDismiss()).isTrue()
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
        assertThat(resultStateHolder.result.value).isNull()
    }

    @Test
    fun `canDismiss allows the sheet to hide when feature flag is disabled`() = runScenario {
        unsavedChangesFeatureFlagTestRule.setEnabled(false)
        coordinator.observeChanges(MutableStateFlow(true))

        assertThat(viewModel.canDismiss()).isTrue()
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
    }

    @Test
    fun `dismiss while saving neither cancels nor shows confirmation`() = runScenario {
        coordinator.observeChanges(MutableStateFlow(true))
        coordinator.setSaving(true)

        viewModel.dismiss()

        assertThat(resultStateHolder.result.value).isNull()
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
        assertThat(coordinator.isSaving).isTrue()
    }

    @Test
    fun `canDismiss prevents the sheet from hiding while saving`() = runScenario {
        coordinator.observeChanges(MutableStateFlow(true))
        coordinator.setSaving(true)

        assertThat(viewModel.canDismiss()).isFalse()
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
        assertThat(resultStateHolder.result.value).isNull()
    }

    @Test
    fun `feature flag off preserves dismissal while saving`() = runScenario {
        unsavedChangesFeatureFlagTestRule.setEnabled(false)
        coordinator.observeChanges(MutableStateFlow(true))
        coordinator.setSaving(true)

        viewModel.dismiss()

        assertThat(resultStateHolder.result.value).isEqualTo(AddressElementActivityContract.Result.Canceled)
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
    }

    @Test
    fun `terminal success allows dismissal without showing a confirmation`() = runScenario {
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails(name = "Jane Doe"))
        resultStateHolder.setResult(result)
        coordinator.observeChanges(MutableStateFlow(true))
        coordinator.setSaving(true)

        assertThat(viewModel.canDismiss()).isTrue()
        viewModel.dismiss()

        assertThat(resultStateHolder.result.value).isEqualTo(result)
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
    }

    @Test
    fun `discarding changes preserves an existing success`() = runScenario {
        coordinator.observeChanges(MutableStateFlow(true))
        viewModel.dismiss()
        assertThat(viewModel.showDiscardConfirmation.value).isTrue()
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails(name = "Jane Doe"))
        resultStateHolder.setResult(result)

        viewModel.discardChanges()

        assertThat(resultStateHolder.result.value).isEqualTo(result)
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val navigator = NavHostAddressElementNavigator()
        val resultStateHolder = AddressElementResultStateHolder()
        val coordinator = AddressElementDismissalCoordinator()
        val viewModel = AddressElementViewModel(
            navigator = navigator,
            resultStateHolder = resultStateHolder,
            inputAddressViewModelSubcomponentFactoryProvider =
            mock<Provider<InputAddressViewModelSubcomponent.Factory>>(),
            autoCompleteViewModelSubcomponentFactoryProvider =
            mock<Provider<AutocompleteViewModelSubcomponent.Factory>>(),
            dismissalCoordinator = coordinator,
        ).also { viewModelStoreRule.track(it) }

        Scenario(
            viewModel = viewModel,
            navigator = navigator,
            resultStateHolder = resultStateHolder,
            coordinator = coordinator,
        ).block()
    }

    private data class Scenario(
        val viewModel: AddressElementViewModel,
        val navigator: NavHostAddressElementNavigator,
        val resultStateHolder: AddressElementResultStateHolder,
        val coordinator: AddressElementDismissalCoordinator,
    )
}
