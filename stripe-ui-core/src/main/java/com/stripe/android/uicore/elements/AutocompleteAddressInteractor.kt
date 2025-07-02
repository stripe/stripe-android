package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AutocompleteAddressInteractor {
    val interactorScope: CoroutineScope

    val autocompleteConfig: Config

    val autocompleteEvent: SharedFlow<Event>

    fun onAutocomplete(country: String)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Config(
        val googlePlacesApiKey: String?,
        val autocompleteCountries: Set<String>,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Event {
        val values: Map<IdentifierSpec, String?>?

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class OnExpandForm(override val values: Map<IdentifierSpec, String?>?) : Event

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class OnValues(override val values: Map<IdentifierSpec, String?>) : Event
    }
}
