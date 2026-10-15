package com.stripe.android.crypto.onramp.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.RetrieveKycRequirementsResponse
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class KycCollectionValidationTest {
    @Test
    fun `answers stop at five thousand characters`() {
        assertThat(limitKycAnswer("a".repeat(5_001), 5_000)).isEqualTo("a".repeat(5_000))
    }

    @Test
    fun `answer limit preserves composed characters and emoji`() {
        val character = "👨‍👩‍👧‍👦"
        assertThat(limitKycAnswer(character.repeat(5_001), 5_000)).isEqualTo(character.repeat(714))
        assertThat(limitKycAnswer("e\u0301".repeat(5_001), 5_000)).isEqualTo("e\u0301".repeat(2_500))
    }

    @Test
    fun `short answers remain unchanged`() {
        assertThat(limitKycAnswer("Savings", 5_000)).isEqualTo("Savings")
    }

    @Test
    fun `unknown answer types are unsupported`() {
        val requirement = requirement()
        val questionnaire = requireNotNull(requirement.questionnaire)
        val unsupported = requirement.copy(
            questionnaire = questionnaire.copy(
                questions = questionnaire.questions.map { it.copy(answerType = "future") }
            )
        )
        assertThat(unsupported.isSupportedForCollection()).isFalse()
    }

    @Test
    fun `duplicate question identifiers are unsupported`() {
        val requirement = requirement()
        val questionnaire = requireNotNull(requirement.questionnaire)
        val unsupported = requirement.copy(
            questionnaire = questionnaire.copy(
                questions = listOf(questionnaire.questions.first(), questionnaire.questions.first())
            )
        )
        assertThat(unsupported.isSupportedForCollection()).isFalse()
    }

    @Test
    fun `impossible document type bounds are unsupported`() {
        val requirement = requirement()
        val unsupported = requirement.copy(
            document = requireNotNull(requirement.document).copy(minDocumentTypes = 3, maxDocumentTypes = 1)
        )
        assertThat(unsupported.isSupportedForCollection()).isFalse()
    }

    @Test
    fun `scalar boundary never splits a composed character`() {
        assertThat(limitKycAnswer("a".repeat(4_999) + "e\u0301", 5_000))
            .isEqualTo("a".repeat(4_999))
        assertThat(limitKycAnswer("😀".repeat(5_001), 5_000)).isEqualTo("😀".repeat(5_000))
    }

    @Test
    fun `nonpositive file allowance is unsupported`() {
        val requirement = requirement()
        val unsupported = requirement.copy(
            document = requireNotNull(requirement.document).copy(maxFilesPerDocumentType = 0),
        )
        assertThat(unsupported.isSupportedForCollection()).isFalse()
        val negative = requirement.copy(
            document = requireNotNull(requirement.document).copy(maxFilesPerDocumentType = -1),
        )
        assertThat(negative.isSupportedForCollection()).isFalse()
    }

    @Test
    fun `server file allowance is preserved`() {
        assertThat(requirement().document?.maxFilesPerDocumentType).isEqualTo(10)
    }

    @Test
    fun `zero answer limit makes collection unsupported`() {
        val requirement = requirement()
        val questionnaire = requireNotNull(requirement.questionnaire)
        val unsupported = requirement.copy(
            questionnaire = questionnaire.copy(
                questions = questionnaire.questions.map { it.copy(maxQuestionnaireAnswerCharacters = 0) }
            )
        )
        assertThat(unsupported.isSupportedForCollection()).isFalse()
    }

    @Test
    fun `negative answer limit makes collection unsupported`() {
        val requirement = requirement()
        val questionnaire = requireNotNull(requirement.questionnaire)
        val unsupported = requirement.copy(
            questionnaire = questionnaire.copy(
                questions = questionnaire.questions.map { it.copy(maxQuestionnaireAnswerCharacters = -1) }
            )
        )
        assertThat(unsupported.isSupportedForCollection()).isFalse()
    }

    private fun requirement() = Json.decodeFromString<RetrieveKycRequirementsResponse>(
        requireNotNull(
            javaClass.classLoader?.getResourceAsStream(
                "kyc_requirements/source_of_funds_required.json"
            )
        ).bufferedReader().use { it.readText() }
    ).requirements.toKycRequirements().userActionRequired.single()
}
