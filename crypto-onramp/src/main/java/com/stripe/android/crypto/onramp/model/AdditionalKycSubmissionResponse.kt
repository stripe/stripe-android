package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdditionalKycSubmissionResponse(
    val id: String,
    @SerialName("object")
    val objectType: String? = null,
    @SerialName("liquidity_provider")
    val liquidityProvider: String? = null,
    @SerialName("submission_type")
    val submissionType: String? = null,
    val documents: List<AdditionalKycDocumentSubmissionResponse>? = null,
    val questionnaire: AdditionalKycQuestionnaireSubmissionResponse? = null,
    val status: String? = null,
    @SerialName("submitted_at")
    val submittedAt: Long? = null,
    val created: Long? = null,
)

@Serializable
internal data class AdditionalKycDocumentSubmissionResponse(
    @SerialName("document_type")
    val documentType: String,
    @SerialName("document_subtype")
    val documentSubtype: String? = null,
    @SerialName("file_ids")
    val fileIds: List<String>,
    val status: String? = null,
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
