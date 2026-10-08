package com.stripe.android.crypto.onramp.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.RetrieveAdditionalKycRequirementsResponse
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AdditionalKycCollectionValidationTest {
    @Test
    fun `answers stop at five thousand characters`() {
        assertThat(limitAdditionalKycAnswer("a".repeat(5_001))).isEqualTo("a".repeat(5_000))
    }

    @Test
    fun `answer limit preserves composed characters and emoji`() {
        val character = "👨‍👩‍👧‍👦"
        assertThat(limitAdditionalKycAnswer(character.repeat(5_001))).isEqualTo(character.repeat(5_000))
        assertThat(limitAdditionalKycAnswer("e\u0301".repeat(5_001))).isEqualTo("e\u0301".repeat(5_000))
    }

    @Test
    fun `short answers remain unchanged`() {
        assertThat(limitAdditionalKycAnswer("Savings")).isEqualTo("Savings")
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

    private fun requirement() = Json.decodeFromString<RetrieveAdditionalKycRequirementsResponse>(
        requireNotNull(
            javaClass.classLoader?.getResourceAsStream(
                "additional_kyc_requirements/source_of_funds_required.json"
            )
        ).bufferedReader().use { it.readText() }
    ).requirements.toAdditionalKycRequirements().userActionRequired.single()
}
