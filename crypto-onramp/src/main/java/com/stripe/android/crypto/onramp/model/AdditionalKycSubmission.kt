package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
internal data class AdditionalKycSubmission(
    val liquidityProvider: String,
    val submissionType: String,
    val documents: List<AdditionalKycDocumentSubmission>?,
    val questionnaire: AdditionalKycQuestionnaireSubmission?,
) : Parcelable

@Parcelize
internal data class AdditionalKycDocumentSubmission(
    val documentType: String,
    val documentSubtype: String?,
    val files: List<File>,
) : Parcelable

@Parcelize
internal data class AdditionalKycQuestionnaireSubmission(
    val answers: List<AdditionalKycQuestionnaireAnswer>,
) : Parcelable

@Parcelize
internal data class AdditionalKycQuestionnaireAnswer(
    val questionId: String,
    val value: String,
) : Parcelable
