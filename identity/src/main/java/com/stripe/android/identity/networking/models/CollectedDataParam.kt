package com.stripe.android.identity.networking.models

import com.stripe.android.core.networking.QueryStringFactory
import com.stripe.android.core.networking.toMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class CollectedDataParam(
    @SerialName("consent")
    val consent: ConsentParam? = null,

    @SerialName("id_document")
    val idDocument: IdDocumentParam? = null
) {
    internal companion object {
        private const val COLLECTED_DATA_PARAM = "collected_data"

        /**
         * Create a x-www-url-encoded string of [CollectedDataParam] with [COLLECTED_DATA_PARAM] as
         * its param name.
         */
        fun CollectedDataParam.createCollectedDataParam(json: Json): String {
            return QueryStringFactory.createFromParamsWithEmptyValues(
                mapOf(
                    COLLECTED_DATA_PARAM to json.encodeToJsonElement(
                        serializer(),
                        this
                    ).toMap()
                )
            )
        }
    }
}
