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

    override fun onAutocompleteSessionStarted(country: String, sessionToken: String) {
        durationProvider.start(DurationProvider.Key.AddressAutocompleteSession, reset = true)
        fireEvent(
            AddressLauncherEvent.AutocompleteStarted(
                country = country,
                autocompleteSessionToken = sessionToken,
            )
        )
    }

    override fun onAutocompleteFetchStarted() {
        durationProvider.start(DurationProvider.Key.AddressAutocompleteFetch, reset = true)
    }

    override fun onAutocompleteDetailsFetchStarted() {
        durationProvider.start(DurationProvider.Key.AddressAutocompleteDetailsFetch, reset = true)
    }

    override fun onAutocompleteSuggestionsReturned(
        country: String,
        sessionToken: String,
        resultCount: Int,
        source: String?,
    ) {
        val fetchDuration = durationProvider.end(DurationProvider.Key.AddressAutocompleteFetch)
        val sessionElapsed = durationProvider.elapsed(DurationProvider.Key.AddressAutocompleteSession)
        fireEvent(
            AddressLauncherEvent.AutocompleteSuggestions(
                country = country,
                autocompleteSessionToken = sessionToken,
                timeToFetch = fetchDuration,
                resultCount = resultCount,
                sessionElapsed = sessionElapsed,
                source = source,
            )
        )
    }

    override fun onAutocompleteSelected(
        country: String,
        sessionToken: String,
        queryLength: Int,
        placeId: String?,
        source: String?,
    ) {
        val sessionElapsed = durationProvider.elapsed(DurationProvider.Key.AddressAutocompleteSession)
        val timeToFetch = durationProvider.end(DurationProvider.Key.AddressAutocompleteDetailsFetch)
        fireEvent(
            AddressLauncherEvent.AutocompleteSelected(
                country = country,
                autocompleteSessionToken = sessionToken,
                queryLength = queryLength,
                placeId = placeId,
                source = source,
                sessionElapsed = sessionElapsed,
                timeToFetch = timeToFetch,
            )
        )
    }

    override fun onAutocompleteError(country: String, sessionToken: String, error: Throwable) {
        val sessionElapsed = durationProvider.elapsed(DurationProvider.Key.AddressAutocompleteSession)
        fireEvent(
            AddressLauncherEvent.AutocompleteError(
                country = country,
                autocompleteSessionToken = sessionToken,
                error = error,
                sessionElapsed = sessionElapsed,
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
