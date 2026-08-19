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
        return AdditionalKycRequirements(
            userActionRequired = entries.filter { it.awaitingActionFrom == USER },
            pendingPartnerAction = entries.filter { it.awaitingActionFrom == PARTNER },
            pendingStripeAction = entries.filter { it.awaitingActionFrom == STRIPE },
            unrecognizedActionOwner = entries.filter {
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
