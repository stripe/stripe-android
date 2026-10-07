package com.stripe.android.crypto.onramp.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KycRequirementsResponseTest {
    @Test
    fun `requirements are classified by action owner`() = runScenario(
        entries = listOf(
            requirement(description = "proof_of_address", awaitingActionFrom = "user"),
            requirement(description = "partner_review", awaitingActionFrom = "partner"),
            requirement(description = "source_of_funds", awaitingActionFrom = "user"),
            requirement(description = "stripe_review", awaitingActionFrom = "stripe"),
        )
    ) {
        assertThat(requirements.userActionRequired.map { it.description })
            .containsExactly("proof_of_address", "source_of_funds")
            .inOrder()
        assertThat(requirements.pendingPartnerAction.map { it.description })
            .containsExactly("partner_review")
        assertThat(requirements.pendingStripeAction.map { it.description })
            .containsExactly("stripe_review")
        assertThat(requirements.unrecognizedActionOwner).isEmpty()
    }

    @Test
    fun `unrecognized action owner is preserved separately`() = runScenario(
        entries = listOf(
            requirement(description = "future_requirement", awaitingActionFrom = "future_owner"),
        )
    ) {
        assertThat(requirements.userActionRequired).isEmpty()
        assertThat(requirements.pendingPartnerAction).isEmpty()
        assertThat(requirements.pendingStripeAction).isEmpty()
        assertThat(requirements.unrecognizedActionOwner.single().description).isEqualTo("future_requirement")
        assertThat(requirements.unrecognizedActionOwner.single().awaitingActionFrom).isEqualTo("future_owner")
    }

    @Test
    fun `empty requirements return empty classifications`() = runScenario {
        assertThat(requirements.userActionRequired).isEmpty()
        assertThat(requirements.pendingPartnerAction).isEmpty()
        assertThat(requirements.pendingStripeAction).isEmpty()
        assertThat(requirements.unrecognizedActionOwner).isEmpty()
    }

    @Test
    fun `requirement questionnaire is normalized without a document`() = runScenario(
        entries = listOf(
            requirement(
                description = "source_of_funds",
                awaitingActionFrom = "user",
                questionnaire = questionnaire(questionId = "document_question"),
            )
        )
    ) {
        val requirement = requirements.userActionRequired.single()

        assertThat(requirement.questionnaire?.questions?.single()?.id)
            .isEqualTo("document_question")
    }

    @Test
    fun `requirement error message is modeled as developer-facing`() = runScenario(
        entries = listOf(
            requirement(
                description = "proof_of_address",
                awaitingActionFrom = "user",
                errors = listOf(
                    KycRequirementErrorResponse(
                        code = "document_unreadable",
                        description = "Raw verification detail",
                    )
                ),
            )
        )
    ) {
        val error = requirements.userActionRequired.single().errors.single()

        assertThat(error.code).isEqualTo("document_unreadable")
        assertThat(error.developerMessage).isEqualTo("Raw verification detail")
    }

    private fun runScenario(
        entries: List<Pair<String, KycRequirementResponse>> = emptyList(),
        block: Scenario.() -> Unit,
    ) {
        Scenario(
            requirements = KycRequirementsResponse(entries.toMap()).toKycRequirements(),
        ).block()
    }

    private data class Scenario(
        val requirements: KycRequirements,
    )

    private companion object {
        fun requirement(
            description: String,
            awaitingActionFrom: String,
            questionnaire: KycQuestionnaireResponse? = null,
            errors: List<KycRequirementErrorResponse> = emptyList(),
        ): Pair<String, KycRequirementResponse> {
            return description to KycRequirementResponse(
                requestedBy = "swapped",
                awaitingActionFrom = awaitingActionFrom,
                errors = errors,
                document = null,
                collectionRequirements = KycCollectionRequirementsResponse(questionnaire),
            )
        }

        fun questionnaire(questionId: String): KycQuestionnaireResponse {
            return KycQuestionnaireResponse(
                questions = listOf(
                    KycQuestionResponse(
                        id = questionId,
                        prompt = "Question prompt",
                        answerType = "free_text",
                        required = true,
                    )
                )
            )
        }
    }
}
