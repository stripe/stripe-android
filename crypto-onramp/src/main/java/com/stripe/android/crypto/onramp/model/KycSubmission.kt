package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
internal data class KycSubmission(
    val requirements: Map<String, KycRequirementSubmission>,
) : Parcelable

@Parcelize
internal data class KycRequirementSubmission(
    val requestedBy: String,
    val documents: List<KycDocumentSubmission>,
    val questionnaire: KycQuestionnaireSubmission?,
) : Parcelable

@Parcelize
internal data class KycDocumentSubmission(
    val documentSubtype: String,
    val files: List<File>,
    val uploadedFileIds: List<String>,
) : Parcelable

@Parcelize
internal data class KycQuestionnaireSubmission(
    val answers: List<KycQuestionnaireAnswer>,
) : Parcelable

@Parcelize
internal data class KycQuestionnaireAnswer(
    val questionId: String,
    val value: String,
) : Parcelable
