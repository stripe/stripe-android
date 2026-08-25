package com.stripe.android.paymentsheet.addresselement

import androidx.navigation.NavHostController
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.injection.AutocompleteViewModelSubcomponent
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.inject.Provider

internal class AddressElementViewModelTest {
    @Test
    fun `clean dismissal exits immediately`() {
        var result: AddressLauncherResult? = null
        val viewModel = createViewModel { result = it }

        viewModel.dismiss()

        assertThat(result).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `dirty dismissal shows confirmation dialog`() {
        var result: AddressLauncherResult? = null
        val coordinator = AddressElementDismissalCoordinator()
        val viewModel = createViewModel(coordinator) { result = it }
        coordinator.setDirty(true)

        viewModel.dismiss()

        assertThat(viewModel.showDiscardConfirmation.value).isTrue()
        assertThat(result).isNull()
    }

    @Test
    fun `keep editing closes dialog and preserves dirty state`() {
        val coordinator = AddressElementDismissalCoordinator()
        val viewModel = createViewModel(coordinator)
        coordinator.setDirty(true)
        viewModel.dismiss()

        viewModel.keepEditing()

        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
        assertThat(coordinator.isDirty.value).isTrue()
    }

    @Test
    fun `discarding changes returns canceled`() {
        var result: AddressLauncherResult? = null
        val coordinator = AddressElementDismissalCoordinator()
        val viewModel = createViewModel(coordinator) { result = it }
        coordinator.setDirty(true)
        viewModel.dismiss()

        viewModel.discardChanges()

        assertThat(result).isEqualTo(AddressLauncherResult.Canceled())
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
        assertThat(coordinator.isDirty.value).isFalse()
    }

    @Test
    fun `repeated dismissal requests do not duplicate confirmation state`() {
        val coordinator = AddressElementDismissalCoordinator()
        val viewModel = createViewModel(coordinator)
        coordinator.setDirty(true)

        viewModel.dismiss()
        viewModel.dismiss()

        assertThat(viewModel.showDiscardConfirmation.value).isTrue()
    }

    @Test
    fun `back from autocomplete does not request dismissal`() {
        var result: AddressLauncherResult? = null
        val coordinator = AddressElementDismissalCoordinator()
        val navigator = NavHostAddressElementNavigator()
        val navigationController = mock<NavHostController>()
        whenever(navigationController.popBackStack()).thenReturn(true)
        navigator.navigationController = navigationController
        val viewModel = createViewModel(coordinator, navigator) { result = it }
        coordinator.setDirty(true)

        viewModel.onBack()

        assertThat(result).isNull()
        assertThat(viewModel.showDiscardConfirmation.value).isFalse()
    }

    private fun createViewModel(
        coordinator: AddressElementDismissalCoordinator = AddressElementDismissalCoordinator(),
        navigator: NavHostAddressElementNavigator = NavHostAddressElementNavigator(),
        onDismiss: (AddressLauncherResult) -> Unit = {},
    ): AddressElementViewModel {
        navigator.onDismiss = onDismiss
        return AddressElementViewModel(
            navigator = navigator,
            inputAddressViewModelSubcomponentFactoryProvider =
            mock<Provider<InputAddressViewModelSubcomponent.Factory>>(),
            autoCompleteViewModelSubcomponentFactoryProvider =
            mock<Provider<AutocompleteViewModelSubcomponent.Factory>>(),
            dismissalCoordinator = coordinator,
        )
    }
}
