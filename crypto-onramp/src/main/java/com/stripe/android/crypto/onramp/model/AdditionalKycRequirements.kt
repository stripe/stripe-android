package com.stripe.android.crypto.onramp.model

internal data class AdditionalKycRequirements(
    val userActionRequired: List<AdditionalKycRequirement>,
    val pendingPartnerAction: List<AdditionalKycRequirement>,
    val pendingStripeAction: List<AdditionalKycRequirement>,
    val unrecognizedActionOwner: List<AdditionalKycRequirement>,
)

internal data class AdditionalKycRequirement(
    val description: String,
    val requestedBy: String,
    val awaitingActionFrom: String,
    val errors: List<AdditionalKycRequirementError>,
    val submissionType: String,
    val document: AdditionalKycDocumentRequirement?,
    val questionnaire: AdditionalKycQuestionnaire?,
)

internal data class AdditionalKycRequirementError(
    val code: String,
    val message: String,
)

internal data class AdditionalKycDocumentRequirement(
    val acceptedSubtypes: List<AdditionalKycDocumentSubtype>,
    val acceptedFormats: List<String>,
    val minDocuments: Int,
    val instructions: List<String>,
)

internal data class AdditionalKycDocumentSubtype(
    val id: String,
    val label: String,
)

internal data class AdditionalKycQuestionnaire(
    val questions: List<AdditionalKycQuestion>,
)

internal data class AdditionalKycQuestion(
    val id: String,
    val prompt: String,
    val answerType: String,
    val required: Boolean,
)
