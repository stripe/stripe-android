package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class PersonTokenParamsTest {

    @Test
    fun `Verification toParamMap maps document and additionalDocument to their own API keys`() {
        val document = PersonTokenParams.Document(front = "id_front", back = "id_back")
        val additionalDocument = PersonTokenParams.Document(front = "proof_front", back = "proof_back")

        val verification = PersonTokenParams.Verification(
            document = document,
            additionalDocument = additionalDocument
        )

        assertThat(verification.toParamMap()).isEqualTo(
            mapOf(
                "document" to document.toParamMap(),
                "additional_document" to additionalDocument.toParamMap()
            )
        )
    }

    @Test
    fun `Verification toParamMap with only document set emits the document key`() {
        val document = PersonTokenParams.Document(front = "id_front", back = "id_back")

        val verification = PersonTokenParams.Verification(document = document)

        assertThat(verification.toParamMap()).isEqualTo(
            mapOf("document" to document.toParamMap())
        )
    }

    @Test
    fun `Verification toParamMap with only additionalDocument set emits the additional_document key`() {
        val additionalDocument = PersonTokenParams.Document(front = "proof_front", back = "proof_back")

        val verification = PersonTokenParams.Verification(additionalDocument = additionalDocument)

        assertThat(verification.toParamMap()).isEqualTo(
            mapOf("additional_document" to additionalDocument.toParamMap())
        )
    }
}
