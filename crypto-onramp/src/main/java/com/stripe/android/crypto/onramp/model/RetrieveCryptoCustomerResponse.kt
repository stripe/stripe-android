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
    val requirements: AdditionalKycRequirementsResponse? = null,
)

@Serializable
internal data class AdditionalKycRequirementsResponse(
    val entries: List<AdditionalKycRequirementResponse> = emptyList(),
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
    val impact: AdditionalKycRequirementImpactResponse? = null,
    @SerialName("requested_reasons")
    val requestedReasons: List<String> = emptyList(),
    val errors: List<AdditionalKycRequirementErrorResponse> = emptyList(),
    @SerialName("submission_type")
    val submissionType: String,
    val document: AdditionalKycDocumentRequirementResponse? = null,
    val questionnaire: AdditionalKycQuestionnaireResponse? = null,
)

@Serializable
internal data class AdditionalKycRequirementImpactResponse(
    @SerialName("restricts_capabilities")
    val restrictsCapabilities: List<AdditionalKycCapabilityImpactResponse> = emptyList(),
)

@Serializable
internal data class AdditionalKycCapabilityImpactResponse(
    val capability: String,
    val restriction: AdditionalKycCapabilityRestrictionResponse,
)

@Serializable
internal data class AdditionalKycCapabilityRestrictionResponse(
    @SerialName("max_transaction_amount")
    val maxTransactionAmount: AdditionalKycAmountResponse? = null,
    @SerialName("lifetime_volume_limit")
    val lifetimeVolumeLimit: AdditionalKycAmountResponse? = null,
    @SerialName("lifetime_volume_threshold")
    val lifetimeVolumeThreshold: AdditionalKycAmountResponse? = null,
    val regions: List<String> = emptyList(),
)

@Serializable
internal data class AdditionalKycAmountResponse(
    val amount: Long? = null,
    val currency: String,
)

@Serializable
internal data class AdditionalKycRequirementErrorResponse(
    val code: String,
    val message: String,
)

@Serializable
internal data class AdditionalKycDocumentRequirementResponse(
    @SerialName("accepted_subtypes")
    val acceptedSubtypes: List<AdditionalKycDocumentSubtypeResponse> = emptyList(),
    @SerialName("accepted_formats")
    val acceptedFormats: List<String> = emptyList(),
    @SerialName("min_documents")
    val minDocuments: Int = 1,
    val instructions: List<String> = emptyList(),
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
    val normalizedQuestionnaire = questionnaire ?: document?.additionalRequirements?.questionnaire

    return AdditionalKycRequirement(
        description = description,
        requestedBy = requestedBy,
        awaitingActionFrom = awaitingActionFrom,
        impact = impact?.toAdditionalKycRequirementImpact(),
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

private fun AdditionalKycRequirementImpactResponse.toAdditionalKycRequirementImpact():
    AdditionalKycRequirementImpact {
    return AdditionalKycRequirementImpact(
        restrictsCapabilities = restrictsCapabilities.map { capabilityImpact ->
            AdditionalKycCapabilityImpact(
                capability = capabilityImpact.capability,
                restriction = capabilityImpact.restriction.toAdditionalKycCapabilityRestriction(),
            )
        }
    )
}

private fun AdditionalKycCapabilityRestrictionResponse.toAdditionalKycCapabilityRestriction():
    AdditionalKycCapabilityRestriction {
    return AdditionalKycCapabilityRestriction(
        maxTransactionAmount = maxTransactionAmount?.toAdditionalKycAmount(),
        lifetimeVolumeLimit = lifetimeVolumeLimit?.toAdditionalKycAmount(),
        lifetimeVolumeThreshold = lifetimeVolumeThreshold?.toAdditionalKycAmount(),
        regions = regions,
    )
}

private fun AdditionalKycAmountResponse.toAdditionalKycAmount(): AdditionalKycAmount {
    return AdditionalKycAmount(
        amount = amount,
        currency = currency,
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
