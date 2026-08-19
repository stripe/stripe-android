package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FulfillAdditionalKycRequirementRequest(
    val credentials: CryptoCustomerRequestParams.Credentials,
    @SerialName("liquidity_provider")
    val liquidityProvider: String,
    @SerialName("submission_type")
    val submissionType: String,
    val documents: List<AdditionalKycDocumentSubmissionRequest>? = null,
    val questionnaire: AdditionalKycQuestionnaireSubmissionRequest? = null,
)

@Serializable
internal data class AdditionalKycDocumentSubmissionRequest(
    @SerialName("document_type")
    val documentType: String,
    @SerialName("document_subtype")
    val documentSubtype: String? = null,
    @SerialName("file_ids")
    val fileIds: List<String>,
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
