package com.stripe.android.crypto.onramp.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdditionalKycRequirementsResponseTest {
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
    fun `top-level questionnaire is normalized`() = runScenario(
        entries = listOf(
            requirement(
                description = "screening_questions",
                awaitingActionFrom = "user",
                submissionType = "questionnaire",
                questionnaire = questionnaire(questionId = "top_level_question"),
            )
        )
    ) {
        val requirement = requirements.userActionRequired.single()

        assertThat(requirement.questionnaire?.questions?.single()?.id)
            .isEqualTo("top_level_question")
    }

    @Test
    fun `document questionnaire is normalized`() = runScenario(
        entries = listOf(
            requirement(
                description = "source_of_funds",
                awaitingActionFrom = "user",
                submissionType = "document",
                document = document(
                    questionnaire = questionnaire(questionId = "document_question"),
                ),
            )
        )
    ) {
        val requirement = requirements.userActionRequired.single()

        assertThat(requirement.questionnaire?.questions?.single()?.id)
            .isEqualTo("document_question")
    }

    @Test
    fun `top-level questionnaire takes precedence when both locations exist`() = runScenario(
        entries = listOf(
            requirement(
                description = "source_of_funds",
                awaitingActionFrom = "user",
                submissionType = "document",
                document = document(
                    questionnaire = questionnaire(questionId = "document_question"),
                ),
                questionnaire = questionnaire(questionId = "top_level_question"),
            )
        )
    ) {
        val requirement = requirements.userActionRequired.single()

        assertThat(requirement.questionnaire?.questions?.single()?.id)
            .isEqualTo("top_level_question")
    }

    @Test
    fun `top-level questionnaire takes precedence independent of submission type`() = runScenario(
        entries = listOf(
            requirement(
                description = "screening_questions",
                awaitingActionFrom = "user",
                submissionType = "questionnaire",
                document = document(
                    questionnaire = questionnaire(questionId = "document_question"),
                ),
                questionnaire = questionnaire(questionId = "top_level_question"),
            )
        )
    ) {
        val requirement = requirements.userActionRequired.single()

        assertThat(requirement.questionnaire?.questions?.single()?.id)
            .isEqualTo("top_level_question")
    }

    private fun runScenario(
        entries: List<AdditionalKycRequirementResponse> = emptyList(),
        block: Scenario.() -> Unit,
    ) {
        Scenario(
            requirements = AdditionalKycRequirementsResponse(entries).toAdditionalKycRequirements(),
        ).block()
    }

    private data class Scenario(
        val requirements: AdditionalKycRequirements,
    )

    private companion object {
        fun requirement(
            description: String,
            awaitingActionFrom: String,
            submissionType: String = "document",
            document: AdditionalKycDocumentRequirementResponse? = null,
            questionnaire: AdditionalKycQuestionnaireResponse? = null,
        ): AdditionalKycRequirementResponse {
            return AdditionalKycRequirementResponse(
                description = description,
                requestedBy = "swapped",
                awaitingActionFrom = awaitingActionFrom,
                errors = emptyList(),
                submissionType = submissionType,
                document = document,
                questionnaire = questionnaire,
            )
        }

        fun document(
            questionnaire: AdditionalKycQuestionnaireResponse?,
        ): AdditionalKycDocumentRequirementResponse {
            return AdditionalKycDocumentRequirementResponse(
                acceptedSubtypes = emptyList(),
                acceptedFormats = emptyList(),
                minDocuments = 1,
                instructions = emptyList(),
                additionalRequirements = AdditionalKycCollectionRequirementsResponse(
                    questionnaire = questionnaire,
                ),
            )
        }

        fun questionnaire(questionId: String): AdditionalKycQuestionnaireResponse {
            return AdditionalKycQuestionnaireResponse(
                questions = listOf(
                    AdditionalKycQuestionResponse(
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
