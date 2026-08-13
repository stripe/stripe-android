package com.stripe.android.ui.core.elements.autocomplete

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PlacesClientProxy {
    suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int
    ): Result<FindAutocompletePredictionsResponse>

    suspend fun fetchPlace(
        placeId: String,
        locale: Locale,
    ): Result<Address>

    fun resetSession()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @Volatile
        @VisibleForTesting
        var override: PlacesClientProxy? = null
    }
}
