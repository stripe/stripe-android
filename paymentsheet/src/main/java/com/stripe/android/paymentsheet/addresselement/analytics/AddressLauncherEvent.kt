package com.stripe.android.paymentsheet.addresselement.analytics

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.payments.core.analytics.ErrorReporter
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal sealed class AddressLauncherEvent : AnalyticsEvent {
    abstract val additionalParams: Map<String, Any>

    class Show(
        val country: String
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_show"
        override val additionalParams: Map<String, Any>
            get() {
                return mapOf(
                    FIELD_ADDRESS_DATA_BLOB to mapOf(
                        FIELD_ADDRESS_COUNTRY_CODE to country
                    )
                )
            }
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
                timeToComplete?.let {
                    data[FIELD_TIME_TO_COMPLETE] = it.toDouble(DurationUnit.SECONDS)
                }
                return mapOf(
                    FIELD_ADDRESS_DATA_BLOB to data
                )
            }
    }

    class AutocompleteStarted(
        private val autocompleteSessionToken: String,
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_autocomplete_start"
        override val additionalParams: Map<String, Any>
            get() = mapOf(
                FIELD_ADDRESS_DATA_BLOB to mapOf(
                    FIELD_AUTOCOMPLETE_SESSION_TOKEN to autocompleteSessionToken,
                    FIELD_SOURCE to SOURCE_STRIPE_HOSTED,
                    FIELD_ADDRESS_INPUT_MODE to ADDRESS_INPUT_MODE_AUTOCOMPLETE,
                )
            )
    }

    class AutocompleteSuggestions(
        private val autocompleteSessionToken: String,
        private val timeToFetch: Duration?,
        private val resultCount: Int,
        private val sessionElapsed: Duration?,
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_autocomplete_suggestions"
        override val additionalParams: Map<String, Any>
            get() {
                val data = mutableMapOf<String, Any>(
                    FIELD_AUTOCOMPLETE_SESSION_TOKEN to autocompleteSessionToken,
                    FIELD_SOURCE to SOURCE_STRIPE_HOSTED,
                    FIELD_RESULT_COUNT to resultCount,
                )
                timeToFetch?.let { data[FIELD_TIME_TO_FETCH] = it.toDouble(DurationUnit.SECONDS) }
                sessionElapsed?.let { data[FIELD_SESSION_ELAPSED] = it.toDouble(DurationUnit.SECONDS) }
                return mapOf(FIELD_ADDRESS_DATA_BLOB to data)
            }
    }

    class AutocompleteSelected(
        private val autocompleteSessionToken: String,
        private val queryLength: Int,
        private val placeId: String?,
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_autocomplete_selected"
        override val additionalParams: Map<String, Any>
            get() {
                val data = mutableMapOf<String, Any>(
                    FIELD_AUTOCOMPLETE_SESSION_TOKEN to autocompleteSessionToken,
                    FIELD_SOURCE to SOURCE_STRIPE_HOSTED,
                    FIELD_QUERY_LENGTH to queryLength,
                )
                placeId?.let { data[FIELD_PLACE_ID] = it }
                return mapOf(FIELD_ADDRESS_DATA_BLOB to data)
            }
    }

    class AutocompleteError(
        private val autocompleteSessionToken: String,
        private val error: Throwable,
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_autocomplete_error"
        override val additionalParams: Map<String, Any>
            get() {
                val data = buildMap<String, Any> {
                    put(FIELD_AUTOCOMPLETE_SESSION_TOKEN, autocompleteSessionToken)
                    putAll(ErrorReporter.getAdditionalParamsFromError(error))
                }
                return mapOf(FIELD_ADDRESS_DATA_BLOB to data)
            }
    }

    internal companion object {
        const val FIELD_ADDRESS_DATA_BLOB = "address_data_blob"
        const val FIELD_ADDRESS_COUNTRY_CODE = "address_country_code"
        const val FIELD_AUTO_COMPLETE_RESULT_SELECTED = "auto_complete_result_selected"
        const val FIELD_EDIT_DISTANCE = "edit_distance"
        const val FIELD_TIME_TO_COMPLETE = "time_to_complete"
        const val FIELD_AUTOCOMPLETE_SESSION_TOKEN = "autocomplete_session_token"
        const val FIELD_SOURCE = "source"
        const val FIELD_ADDRESS_INPUT_MODE = "address_input_mode"
        const val FIELD_TIME_TO_FETCH = "time_to_fetch"
        const val FIELD_RESULT_COUNT = "result_count"
        const val FIELD_SESSION_ELAPSED = "session_elapsed"
        const val FIELD_QUERY_LENGTH = "query_length"
        const val FIELD_PLACE_ID = "place_id"

        private const val SOURCE_STRIPE_HOSTED = "stripe_hosted"
        private const val ADDRESS_INPUT_MODE_AUTOCOMPLETE = "autocomplete"
    }
}
