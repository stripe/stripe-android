package com.stripe.android.ui.core.elements.autocomplete.model

import android.text.SpannableString
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class FindAutocompletePredictionsResponse(
    val autocompletePredictions: List<AutocompletePrediction>
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AutocompletePrediction(
    val primaryText: SpannableString,
    val secondaryText: SpannableString,
    val placeId: String
)
