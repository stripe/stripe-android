package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

private val defaultInlinePredictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState> =
    stateFlowOf(AutocompleteAddressInteractor.InlinePredictionsState.Idle)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AutocompleteAddressInteractor {
    val autocompleteConfig: Config
    val inlinePredictionsState: StateFlow<InlinePredictionsState>
        get() = defaultInlinePredictionsState

    fun register(onEvent: (Event) -> Unit)

    fun onAutocomplete(country: String)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Config(
        val googlePlacesApiKey: String?,
        val autocompleteCountries: Set<String>,
        val isPlacesAvailable: Boolean = DefaultIsPlacesAvailable().invoke(),
        val isInlineAutocomplete: Boolean = false,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface InlinePredictionsState {
        data object Idle : InlinePredictionsState
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Event {
        val values: Map<IdentifierSpec, String?>?

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class OnExpandForm(override val values: Map<IdentifierSpec, String?>?) : Event

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class OnValues(override val values: Map<IdentifierSpec, String?>) : Event
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface Factory {
        fun create(): AutocompleteAddressInteractor
    }
}
