package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val defaultInlinePredictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState> =
    MutableStateFlow(AutocompleteAddressInteractor.InlinePredictionsState.Idle).asStateFlow()

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AutocompleteAddressInteractor {
    val autocompleteConfig: Config
    val inlinePredictionsState: StateFlow<InlinePredictionsState>
        get() = defaultInlinePredictionsState

    fun register(onEvent: (Event) -> Unit)

    fun onAutocomplete(country: String)

    fun observeQueryChanges(query: StateFlow<String>, country: StateFlow<String?>) = Unit

    fun onPredictionSelected(predictionId: String) = Unit

    fun onDismissed() = Unit

    fun onEnterManuallyFromInline() = Unit

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Config(
        val googlePlacesApiKey: String?,
        val autocompleteCountries: Set<String>,
        val isPlacesAvailable: Boolean = DefaultIsPlacesAvailable().invoke(),
        val isInlineAutocompleteEnabled: Boolean = false,
        val getAttributionDrawable: ((isDarkTheme: Boolean) -> Int?)? = null,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class InlineAddressPrediction(
        val id: String,
        val primaryText: String,
        val formattedPrimaryText: AnnotatedString,
        val secondaryText: String,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface InlinePredictionsState {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Idle : InlinePredictionsState

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Loading : InlinePredictionsState

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Results(
            val query: String,
            val predictions: List<InlineAddressPrediction>,
        ) : InlinePredictionsState
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
