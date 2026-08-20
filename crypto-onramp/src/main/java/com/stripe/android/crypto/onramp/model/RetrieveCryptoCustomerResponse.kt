package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
internal data class RetrieveCryptoCustomerResponse(
    val id: String,
    val requirements: AdditionalKycRequirementsResponse,
)

@Serializable
internal data class AdditionalKycRequirementsResponse(
    val entries: List<AdditionalKycRequirementResponse>,
) {
    fun toAdditionalKycRequirements(): AdditionalKycRequirements {
        val requirements = entries.map { it.toAdditionalKycRequirement() }
        return AdditionalKycRequirements(
            userActionRequired = requirements.filter { it.awaitingActionFrom == USER },
            pendingPartnerAction = requirements.filter { it.awaitingActionFrom == PARTNER },
            pendingStripeAction = requirements.filter { it.awaitingActionFrom == STRIPE },
            unrecognizedActionOwner = requirements.filter {
                it.awaitingActionFrom !in RECOGNIZED_ACTION_OWNERS
            },
        )
    }

    private companion object {
        const val USER = "user"
        const val PARTNER = "partner"
        const val STRIPE = "stripe"
        val RECOGNIZED_ACTION_OWNERS = setOf(USER, PARTNER, STRIPE)
    }
}

@Serializable
internal data class AdditionalKycRequirementResponse(
    val description: String,
    @SerialName("requested_by")
    val requestedBy: String,
    @SerialName("awaiting_action_from")
    val awaitingActionFrom: String,
    @SerialName("requested_reasons")
    val requestedReasons: List<String> = emptyList(),
    val errors: List<AdditionalKycRequirementErrorResponse> = emptyList(),
    @SerialName("submission_type")
    val submissionType: String,
    val document: AdditionalKycDocumentRequirementResponse? = null,
    val questionnaire: AdditionalKycQuestionnaireResponse? = null,
)

@Serializable
internal data class AdditionalKycRequirementErrorResponse(
    val code: String,
    val message: String,
)

@Serializable
internal data class AdditionalKycDocumentRequirementResponse(
    @SerialName("accepted_subtypes")
    val acceptedSubtypes: List<AdditionalKycDocumentSubtypeResponse>,
    @SerialName("accepted_formats")
    val acceptedFormats: List<String>,
    @SerialName("min_documents")
    val minDocuments: Int,
    val instructions: List<String>,
    @SerialName("additional_requirements")
    val additionalRequirements: AdditionalKycCollectionRequirementsResponse? = null,
)

@Serializable
internal data class AdditionalKycDocumentSubtypeResponse(
    val id: String,
    val label: String,
)

@Serializable
internal data class AdditionalKycCollectionRequirementsResponse(
    val questionnaire: AdditionalKycQuestionnaireResponse? = null,
)

@Serializable
internal data class AdditionalKycQuestionnaireResponse(
    val questions: List<AdditionalKycQuestionResponse>,
)

@Serializable
internal data class AdditionalKycQuestionResponse(
    val id: String,
    val prompt: String,
    @SerialName("answer_type")
    val answerType: String,
    val required: Boolean,
)

private fun AdditionalKycRequirementResponse.toAdditionalKycRequirement(): AdditionalKycRequirement {
    val normalizedQuestionnaire = when (submissionType) {
        "document" -> document?.additionalRequirements?.questionnaire
        "questionnaire" -> questionnaire
        else -> null
    }

    return AdditionalKycRequirement(
        description = description,
        requestedBy = requestedBy,
        awaitingActionFrom = awaitingActionFrom,
        requestedReasons = requestedReasons,
        errors = errors.map { error ->
            AdditionalKycRequirementError(
                code = error.code,
                message = error.message,
            )
        },
        submissionType = submissionType,
        document = document?.toAdditionalKycDocumentRequirement(),
        questionnaire = normalizedQuestionnaire?.toAdditionalKycQuestionnaire(),
    )
}

private fun AdditionalKycDocumentRequirementResponse.toAdditionalKycDocumentRequirement():
    AdditionalKycDocumentRequirement {
    return AdditionalKycDocumentRequirement(
        acceptedSubtypes = acceptedSubtypes.map { subtype ->
            AdditionalKycDocumentSubtype(
                id = subtype.id,
                label = subtype.label,
            )
        },
        acceptedFormats = acceptedFormats,
        minDocuments = minDocuments,
        instructions = instructions,
    )
}

private fun AdditionalKycQuestionnaireResponse.toAdditionalKycQuestionnaire(): AdditionalKycQuestionnaire {
    return AdditionalKycQuestionnaire(
        questions = questions.map { question ->
            AdditionalKycQuestion(
                id = question.id,
                prompt = question.prompt,
                answerType = question.answerType,
                required = question.required,
            )
        }
    )
}
