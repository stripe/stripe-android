package com.stripe.android.crypto.onramp.ui

import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaire
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireAnswer
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireSubmission

internal class KycQuestionnaireModel(
    private val questionnaire: AdditionalKycQuestionnaire?,
) {
    private val questions = questionnaire?.questions.orEmpty()
    private val answers = questions.associate { it.id to "" }.toMutableMap()

    val hasQuestions: Boolean get() = questions.isNotEmpty()
    val hasMissingAnswers: Boolean
        get() = questions.any { it.required && answers[it.id].isNullOrBlank() }

    val state: List<AdditionalKycQuestionState>
        get() = questions.map {
            AdditionalKycQuestionState(it.id, it.prompt, answers[it.id].orEmpty(), it.required)
        }

    fun updateAnswer(questionId: String, answer: String): Boolean {
        if (questionId !in answers) return false
        answers[questionId] = limitAdditionalKycAnswer(answer)
        return true
    }

    fun createSubmission(): AdditionalKycQuestionnaireSubmission? = questionnaire?.let {
        AdditionalKycQuestionnaireSubmission(
            answers = questions.mapNotNull { question ->
                answers[question.id]
                    ?.takeIf { it.isNotBlank() || question.required }
                    ?.let { AdditionalKycQuestionnaireAnswer(question.id, it.trim()) }
            },
        )
    }
}
