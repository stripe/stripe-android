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
import kotlin.time.Duration

@Singleton
internal class DefaultAddressLauncherEventReporter @Inject internal constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
    @IOContext private val workContext: CoroutineContext
) : AddressLauncherEventReporter {

    override fun onShow(country: String) {
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
        timeToComplete: Duration?,
    ) {
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

    override fun onAutocompleteSuggestionsReturned(
        sessionToken: String,
        resultCount: Int,
        fetchDuration: Duration?,
    ) {
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
