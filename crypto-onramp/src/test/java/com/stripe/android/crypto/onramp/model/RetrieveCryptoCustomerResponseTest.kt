package com.stripe.android.crypto.onramp.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class RetrieveCryptoCustomerResponseTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `proof of address requirement is parsed`() {
        val response = parseFixture("proof_of_address_required.json")
        val requirement = requireNotNull(response.requirements).entries.single()
        val document = requireNotNull(requirement.document)

        assertThat(response.id).isEqualTo("crc_poa")
        assertThat(requirement.description).isEqualTo("proof_of_address")
        assertThat(requirement.requestedBy).isEqualTo("swapped")
        assertThat(requirement.awaitingActionFrom).isEqualTo("user")
        assertThat(requirement.requestedReasons).containsExactly("kyc_step_up")
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
        val response = parseFixture("source_of_funds_required.json")
        val requirement = requireNotNull(response.requirements).entries.single()
        val questionnaire = requireNotNull(requirement.document)
            .additionalRequirements
            ?.questionnaire
        val questions = requireNotNull(questionnaire).questions

        assertThat(requirement.description).isEqualTo("source_of_funds")
        assertThat(questions.map { it.id })
            .containsExactly("purchase_purpose", "third_party_advised", "funding_sources")
            .inOrder()
        assertThat(questions.map { it.answerType }).containsExactly("free_text", "free_text", "free_text")
        assertThat(questions.all { it.required }).isTrue()

        val capabilityImpact = requireNotNull(requirement.impact).restrictsCapabilities.single()
        assertThat(capabilityImpact.capability).isEqualTo("crypto_onramp_transactions")
        assertThat(capabilityImpact.restriction.maxTransactionAmount?.amount).isEqualTo(2500000L)
        assertThat(capabilityImpact.restriction.maxTransactionAmount?.currency).isEqualTo("eur")
        assertThat(capabilityImpact.restriction.lifetimeVolumeLimit?.amount).isNull()
        assertThat(capabilityImpact.restriction.lifetimeVolumeLimit?.currency).isEqualTo("eur")
        assertThat(capabilityImpact.restriction.regions).containsExactly("CO", "PH", "CA").inOrder()
    }

    @Test
    fun `top-level questionnaire is parsed`() {
        val response = parseFixture("questionnaire_required.json")
        val requirement = requireNotNull(response.requirements).entries.single()
        val questions = requireNotNull(requirement.questionnaire).questions

        assertThat(requirement.submissionType).isEqualTo("questionnaire")
        assertThat(requirement.document).isNull()
        assertThat(questions.single().id).isEqualTo("purchase_purpose")
        assertThat(questions.single().answerType).isEqualTo("free_text")
    }

    @Test
    fun `requirement awaiting partner action is parsed`() {
        val response = parseFixture("pending_review.json")
        val requirement = requireNotNull(response.requirements).entries.single()

        assertThat(requirement.description).isEqualTo("proof_of_address")
        assertThat(requirement.awaitingActionFrom).isEqualTo("partner")
        assertThat(requirement.submissionType).isEqualTo("document")
    }

    @Test
    fun `unknown submission type and fields are preserved or ignored`() {
        val response = parseFixture("unknown_submission_type.json")
        val requirement = requireNotNull(response.requirements).entries.single()

        assertThat(requirement.description).isEqualTo("ownership_attestation")
        assertThat(requirement.requestedBy).isEqualTo("future_partner")
        assertThat(requirement.submissionType).isEqualTo("attestation")
        assertThat(requirement.document).isNull()
        assertThat(requirement.questionnaire).isNull()
    }

    @Test
    fun `missing requirements are supported`() {
        val response = json.decodeFromString(
            RetrieveCryptoCustomerResponse.serializer(),
            """{"id":"crc_without_requirements"}""",
        )

        assertThat(response.id).isEqualTo("crc_without_requirements")
        assertThat(response.requirements).isNull()
    }

    @Test
    fun `missing document collection configuration uses defaults`() {
        val response = json.decodeFromString(
            RetrieveCryptoCustomerResponse.serializer(),
            """
                {
                  "id": "crc_defaults",
                  "requirements": {
                    "entries": [{
                      "description": "proof_of_address",
                      "requested_by": "swapped",
                      "awaiting_action_from": "user",
                      "submission_type": "document",
                      "document": {}
                    }]
                  }
                }
            """.trimIndent(),
        )
        val document = requireNotNull(requireNotNull(response.requirements).entries.single().document)

        assertThat(document.acceptedSubtypes).isEmpty()
        assertThat(document.acceptedFormats).isEmpty()
        assertThat(document.minDocuments).isEqualTo(1)
        assertThat(document.instructions).isEmpty()
    }

    private fun parseFixture(fileName: String): RetrieveCryptoCustomerResponse {
        val fixture = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("crypto_customer/$fileName")
        ).bufferedReader().use { it.readText() }

        return json.decodeFromString(RetrieveCryptoCustomerResponse.serializer(), fixture)
    }
}
