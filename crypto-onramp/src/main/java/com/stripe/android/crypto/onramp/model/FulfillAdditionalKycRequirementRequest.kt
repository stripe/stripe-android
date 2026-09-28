package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FulfillAdditionalKycRequirementRequest(
    val requirements: Map<String, AdditionalKycRequirementSubmissionRequest>,
)

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
    val documentSubtype: String?,
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
