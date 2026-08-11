package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface InlineAutocompleteHandler {
    val predictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState>

    fun onPredictionSelected(predictionId: String)

    fun onDismissed()

    fun onFocusLost()

    fun onFocusGained()

    fun onEnterManually()

    fun getAttributionDrawable(isDarkTheme: Boolean): Int?

    fun onSearchActivated()
}
