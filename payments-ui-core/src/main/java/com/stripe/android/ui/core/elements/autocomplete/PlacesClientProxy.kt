package com.stripe.android.ui.core.elements.autocomplete

import android.content.Context
import android.graphics.Typeface
import android.text.style.StyleSpan
import androidx.annotation.RestrictTo
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.stripe.android.BuildConfig
import com.stripe.android.ui.core.elements.autocomplete.model.AddressComponent
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
import com.stripe.android.uicore.elements.DefaultIsPlacesAvailable
import com.stripe.android.uicore.elements.IsPlacesAvailable
import kotlinx.coroutines.tasks.await
import com.google.android.libraries.places.R as PlacesR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PlacesClientProxy {
    suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int
    ): Result<FindAutocompletePredictionsResponse>

    suspend fun fetchPlace(
        placeId: String
    ): Result<FetchPlaceResponse>

    companion object {
        fun create(
            context: Context,
            googlePlacesApiKey: String,
            isPlacesAvailable: IsPlacesAvailable = DefaultIsPlacesAvailable(),
            clientFactory: (Context) -> PlacesClient = { Places.createClient(context) },
            initializer: () -> Unit = { Places.initialize(context, googlePlacesApiKey) }
        ): PlacesClientProxy {
            return if (isPlacesAvailable()) {
                initializer()
                DefaultPlacesClientProxy(
                    clientFactory(context)
                )
            } else {
                UnsupportedPlacesClientProxy()
            }
        }

        fun getPlacesPoweredByGoogleDrawable(
            isSystemDarkTheme: Boolean,
            isPlacesAvailable: IsPlacesAvailable = DefaultIsPlacesAvailable()
        ): Int? {
            return if (isPlacesAvailable()) {
                if (isSystemDarkTheme) {
                    PlacesR.drawable.places_powered_by_google_dark
                } else {
                    PlacesR.drawable.places_powered_by_google_light
                }
            } else {
                if (BuildConfig.DEBUG) {
                    throw IllegalStateException(
                        "Missing Google Places dependency, please add it to your apps build.gradle"
                    )
                }
                null
            }
        }
    }
}

internal class DefaultPlacesClientProxy(
    private val client: PlacesClient
) : PlacesClientProxy {
    private val token = AutocompleteSessionToken.newInstance()

    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int
    ): Result<FindAutocompletePredictionsResponse> {
        return try {
            val response = client.findAutocompletePredictions(
                FindAutocompletePredictionsRequest
                    .builder()
                    .setSessionToken(token)
                    .setQuery(query)
                    .setCountry(country)
                    .setTypeFilter(TypeFilter.ADDRESS)
                    .build()
            ).await()
            Result.success(
                FindAutocompletePredictionsResponse(
                    autocompletePredictions = response.autocompletePredictions.map {
                        AutocompletePrediction(
                            primaryText = it.getPrimaryText(StyleSpan(Typeface.BOLD)),
                            secondaryText = it.getSecondaryText(StyleSpan(Typeface.BOLD)),
                            placeId = it.placeId
                        )
                    }.take(limit ?: response.autocompletePredictions.size)
                )
            )
        } catch (e: Exception) {
            Result.failure(
                Exception("Could not find autocomplete predictions: ${e.message}")
            )
        }
    }

    override suspend fun fetchPlace(
        placeId: String
    ): Result<FetchPlaceResponse> {
        return try {
            val response = client.fetchPlace(
                FetchPlaceRequest.newInstance(
                    placeId,
                    listOf(
                        com.google.android.libraries.places.api.model.Place.Field.ADDRESS_COMPONENTS
                    )
                )
            ).await()
            Result.success(
                FetchPlaceResponse(
                    Place(
                        response.place.addressComponents?.asList()?.map {
                            AddressComponent(
                                shortName = it.shortName,
                                longName = it.name,
                                types = it.types
                            )
                        }
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(
                Exception("Could not fetch place: ${e.message}")
            )
        }
    }
}

internal class UnsupportedPlacesClientProxy : PlacesClientProxy {
    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int
    ): Result<FindAutocompletePredictionsResponse> {
        val exception = IllegalStateException(
            "Missing Google Places dependency, please add it to your apps build.gradle"
        )
        if (BuildConfig.DEBUG) {
            throw exception
        }
        return Result.failure(exception)
    }

    override suspend fun fetchPlace(placeId: String): Result<FetchPlaceResponse> {
        val exception = IllegalStateException(
            "Missing Google Places dependency, please add it to your apps build.gradle"
        )
        if (BuildConfig.DEBUG) {
            throw exception
        }
        return Result.failure(exception)
    }
}
