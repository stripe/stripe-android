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
internal data class RetrieveKycRequirementsResponse(
    val requirements: KycRequirementsResponse,
)

@JvmInline
@Serializable
internal value class KycRequirementsResponse(
    val entries: Map<String, KycRequirementResponse>,
) {
    fun toKycRequirements(): KycRequirements {
        val requirements = entries.map { (name, requirement) -> requirement.toKycRequirement(name) }
        return KycRequirements(
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
internal data class KycRequirementResponse(
    @SerialName("requested_by")
    val requestedBy: String,
    @SerialName("awaiting_action_from")
    val awaitingActionFrom: String,
    val errors: List<KycRequirementErrorResponse>,
    @Serializable(with = EmptyArrayAsNullDocumentRequirementSerializer::class)
    val document: KycDocumentRequirementResponse? = null,
    @SerialName("additional_requirements")
    @Serializable(with = EmptyArrayAsNullCollectionRequirementsSerializer::class)
    val collectionRequirements: KycCollectionRequirementsResponse? = null,
)

@Serializable
internal data class KycRequirementErrorResponse(
    val code: String,
    val description: String,
)

@Serializable
internal data class KycDocumentRequirementResponse(
    @SerialName("accepted_subtypes")
    val acceptedSubtypes: List<KycDocumentSubtypeResponse>,
    @SerialName("accepted_formats")
    val acceptedFormats: List<String>,
    @SerialName("max_file_size_bytes")
    val maxFileSizeBytes: Long,
    @SerialName("min_document_types")
    val minDocumentTypes: Int,
    @SerialName("max_document_types")
    val maxDocumentTypes: Int,
    @SerialName("max_files_per_document_type")
    val maxFilesPerDocumentType: Int? = null,
    @SerialName("file_requirements")
    val fileRequirements: String,
    val instructions: List<String>,
)

@Serializable
internal data class KycDocumentSubtypeResponse(
    val id: String,
    val label: String,
    val description: String? = null,
)

@Serializable
internal data class KycCollectionRequirementsResponse(
    @Serializable(with = EmptyArrayAsNullQuestionnaireSerializer::class)
    val questionnaire: KycQuestionnaireResponse? = null,
)

@Serializable
internal data class KycQuestionnaireResponse(
    val questions: List<KycQuestionResponse>,
)

@Serializable
internal data class KycQuestionResponse(
    val id: String,
    val prompt: String,
    @SerialName("answer_type")
    val answerType: String,
    val required: Boolean,
)

private fun KycRequirementResponse.toKycRequirement(name: String): KycRequirement {
    return KycRequirement(
        description = name,
        requestedBy = requestedBy,
        awaitingActionFrom = awaitingActionFrom,
        errors = errors.map { error ->
            KycRequirementError(
                code = error.code,
                developerMessage = error.description,
            )
        },
        document = document?.toKycDocumentRequirement(),
        questionnaire = collectionRequirements?.questionnaire?.toKycQuestionnaire(),
    )
}

private fun KycDocumentRequirementResponse.toKycDocumentRequirement():
    KycDocumentRequirement {
    return KycDocumentRequirement(
        acceptedSubtypes = acceptedSubtypes.map { subtype ->
            KycDocumentSubtype(
                id = subtype.id,
                label = subtype.label,
                description = subtype.description,
            )
        },
        acceptedFormats = acceptedFormats,
        minDocumentTypes = minDocumentTypes,
        maxDocumentTypes = maxDocumentTypes,
        maxFilesPerDocumentType = maxFilesPerDocumentType ?: 10,
        maxFileSizeBytes = maxFileSizeBytes,
        fileRequirements = fileRequirements,
        instructions = instructions,
    )
}

private fun KycQuestionnaireResponse.toKycQuestionnaire(): KycQuestionnaire {
    return KycQuestionnaire(
        questions = questions.map { question ->
            KycQuestion(
                id = question.id,
                prompt = question.prompt,
                answerType = question.answerType,
                required = question.required,
            )
        }
    )
}

internal object EmptyArrayAsNullDocumentRequirementSerializer :
    EmptyArrayAsNullSerializer<KycDocumentRequirementResponse>(
        KycDocumentRequirementResponse.serializer()
    )

internal object EmptyArrayAsNullCollectionRequirementsSerializer :
    EmptyArrayAsNullSerializer<KycCollectionRequirementsResponse>(
        KycCollectionRequirementsResponse.serializer()
    )

internal object EmptyArrayAsNullQuestionnaireSerializer :
    EmptyArrayAsNullSerializer<KycQuestionnaireResponse>(
        KycQuestionnaireResponse.serializer()
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
