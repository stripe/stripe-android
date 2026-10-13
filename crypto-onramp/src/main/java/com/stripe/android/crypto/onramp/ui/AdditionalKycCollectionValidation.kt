package com.stripe.android.crypto.onramp.ui

import android.icu.text.BreakIterator
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirement

internal fun AdditionalKycRequirement.isSupportedForCollection(): Boolean {
    if (description !in setOf("proof_of_address", "source_of_funds", "source_of_funds_questions")) {
        return false
    }
    val questions = questionnaire?.questions.orEmpty()
    if (questions.any { it.id.isBlank() || it.answerType != "free_text" } ||
        questions.map { it.id }.distinct().size != questions.size
    ) {
        return false
    }
    val document = document ?: return questionnaire != null
    val subtypes = document.acceptedSubtypes
    return document.minDocumentTypes >= 0 &&
        document.maxDocumentTypes >= maxOf(1, document.minDocumentTypes) &&
        document.minDocumentTypes <= subtypes.size &&
        document.maxFileSizeBytes > 0 &&
        document.maxFilesPerDocumentType > 0 &&
        subtypes.isNotEmpty() &&
        subtypes.all { it.id.isNotBlank() } &&
        subtypes.map { it.id }.distinct().size == subtypes.size &&
        (description != "proof_of_address" || document.minDocumentTypes <= 1)
}

internal fun limitAdditionalKycAnswer(answer: String): String {
    if (answer.length <= MAXIMUM_ANSWER_LENGTH) {
        return answer
    }
    val iterator = BreakIterator.getCharacterInstance().apply { setText(answer) }
    var end = iterator.first()
    var scalarCount = 0
    while (true) {
        val next = iterator.next()
        if (next == BreakIterator.DONE) return answer
        val nextCount = answer.codePointCount(end, next)
        if (scalarCount + nextCount > MAXIMUM_ANSWER_LENGTH) return answer.substring(0, end)
        scalarCount += nextCount
        end = next
    }
}

private const val MAXIMUM_ANSWER_LENGTH = 5_000
