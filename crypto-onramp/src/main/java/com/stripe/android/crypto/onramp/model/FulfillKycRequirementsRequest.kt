package com.stripe.android.crypto.onramp.model

import com.stripe.android.core.networking.toMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

internal data class FulfillKycRequirementsRequest(
    val requirements: Map<String, KycRequirementSubmissionRequest>,
) {
    fun toParamMap(): Map<String, *> {
        return mapOf(
            "requirements" to requirements.mapValues { (_, requirement) ->
                val params = json.encodeToJsonElement(requirement).jsonObject.toMap()
                // Omit empty document lists for questionnaire-only submissions.
                if (requirement.documents.isEmpty()) params - "documents" else params
            }
        )
    }

    private companion object {
        val json = Json { explicitNulls = false }
    }
}

@Serializable
internal data class KycRequirementSubmissionRequest(
    @SerialName("requested_by")
    val requestedBy: String,
    val documents: List<KycDocumentSubmissionRequest>,
    @SerialName("additional_requirements")
    val collectionRequirements: KycCollectionSubmissionRequest?,
)

@Serializable
internal data class KycDocumentSubmissionRequest(
    @SerialName("document_subtype")
    val documentSubtype: String,
    @SerialName("file_ids")
    val fileIds: List<String>,
)

@Serializable
internal data class KycCollectionSubmissionRequest(
    val questionnaire: KycQuestionnaireSubmissionRequest,
)

@Serializable
internal data class KycQuestionnaireSubmissionRequest(
    val answers: List<KycQuestionnaireAnswerRequest>,
)

@Serializable
internal data class KycQuestionnaireAnswerRequest(
    @SerialName("question_id")
    val questionId: String,
    val value: String,
)
