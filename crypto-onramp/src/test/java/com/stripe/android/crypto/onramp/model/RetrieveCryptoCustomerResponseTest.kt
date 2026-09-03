package com.stripe.android.crypto.onramp.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import org.junit.Test

class RetrieveCryptoCustomerResponseTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `proof of address requirement is parsed`() {
        val requirement = parseFixture("proof_of_address_required.json").requirements.entries.single()
        val document = requireNotNull(requirement.document)

        assertThat(requirement.description).isEqualTo("proof_of_address")
        assertThat(requirement.requestedBy).isEqualTo("swapped")
        assertThat(requirement.awaitingActionFrom).isEqualTo("user")
        assertThat(requirement.errors).isEmpty()
        assertThat(requirement.submissionType).isEqualTo("document")
        assertThat(document.acceptedSubtypes.map { it.id })
            .containsExactly("utility_bill", "bank_statement")
            .inOrder()
        assertThat(document.acceptedFormats).containsExactly("pdf", "jpeg", "png").inOrder()
        assertThat(document.minDocuments).isEqualTo(1)
        assertThat(document.instructions).hasSize(2)
        assertThat(document.additionalRequirements).isNull()
    }

    @Test
    fun `source of funds questionnaire is parsed`() {
        val requirement = parseFixture("source_of_funds_required.json").requirements.entries.single()
        val questionnaire = requireNotNull(requirement.document)
            .additionalRequirements
            ?.questionnaire
        val questions = requireNotNull(questionnaire).questions

        assertThat(requirement.description).isEqualTo("source_of_funds")
        assertThat(requirement.errors).isEmpty()
        assertThat(questions.map { it.id })
            .containsExactly("purchase_purpose", "third_party_advised", "funding_sources")
            .inOrder()
        assertThat(questions.map { it.answerType }).containsExactly("free_text", "free_text", "free_text")
        assertThat(questions.all { it.required }).isTrue()
    }

    @Test
    fun `top-level questionnaire is parsed`() {
        val requirement = parseFixture("questionnaire_required.json").requirements.entries.single()
        val questions = requireNotNull(requirement.questionnaire).questions

        assertThat(requirement.submissionType).isEqualTo("questionnaire")
        assertThat(requirement.document).isNull()
        assertThat(questions.single().id).isEqualTo("purchase_purpose")
        assertThat(questions.single().answerType).isEqualTo("free_text")
    }

    @Test
    fun `requirement awaiting partner action is parsed`() {
        val requirement = parseFixture("pending_review.json").requirements.entries.single()

        assertThat(requirement.description).isEqualTo("proof_of_address")
        assertThat(requirement.awaitingActionFrom).isEqualTo("partner")
        assertThat(requirement.submissionType).isEqualTo("document")
    }

    @Test
    fun `unknown submission type and fields are preserved or ignored`() {
        val requirement = parseFixture("unknown_submission_type.json").requirements.entries.single()

        assertThat(requirement.description).isEqualTo("ownership_attestation")
        assertThat(requirement.requestedBy).isEqualTo("future_partner")
        assertThat(requirement.submissionType).isEqualTo("attestation")
        assertThat(requirement.document).isNull()
        assertThat(requirement.questionnaire).isNull()
    }

    @Test
    fun `empty requirement entries are supported`() {
        val response = json.decodeFromString(
            RetrieveCryptoCustomerResponse.serializer(),
            """{"requirements":{"entries":[]}}""",
        )

        assertThat(response.requirements.entries).isEmpty()
    }

    @Test
    fun `missing requirements fail decoding`() {
        val result = runCatching {
            json.decodeFromString(
                RetrieveCryptoCustomerResponse.serializer(),
                "{}",
            )
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(MissingFieldException::class.java)
    }

    @Test
    fun `missing required document collection fields fail decoding`() {
        val result = runCatching {
            json.decodeFromString(
                RetrieveCryptoCustomerResponse.serializer(),
                """
                    {
                      "requirements": {
                        "entries": [{
                          "description": "proof_of_address",
                          "requested_by": "swapped",
                          "awaiting_action_from": "user",
                          "errors": [],
                          "submission_type": "document",
                          "document": {}
                        }]
                      }
                    }
                """.trimIndent(),
            )
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(MissingFieldException::class.java)
    }

    private fun parseFixture(fileName: String): RetrieveCryptoCustomerResponse {
        val fixture = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("crypto_customer/$fileName")
        ).bufferedReader().use { it.readText() }

        return json.decodeFromString(RetrieveCryptoCustomerResponse.serializer(), fixture)
    }
}
