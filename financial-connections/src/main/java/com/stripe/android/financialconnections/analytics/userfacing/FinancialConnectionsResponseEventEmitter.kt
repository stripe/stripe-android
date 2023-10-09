package com.stripe.android.financialconnections.analytics.userfacing

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.networking.responseJson
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ErrorCode
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Emits user facing events if present on a [StripeResponse].
 */
internal class FinancialConnectionsResponseEventEmitter @Inject constructor(
    private val json: Json,
    private val logger: Logger
) {

    fun emitIfPresent(
        response: StripeResponse<String>
    ) = runCatching {
        response.eventsToEmit()
            ?.let { eventsResponse ->
                json.decodeFromString(
                    ListSerializer(UserFacingEventResponse.serializer()),
                    eventsResponse
                )
                    .mapNotNull { it.toEvent() }
                    .forEach {
                        logger.debug("Emitting event ${it.name} with metadata ${it.metadata}")
                        FinancialConnections.emitEvent(it.name, it.metadata)
                    }
            }
    }.onFailure {
        logger.error("Error decoding event response", it)
    }.getOrNull()

    private fun StripeResponse<String>.eventsToEmit() = when {
        // error responses: events to emit come in the extra_fields error object
        isError -> responseJson()
            .optJSONObject("error")
            ?.optJSONObject("extra_fields")

        // success responses: events to emit come in the root json object
        else -> responseJson()
    }
        ?.optString("events_to_emit")
        ?.takeIf { it.isNotEmpty() }

    private fun UserFacingEventResponse.toEvent() = runCatching {
        FinancialConnectionsEvent(
            name = Name.values().first { it.value == type },
            metadata = Metadata(
                errorCode = error?.errorCode
                    ?.let { errorCode ->
                        ErrorCode.values()
                            .firstOrNull { it.value == errorCode }
                            ?: ErrorCode.UNEXPECTED_ERROR
                    },
                institutionName = institutionSelected?.institutionName,
                manualEntry = success?.manualEntry
            )
        )
    }.onFailure {
        logger.error("Error mapping event response", it)
    }.getOrNull()
}

@Serializable
private data class UserFacingEventResponse(
    @SerialName("type")
    val type: String,
    @SerialName("institution_selected")
    val institutionSelected: InstitutionSelected? = null,
    @SerialName("error")
    val error: Error? = null,
    @SerialName("success")
    val success: Success? = null
) {
    @Serializable
    data class InstitutionSelected(
        @SerialName("institution_name")
        val institutionName: String
    )

    @Serializable
    data class Error(
        @SerialName("error_code")
        val errorCode: String
    )

    @Serializable
    data class Success(
        @SerialName("manual_entry")
        val manualEntry: Boolean
    )
}
