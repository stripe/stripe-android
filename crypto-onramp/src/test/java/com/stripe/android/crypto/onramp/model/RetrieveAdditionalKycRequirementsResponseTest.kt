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
        val requirement = parseFixture("proof_of_address_required.json")
            .requirements.entries.getValue("proof_of_address")
        val document = requireNotNull(requirement.document)

        assertThat(requirement.requestedBy).isEqualTo("swapped")
        assertThat(requirement.awaitingActionFrom).isEqualTo("user")
        assertThat(requirement.errors).isEmpty()
        assertThat(document.acceptedSubtypes.map { it.id })
            .containsExactly("utility_bill", "bank_statement")
            .inOrder()
        assertThat(document.acceptedFormats).containsExactly("pdf", "jpeg", "png").inOrder()
        assertThat(document.minDocumentTypes).isEqualTo(1)
        assertThat(document.maxDocumentTypes).isEqualTo(2)
        assertThat(document.maxFileSizeBytes).isEqualTo(5_000_000L)
        assertThat(document.fileRequirements).isEqualTo("PDF, JPEG, or PNG, up to 5 MB per file.")
        assertThat(document.acceptedSubtypes.first().description).isEqualTo("Recent utility bill")
        assertThat(document.instructions).hasSize(2)
        assertThat(requirement.additionalRequirements).isNull()
    }

    @Test
    fun `source of funds questionnaire is parsed beside document requirements`() {
        val requirement = parseFixture("source_of_funds_required.json")
            .requirements.entries.getValue("source_of_funds")
        val questionnaire = requirement.additionalRequirements
            ?.questionnaire
        val questions = requireNotNull(questionnaire).questions

        assertThat(requirement.errors).isEmpty()
        assertThat(questions.map { it.id })
            .containsExactly("purchase_purpose", "third_party_advised", "funding_sources")
            .inOrder()
        assertThat(questions.map { it.answerType }).containsExactly("free_text", "free_text", "free_text")
        assertThat(questions.all { it.required }).isTrue()
    }

    @Test
    fun `requirement awaiting partner action omits document configuration`() {
        val requirement = parseFixture("pending_review.json").requirements.entries.getValue("proof_of_address")

        assertThat(requirement.awaitingActionFrom).isEqualTo("partner")
        assertThat(requirement.document).isNull()
        assertThat(requirement.additionalRequirements).isNull()
    }

    @Test
    fun `requirement awaiting Stripe action omits document configuration`() {
        val requirement = decode(
            """
                {
                  "requirements": {
                    "source_of_funds": {
                      "requested_by": "swapped",
                      "awaiting_action_from": "stripe",
                      "errors": []
                    }
                  }
                }
            """.trimIndent()
        ).requirements.entries.values.single()

        assertThat(requirement.awaitingActionFrom).isEqualTo("stripe")
        assertThat(requirement.document).isNull()
    }

    @Test
    fun `empty requirement entries are supported`() {
        val response = decode("""{"requirements":{}}""")

        assertThat(response.requirements.entries).isEmpty()
    }

    @Test
    fun `empty document array is treated as absent`() {
        val requirement = decode(userRequirementJson(document = "[]"))
            .requirements.entries.values.single()

        assertThat(requirement.document).isNull()
    }

    @Test
    fun `empty additional requirements array is treated as absent`() {
        val requirement = decode(
            userRequirementJson(
                document = "null",
                additionalRequirements = "[]"
            )
        ).requirements.entries.values.single()

        assertThat(requirement.additionalRequirements).isNull()
    }

    @Test
    fun `empty questionnaire array is treated as absent`() {
        val requirement = decode(
            userRequirementJson(
                document = "null",
                additionalRequirements = """{"questionnaire": []}"""
            )
        ).requirements.entries.values.single()

        assertThat(requirement.additionalRequirements?.questionnaire).isNull()
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

    @Test
    fun `unknown action owner is preserved when decoding`() {
        val response = decode(userRequirementJson(document = "null").replace("user", "future_owner"))
        val requirement = response.requirements.toAdditionalKycRequirements().unrecognizedActionOwner.single()

        assertThat(requirement.description).isEqualTo("proof_of_address")
        assertThat(requirement.awaitingActionFrom).isEqualTo("future_owner")
    }

    @Test
    fun `unknown answer type is preserved without a document`() {
        val response = decode(
            userRequirementJson(
                document = "null",
                additionalRequirements = """
                    {"questionnaire":{"questions":[{
                      "id":"purpose", "prompt":"Why?", "answer_type":"future_type", "required":true
                    }]}}
                """.trimIndent(),
            )
        )
        val requirement = response.requirements.toAdditionalKycRequirements().userActionRequired.single()

        assertThat(requirement.document).isNull()
        assertThat(requirement.questionnaire?.questions?.single()?.answerType).isEqualTo("future_type")
    }

    @Test
    fun `document collection settings are preserved in domain model`() {
        val response = parseFixture("proof_of_address_required.json")
        val requirement = response.requirements.toAdditionalKycRequirements().userActionRequired.single()
        val document = requireNotNull(requirement.document)

        assertThat(requirement.description).isEqualTo("proof_of_address")
        assertThat(document.minDocumentTypes).isEqualTo(1)
        assertThat(document.maxDocumentTypes).isEqualTo(2)
        assertThat(document.maxFileSizeBytes).isEqualTo(5_000_000L)
        assertThat(document.fileRequirements).isEqualTo("PDF, JPEG, or PNG, up to 5 MB per file.")
        assertThat(document.acceptedSubtypes.first().description).isEqualTo("Recent utility bill")
    }

    @Test
    fun `error description is decoded as developer-facing detail`() {
        val response = decode(
            userRequirementJson(document = "null").replace(
                "\"errors\": []",
                """"errors": [{"code":"document_rejected","description":"Verification failed"}]""",
            )
        )
        val error = response.requirements.toAdditionalKycRequirements().userActionRequired.single().errors.single()

        assertThat(error.code).isEqualTo("document_rejected")
        assertThat(error.developerMessage).isEqualTo("Verification failed")
    }

    @Test
    fun `missing subtype description is preserved as null`() {
        val response = parseFixture("source_of_funds_required.json")
        val requirement = response.requirements.toAdditionalKycRequirements().userActionRequired.single()
        val subtypes = requireNotNull(requirement.document).acceptedSubtypes

        assertThat(subtypes.first().description).isEqualTo("Recent payslip")
        assertThat(subtypes.last().id).isEqualTo("bank_statement")
        assertThat(subtypes.last().label).isEqualTo("Bank statement")
        assertThat(subtypes.last().description).isNull()
    }

    @Test
    fun `explicit null subtype description is preserved as null`() {
        val response = parseFixture("source_of_funds_required.json") { fixture ->
            fixture.replace("\"description\": \"Recent payslip\"", "\"description\": null")
        }
        val requirement = response.requirements.toAdditionalKycRequirements().userActionRequired.single()
        val subtype = requireNotNull(requirement.document).acceptedSubtypes.first()

        assertThat(subtype.id).isEqualTo("payslip")
        assertThat(subtype.description).isNull()
    }

    private fun parseFixture(
        fileName: String,
        transform: (String) -> String = { it },
    ): RetrieveAdditionalKycRequirementsResponse {
        val fixture = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("additional_kyc_requirements/$fileName")
        ).bufferedReader().use { it.readText() }

        return decode(transform(fixture))
    }

    private fun decode(value: String): RetrieveAdditionalKycRequirementsResponse {
        return json.decodeFromString(RetrieveAdditionalKycRequirementsResponse.serializer(), value)
    }

    private fun userRequirementJson(document: String, additionalRequirements: String = "null"): String {
        return """
            {
              "requirements": {
                "proof_of_address": {
                  "requested_by": "swapped",
                  "awaiting_action_from": "user",
                  "errors": [],
                  "document": $document,
                  "additional_requirements": $additionalRequirements
                }
              }
            }
        """.trimIndent()
    }
}
