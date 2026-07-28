package com.stripe.android.paymentsheet.addresselement.analytics

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class DefaultAddressLauncherEventReporter @Inject internal constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
    @IOContext private val workContext: CoroutineContext
) : AddressLauncherEventReporter {

    override fun onShow(country: String) {
        durationProvider.start(DurationProvider.Key.AddressElementCompletion, reset = true)
        fireEvent(
            AddressLauncherEvent.Show(
                country = country
            )
        )
    }

    override fun onCompleted(
        country: String,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) {
        val timeToComplete = durationProvider.end(DurationProvider.Key.AddressElementCompletion)
        fireEvent(
            AddressLauncherEvent.Completed(
                country = country,
                autocompleteResultSelected = autocompleteResultSelected,
                editDistance = editDistance,
                timeToComplete = timeToComplete,
            )
        )
    }

    override fun onAutocompleteSessionStarted(sessionToken: String) {
        durationProvider.start(DurationProvider.Key.AddressAutocompleteSession, reset = true)
        fireEvent(
            AddressLauncherEvent.AutocompleteStarted(
                autocompleteSessionToken = sessionToken
            )
        )
    }

    override fun onAutocompleteFetchStarted(sessionToken: String) {
        durationProvider.start(DurationProvider.Key.AddressAutocompleteFetch, reset = true)
    }

    override fun onAutocompleteSuggestionsReturned(
        sessionToken: String,
        resultCount: Int,
    ) {
        val fetchDuration = durationProvider.end(DurationProvider.Key.AddressAutocompleteFetch)
        val sessionElapsed = durationProvider.elapsed(DurationProvider.Key.AddressAutocompleteSession)
        fireEvent(
            AddressLauncherEvent.AutocompleteSuggestions(
                autocompleteSessionToken = sessionToken,
                timeToFetch = fetchDuration,
                resultCount = resultCount,
                sessionElapsed = sessionElapsed,
            )
        )
    }

    override fun onAutocompleteSelected(sessionToken: String, queryLength: Int, placeId: String) {
        fireEvent(
            AddressLauncherEvent.AutocompleteSelected(
                autocompleteSessionToken = sessionToken,
                queryLength = queryLength,
                placeId = placeId,
            )
        )
    }

    override fun onAutocompleteError(sessionToken: String, error: Throwable) {
        fireEvent(
            AddressLauncherEvent.AutocompleteError(
                autocompleteSessionToken = sessionToken,
                error = error,
            )
        )
    }

    private fun fireEvent(event: AddressLauncherEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event,
                    event.additionalParams
                )
            )
        }
    }
}
