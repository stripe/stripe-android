package com.stripe.android.uicore.elements

import kotlinx.coroutines.flow.StateFlow

interface ManagedAddressManager {
    val googlePlacesApiKey: String

    val state: StateFlow<State>

    fun navigateToAutocomplete(country: String)

    sealed interface State {
        data object Condensed : State

        data class Expanded(val values: Map<IdentifierSpec, String?>?) : State
    }

    interface Factory {
        fun create(initialValues: Map<IdentifierSpec, String?>): ManagedAddressManager
    }
}
