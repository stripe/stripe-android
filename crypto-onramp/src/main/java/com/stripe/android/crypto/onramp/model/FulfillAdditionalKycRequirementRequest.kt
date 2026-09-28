package com.stripe.android.crypto.onramp.model

import com.stripe.android.core.networking.toMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

internal data class FulfillAdditionalKycRequirementRequest(
    val requirements: Map<String, AdditionalKycRequirementSubmissionRequest>,
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
internal data class AdditionalKycRequirementSubmissionRequest(
    @SerialName("requested_by")
    val requestedBy: String,
    val documents: List<AdditionalKycDocumentSubmissionRequest>,
    @SerialName("additional_requirements")
    val additionalRequirements: AdditionalKycCollectionSubmissionRequest?,
)

@Serializable
internal data class AdditionalKycDocumentSubmissionRequest(
    @SerialName("document_subtype")
    val documentSubtype: String,
    @SerialName("file_ids")
    val fileIds: List<String>,
)

@Serializable
internal data class AdditionalKycCollectionSubmissionRequest(
    val questionnaire: AdditionalKycQuestionnaireSubmissionRequest,
)

@Serializable
internal data class AdditionalKycQuestionnaireSubmissionRequest(
    val answers: List<AdditionalKycQuestionnaireAnswerRequest>,
)

@Serializable
internal data class AdditionalKycQuestionnaireAnswerRequest(
    @SerialName("question_id")
    val questionId: String,
    val value: String,
)
