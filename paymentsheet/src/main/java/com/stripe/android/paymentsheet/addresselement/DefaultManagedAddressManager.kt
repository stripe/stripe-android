package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.ManagedAddressManager
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

internal class DefaultManagedAddressManager(
    private val launcher: AutocompleteLauncher,
    initialValues: Map<IdentifierSpec, String?>
) : ManagedAddressManager {
    private val id = UUID.randomUUID().toString()

    override val googlePlacesApiKey: String = launcher.googlePlacesApiKey

    override val state = MutableStateFlow(
        if (initialValues[IdentifierSpec.Line1] != null) {
            ManagedAddressManager.State.Expanded(initialValues)
        } else {
            ManagedAddressManager.State.Condensed
        }
    )

    override fun navigateToAutocomplete(country: String) {
        launcher.launch(id, country) {
            state.value = it
        }
    }

    class Factory(
        private val autocompleteLauncher: AutocompleteLauncher,
    ) : ManagedAddressManager.Factory {
        override fun create(initialValues: Map<IdentifierSpec, String?>): ManagedAddressManager {
            return DefaultManagedAddressManager(autocompleteLauncher, initialValues)
        }
    }
}
