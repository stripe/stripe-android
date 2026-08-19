package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdditionalKycSubmissionResponse(
    val id: String,
    @SerialName("object")
    val objectType: String,
    @SerialName("liquidity_provider")
    val liquidityProvider: String,
    @SerialName("submission_type")
    val submissionType: String,
    val documents: List<AdditionalKycDocumentSubmissionResponse>? = null,
    val questionnaire: AdditionalKycQuestionnaireSubmissionResponse? = null,
    @SerialName("submitted_at")
    val submittedAt: Long,
)

@Serializable
internal data class AdditionalKycDocumentSubmissionResponse(
    @SerialName("document_type")
    val documentType: String,
    @SerialName("document_subtype")
    val documentSubtype: String? = null,
    @SerialName("file_ids")
    val fileIds: List<String>,
)

@Serializable
internal data class AdditionalKycQuestionnaireSubmissionResponse(
    val answers: List<AdditionalKycQuestionnaireAnswerResponse>,
)

@Serializable
internal data class AdditionalKycQuestionnaireAnswerResponse(
    @SerialName("question_id")
    val questionId: String,
    val value: String,
)
