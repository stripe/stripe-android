package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
internal data class RetrieveAdditionalKycRequirementsResponse(
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
    val errors: List<AdditionalKycRequirementErrorResponse>,
    @Serializable(with = EmptyArrayAsNullDocumentRequirementSerializer::class)
    val document: AdditionalKycDocumentRequirementResponse? = null,
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
    @Serializable(with = EmptyArrayAsNullCollectionRequirementsSerializer::class)
    val additionalRequirements: AdditionalKycCollectionRequirementsResponse? = null,
)

@Serializable
internal data class AdditionalKycDocumentSubtypeResponse(
    val id: String,
    val label: String,
)

@Serializable
internal data class AdditionalKycCollectionRequirementsResponse(
    @Serializable(with = EmptyArrayAsNullQuestionnaireSerializer::class)
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
    return AdditionalKycRequirement(
        description = description,
        requestedBy = requestedBy,
        awaitingActionFrom = awaitingActionFrom,
        errors = errors.map { error ->
            AdditionalKycRequirementError(
                code = error.code,
                developerMessage = error.message,
            )
        },
        document = document?.toAdditionalKycDocumentRequirement(),
        questionnaire = document?.additionalRequirements?.questionnaire?.toAdditionalKycQuestionnaire(),
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

internal object EmptyArrayAsNullDocumentRequirementSerializer :
    EmptyArrayAsNullSerializer<AdditionalKycDocumentRequirementResponse>(
        AdditionalKycDocumentRequirementResponse.serializer()
    )

internal object EmptyArrayAsNullCollectionRequirementsSerializer :
    EmptyArrayAsNullSerializer<AdditionalKycCollectionRequirementsResponse>(
        AdditionalKycCollectionRequirementsResponse.serializer()
    )

internal object EmptyArrayAsNullQuestionnaireSerializer :
    EmptyArrayAsNullSerializer<AdditionalKycQuestionnaireResponse>(
        AdditionalKycQuestionnaireResponse.serializer()
    )

internal abstract class EmptyArrayAsNullSerializer<T>(
    private val valueSerializer: KSerializer<T>,
) : KSerializer<T?> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): T? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer can be used only with JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            JsonNull -> null
            is JsonArray -> {
                if (element.isEmpty()) {
                    null
                } else {
                    throw SerializationException("Expected an object, null, or an empty array")
                }
            }
            else -> jsonDecoder.json.decodeFromJsonElement(valueSerializer, element)
        }
    }

    override fun serialize(encoder: Encoder, value: T?) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer can be used only with JSON")
        val element = value?.let {
            jsonEncoder.json.encodeToJsonElement(valueSerializer, it)
        } ?: JsonNull
        jsonEncoder.encodeJsonElement(element)
    }
}
