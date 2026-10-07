package com.stripe.android.crypto.onramp.ui

import com.stripe.android.crypto.onramp.model.KycQuestionnaire
import com.stripe.android.crypto.onramp.model.KycQuestionnaireAnswer
import com.stripe.android.crypto.onramp.model.KycQuestionnaireSubmission

internal class KycQuestionnaireModel(
    private val questionnaire: KycQuestionnaire?,
) {
    private val questions = questionnaire?.questions.orEmpty()
    private val answers = questions.associate { it.id to "" }.toMutableMap()

    val hasQuestions: Boolean get() = questions.isNotEmpty()
    val hasMissingAnswers: Boolean
        get() = questions.any { it.required && answers[it.id].isNullOrBlank() }

    val state: List<KycQuestionState>
        get() = questions.map {
            KycQuestionState(it.id, it.prompt, answers[it.id].orEmpty(), it.required)
        }

    fun updateAnswer(questionId: String, answer: String): Boolean {
        if (questionId !in answers) return false
        answers[questionId] = limitKycAnswer(answer)
        return true
    }

    fun createSubmission(): KycQuestionnaireSubmission? = questionnaire?.let {
        KycQuestionnaireSubmission(
            answers = questions.mapNotNull { question ->
                answers[question.id]
                    ?.takeIf { it.isNotBlank() || question.required }
                    ?.let { KycQuestionnaireAnswer(question.id, it.trim()) }
            },
        )
    }
}
