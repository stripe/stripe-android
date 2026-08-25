package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AdditionalKycRequirements(
    val userActionRequired: List<AdditionalKycRequirement>,
    val pendingPartnerAction: List<AdditionalKycRequirement>,
    val pendingStripeAction: List<AdditionalKycRequirement>,
    val unrecognizedActionOwner: List<AdditionalKycRequirement>,
) : Parcelable

@Parcelize
internal data class AdditionalKycRequirement(
    val description: String,
    val requestedBy: String,
    val awaitingActionFrom: String,
    val requestedReasons: List<String>,
    val errors: List<AdditionalKycRequirementError>,
    val submissionType: String,
    val document: AdditionalKycDocumentRequirement?,
    val questionnaire: AdditionalKycQuestionnaire?,
) : Parcelable

@Parcelize
internal data class AdditionalKycRequirementError(
    val code: String,
    val message: String,
) : Parcelable

@Parcelize
internal data class AdditionalKycDocumentRequirement(
    val acceptedSubtypes: List<AdditionalKycDocumentSubtype>,
    val acceptedFormats: List<String>,
    val minDocuments: Int,
    val instructions: List<String>,
) : Parcelable

@Parcelize
internal data class AdditionalKycDocumentSubtype(
    val id: String,
    val label: String,
) : Parcelable

@Parcelize
internal data class AdditionalKycQuestionnaire(
    val questions: List<AdditionalKycQuestion>,
) : Parcelable

@Parcelize
internal data class AdditionalKycQuestion(
    val id: String,
    val prompt: String,
    val answerType: String,
    val required: Boolean,
) : Parcelable
