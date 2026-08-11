package com.stripe.android.paymentsheet.addresselement.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class AddressLauncherEventTest {

    @Test
    fun `Show event payload contains country in address_data_blob`() {
        val event = AddressLauncherEvent.Show(country = "US")

        assertThat(event.eventName).isEqualTo("mc_address_show")
        assertThat(event.additionalParams).isEqualTo(
            mapOf("address_data_blob" to mapOf("address_country_code" to "US"))
        )
    }

    @Test
    fun `Completed event puts ms_to_complete at root level in milliseconds`() {
        val event = AddressLauncherEvent.Completed(
            country = "US",
            autocompleteResultSelected = true,
            editDistance = 3,
            timeToComplete = 1500.milliseconds,
        )

        assertThat(event.eventName).isEqualTo("mc_address_completed")
        val params = event.additionalParams
        val blob = params["address_data_blob"] as Map<*, *>
        assertThat(blob["address_country_code"]).isEqualTo("US")
        assertThat(blob["auto_complete_result_selected"]).isEqualTo(true)
        assertThat(blob["edit_distance"]).isEqualTo(3)
        assertThat(params["ms_to_complete"]).isEqualTo(1500.0)
    }

    @Test
    fun `AutocompleteStarted event has session token at root and country in blob`() {
        val event = AddressLauncherEvent.AutocompleteStarted(
            country = "CA",
            autocompleteSessionToken = "token-123",
        )

        assertThat(event.eventName).isEqualTo("mc_address_autocomplete_start")
        assertThat(event.additionalParams).isEqualTo(
            mapOf(
                "address_data_blob" to mapOf("address_country_code" to "CA"),
                "autocomplete_session_token" to "token-123",
            )
        )
    }

    @Test
    fun `AutocompleteSuggestions event has timing in ms at root level`() {
        val event = AddressLauncherEvent.AutocompleteSuggestions(
            country = "US",
            autocompleteSessionToken = "token-abc",
            timeToFetch = 250.milliseconds,
            resultCount = 3,
            sessionElapsed = 1200.milliseconds,
            source = "stripe",
        )

        assertThat(event.eventName).isEqualTo("mc_address_autocomplete_suggestions")
        val params = event.additionalParams
        assertThat(params["address_data_blob"]).isEqualTo(mapOf("address_country_code" to "US"))
        assertThat(params["autocomplete_session_token"]).isEqualTo("token-abc")
        assertThat(params["result_count"]).isEqualTo(3)
        assertThat(params["source"]).isEqualTo("stripe")
        assertThat(params["ms_to_fetch"]).isEqualTo(250.0)
        assertThat(params["ms_session_elapsed"]).isEqualTo(1200.0)
    }

    @Test
    fun `AutocompleteSelected event has query_length and timing in ms at root`() {
        val event = AddressLauncherEvent.AutocompleteSelected(
            country = "GB",
            autocompleteSessionToken = "token-xyz",
            queryLength = 8,
            placeId = "place_123",
            source = "google",
            sessionElapsed = 3000.milliseconds,
            timeToFetch = 400.milliseconds,
        )

        assertThat(event.eventName).isEqualTo("mc_address_autocomplete_selected")
        val params = event.additionalParams
        assertThat(params["address_data_blob"]).isEqualTo(mapOf("address_country_code" to "GB"))
        assertThat(params["autocomplete_session_token"]).isEqualTo("token-xyz")
        assertThat(params["query_length"]).isEqualTo(8)
        assertThat(params["place_id"]).isEqualTo("place_123")
        assertThat(params["source"]).isEqualTo("google")
        assertThat(params["ms_session_elapsed"]).isEqualTo(3000.0)
        assertThat(params["ms_to_fetch"]).isEqualTo(400.0)
    }

    @Test
    fun `AutocompleteError event has session elapsed and error params at root`() {
        val event = AddressLauncherEvent.AutocompleteError(
            country = "US",
            autocompleteSessionToken = "token-err",
            error = RuntimeException("Network timeout"),
            sessionElapsed = 5000.milliseconds,
        )

        assertThat(event.eventName).isEqualTo("mc_address_autocomplete_error")
        val params = event.additionalParams
        assertThat(params["address_data_blob"]).isEqualTo(mapOf("address_country_code" to "US"))
        assertThat(params["autocomplete_session_token"]).isEqualTo("token-err")
        assertThat(params["ms_session_elapsed"]).isEqualTo(5000.0)
    }

}
