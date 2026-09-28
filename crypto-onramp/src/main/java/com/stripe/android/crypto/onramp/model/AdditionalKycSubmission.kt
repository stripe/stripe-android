package com.stripe.android.crypto.onramp.model

import java.io.File

internal data class AdditionalKycSubmission(
    val requirements: Map<String, AdditionalKycRequirementSubmission>,
)

internal data class AdditionalKycRequirementSubmission(
    val requestedBy: String,
    val documents: List<AdditionalKycDocumentSubmission>,
    val questionnaire: AdditionalKycQuestionnaireSubmission?,
)

internal data class AdditionalKycDocumentSubmission(
    val documentSubtype: String?,
    val files: List<File>,
)

internal data class AdditionalKycQuestionnaireSubmission(
    val answers: List<AdditionalKycQuestionnaireAnswer>,
)

internal data class AdditionalKycQuestionnaireAnswer(
    val questionId: String,
    val value: String,
)
