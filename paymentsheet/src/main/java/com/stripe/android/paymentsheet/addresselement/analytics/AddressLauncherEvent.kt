package com.stripe.android.paymentsheet.addresselement.analytics

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.payments.core.analytics.ErrorReporter
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal sealed class AddressLauncherEvent : AnalyticsEvent {
    abstract val additionalParams: Map<String, Any>

    protected fun addressDataBlob(country: String): Map<String, Any> = buildMap {
        if (country.isNotEmpty()) put(FIELD_ADDRESS_COUNTRY_CODE, country)
    }

    class Show(
        val country: String
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_show"
        override val additionalParams: Map<String, Any>
            get() = mapOf(FIELD_ADDRESS_DATA_BLOB to addressDataBlob(country))
    }

    class Completed(
        val country: String,
        private val autocompleteResultSelected: Boolean,
        private val editDistance: Int?,
        private val timeToComplete: Duration?,
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_completed"
        override val additionalParams: Map<String, Any>
            get() {
                val data = mutableMapOf<String, Any>(
                    FIELD_ADDRESS_COUNTRY_CODE to country,
                    FIELD_AUTO_COMPLETE_RESULT_SELECTED to autocompleteResultSelected
                )
                editDistance?.let {
                    data[FIELD_EDIT_DISTANCE] = it
                }
                val params = mutableMapOf<String, Any>(
                    FIELD_ADDRESS_DATA_BLOB to data
                )
                timeToComplete?.let {
                    params[FIELD_MS_TO_COMPLETE] = it.toDouble(DurationUnit.MILLISECONDS)
                }
                return params
            }
    }

    class AutocompleteStarted(
        private val country: String,
        private val autocompleteSessionToken: String,
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_autocomplete_start"
        override val additionalParams: Map<String, Any>
            get() = mapOf(
                FIELD_ADDRESS_DATA_BLOB to addressDataBlob(country),
                FIELD_AUTOCOMPLETE_SESSION_TOKEN to autocompleteSessionToken,
            )
    }

    class AutocompleteSuggestions(
        private val country: String,
        private val autocompleteSessionToken: String,
        private val timeToFetch: Duration?,
        private val resultCount: Int,
        private val sessionElapsed: Duration?,
        private val source: String?,
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_autocomplete_suggestions"
        override val additionalParams: Map<String, Any>
            get() = buildMap {
                put(FIELD_ADDRESS_DATA_BLOB, addressDataBlob(country))
                put(FIELD_AUTOCOMPLETE_SESSION_TOKEN, autocompleteSessionToken)
                put(FIELD_RESULT_COUNT, resultCount)
                source?.let { put(FIELD_SOURCE, it) }
                timeToFetch?.let { put(FIELD_MS_TO_FETCH, it.toDouble(DurationUnit.MILLISECONDS)) }
                sessionElapsed?.let { put(FIELD_MS_SESSION_ELAPSED, it.toDouble(DurationUnit.MILLISECONDS)) }
            }
    }

    class AutocompleteSelected(
        private val country: String,
        private val autocompleteSessionToken: String,
        private val queryLength: Int,
        private val placeId: String?,
        private val source: String?,
        private val sessionElapsed: Duration?,
        private val timeToFetch: Duration?,
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_autocomplete_selected"
        override val additionalParams: Map<String, Any>
            get() = buildMap {
                put(FIELD_ADDRESS_DATA_BLOB, addressDataBlob(country))
                put(FIELD_AUTOCOMPLETE_SESSION_TOKEN, autocompleteSessionToken)
                put(FIELD_QUERY_LENGTH, queryLength)
                placeId?.let { put(FIELD_PLACE_ID, it) }
                source?.let { put(FIELD_SOURCE, it) }
                sessionElapsed?.let { put(FIELD_MS_SESSION_ELAPSED, it.toDouble(DurationUnit.MILLISECONDS)) }
                timeToFetch?.let { put(FIELD_MS_TO_FETCH, it.toDouble(DurationUnit.MILLISECONDS)) }
            }
    }

    class AutocompleteError(
        private val country: String,
        private val autocompleteSessionToken: String,
        private val error: Throwable,
        private val sessionElapsed: Duration?,
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_autocomplete_error"
        override val additionalParams: Map<String, Any>
            get() = buildMap {
                put(FIELD_ADDRESS_DATA_BLOB, addressDataBlob(country))
                put(FIELD_AUTOCOMPLETE_SESSION_TOKEN, autocompleteSessionToken)
                putAll(ErrorReporter.getAdditionalParamsFromError(error))
                sessionElapsed?.let { put(FIELD_MS_SESSION_ELAPSED, it.toDouble(DurationUnit.MILLISECONDS)) }
            }
    }

    internal companion object {
        const val FIELD_ADDRESS_DATA_BLOB = "address_data_blob"
        const val FIELD_ADDRESS_COUNTRY_CODE = "address_country_code"
        const val FIELD_AUTO_COMPLETE_RESULT_SELECTED = "auto_complete_result_selected"
        const val FIELD_EDIT_DISTANCE = "edit_distance"
        const val FIELD_MS_TO_COMPLETE = "ms_to_complete"
        const val FIELD_AUTOCOMPLETE_SESSION_TOKEN = "autocomplete_session_token"
        const val FIELD_MS_TO_FETCH = "ms_to_fetch"
        const val FIELD_RESULT_COUNT = "result_count"
        const val FIELD_MS_SESSION_ELAPSED = "ms_session_elapsed"
        const val FIELD_QUERY_LENGTH = "query_length"
        const val FIELD_PLACE_ID = "place_id"
        const val FIELD_SOURCE = "source"
    }
}
