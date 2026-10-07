package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class KycRequirements(
    val userActionRequired: List<KycRequirement>,
    val pendingPartnerAction: List<KycRequirement>,
    val pendingStripeAction: List<KycRequirement>,
    val unrecognizedActionOwner: List<KycRequirement>,
) : Parcelable

@Parcelize
internal data class KycRequirement(
    val description: String,
    val requestedBy: String,
    val awaitingActionFrom: String,
    val errors: List<KycRequirementError>,
    val document: KycDocumentRequirement?,
    val questionnaire: KycQuestionnaire?,
) : Parcelable

@Parcelize
internal data class KycRequirementError(
    val code: String,
    val developerMessage: String,
) : Parcelable

@Parcelize
internal data class KycDocumentRequirement(
    val acceptedSubtypes: List<KycDocumentSubtype>,
    val acceptedFormats: List<String>,
    val minDocumentTypes: Int,
    val maxDocumentTypes: Int,
    val maxFilesPerDocumentType: Int,
    val maxFileSizeBytes: Long,
    val fileRequirements: String,
    val instructions: List<String>,
) : Parcelable

@Parcelize
internal data class KycDocumentSubtype(
    val id: String,
    val label: String,
    val description: String?,
) : Parcelable

@Parcelize
internal data class KycQuestionnaire(
    val questions: List<KycQuestion>,
) : Parcelable

@Parcelize
internal data class KycQuestion(
    val id: String,
    val prompt: String,
    val answerType: String,
    val required: Boolean,
) : Parcelable
