package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface InlineAutocompleteHandler {
    val predictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState>
    val maxVisiblePredictions: Int?
        get() = null

    fun onPredictionSelected(predictionId: String)

    fun onDismissed()

    fun onEnterManually()

    fun getAttributionDrawable(isDarkTheme: Boolean): Int?
}
