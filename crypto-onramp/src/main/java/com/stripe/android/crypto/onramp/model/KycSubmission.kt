package com.stripe.android.crypto.onramp.model

internal data class KycSubmission(
    val requirements: Map<String, KycRequirementSubmission>,
)

internal data class KycRequirementSubmission(
    val requestedBy: String,
    val documents: List<KycDocumentSubmission>,
    val questionnaire: KycQuestionnaireSubmission?,
)

internal data class KycDocumentSubmission(
    val documentSubtype: String,
    val uploadedFileIds: List<String>,
)

internal data class KycQuestionnaireSubmission(
    val answers: List<KycQuestionnaireAnswer>,
)

internal data class KycQuestionnaireAnswer(
    val questionId: String,
    val value: String,
)
