package com.stripe.android.crypto.onramp.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Test

class RetrieveAdditionalKycRequirementsResponseTest {
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
        assertThat(document.acceptedSubtypes.map { it.id })
            .containsExactly("utility_bill", "bank_statement")
            .inOrder()
        assertThat(document.acceptedFormats).containsExactly("pdf", "jpeg", "png").inOrder()
        assertThat(document.minDocuments).isEqualTo(1)
        assertThat(document.instructions).hasSize(2)
        assertThat(document.additionalRequirements).isNull()
    }

    @Test
    fun `source of funds questionnaire is parsed under document requirements`() {
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
    fun `requirement awaiting partner action omits document configuration`() {
        val requirement = parseFixture("pending_review.json").requirements.entries.single()

        assertThat(requirement.description).isEqualTo("proof_of_address")
        assertThat(requirement.awaitingActionFrom).isEqualTo("partner")
        assertThat(requirement.document).isNull()
    }

    @Test
    fun `requirement awaiting Stripe action omits document configuration`() {
        val requirement = decode(
            """
                {
                  "requirements": {
                    "entries": [{
                      "description": "source_of_funds",
                      "requested_by": "swapped",
                      "awaiting_action_from": "stripe",
                      "errors": []
                    }]
                  }
                }
            """.trimIndent()
        ).requirements.entries.single()

        assertThat(requirement.awaitingActionFrom).isEqualTo("stripe")
        assertThat(requirement.document).isNull()
    }

    @Test
    fun `empty requirement entries are supported`() {
        val response = decode("""{"requirements":{"entries":[]}}""")

        assertThat(response.requirements.entries).isEmpty()
    }

    @Test
    fun `empty document array is treated as absent`() {
        val requirement = decode(userRequirementJson(document = "[]"))
            .requirements.entries.single()

        assertThat(requirement.document).isNull()
    }

    @Test
    fun `empty additional requirements array is treated as absent`() {
        val requirement = decode(
            userRequirementJson(
                document = documentJson(additionalRequirements = "[]")
            )
        ).requirements.entries.single()

        assertThat(requireNotNull(requirement.document).additionalRequirements).isNull()
    }

    @Test
    fun `empty questionnaire array is treated as absent`() {
        val requirement = decode(
            userRequirementJson(
                document = documentJson(
                    additionalRequirements = """{"questionnaire": []}"""
                )
            )
        ).requirements.entries.single()

        assertThat(requireNotNull(requirement.document).additionalRequirements?.questionnaire).isNull()
    }

    @Test
    fun `missing requirements fail decoding`() {
        val result = runCatching { decode("{}") }

        assertThat(result.exceptionOrNull()).isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `missing required document collection fields fail decoding`() {
        val result = runCatching {
            decode(userRequirementJson(document = "{}"))
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(SerializationException::class.java)
    }

    private fun parseFixture(fileName: String): RetrieveAdditionalKycRequirementsResponse {
        val fixture = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("additional_kyc_requirements/$fileName")
        ).bufferedReader().use { it.readText() }

        return decode(fixture)
    }

    private fun decode(value: String): RetrieveAdditionalKycRequirementsResponse {
        return json.decodeFromString(RetrieveAdditionalKycRequirementsResponse.serializer(), value)
    }

    private fun userRequirementJson(document: String): String {
        return """
            {
              "requirements": {
                "entries": [{
                  "description": "proof_of_address",
                  "requested_by": "swapped",
                  "awaiting_action_from": "user",
                  "errors": [],
                  "document": $document
                }]
              }
            }
        """.trimIndent()
    }

    private fun documentJson(additionalRequirements: String): String {
        return """
            {
              "accepted_subtypes": [],
              "accepted_formats": [],
              "min_documents": 1,
              "instructions": [],
              "additional_requirements": $additionalRequirements
            }
        """.trimIndent()
    }
}
