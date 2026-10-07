package com.stripe.android.crypto.onramp.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.RetrieveKycRequirementsResponse
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

internal class KycDocumentCollectionModelTest {
    @Test
    fun `removing a file restores its source capacity`() {
        val model = model()
        assertThat(model.onAddDocuments()).isTrue()
        val slot = requireNotNull(model.editingDocumentSlot)
        model.onFileSelected(slot, File("salary.pdf"), "salary.pdf", "file_salary")
        val nextSlot = requireNotNull(model.editingDocumentSlot)
        assertThat(model.canSelectFile(nextSlot)).isFalse()
        assertThat(model.createSubmission().single().uploadedFileIds).containsExactly("file_salary")
        assertThat(model.onFileRemoved(slot, true)).isEqualTo(File("salary.pdf"))
        assertThat(model.canSelectFile(requireNotNull(model.editingDocumentSlot))).isTrue()
    }

    @Test
    fun `upload failure clears progress and permits retry`() {
        val model = model()
        model.onAddDocuments()
        val slot = requireNotNull(model.editingDocumentSlot)
        model.onFileSelectionStarted(slot)
        model.onFileUploadStarted(slot, "salary.pdf")
        assertThat(model.selectingFileName).isEqualTo("salary.pdf")
        model.onFileSelectionFailed()
        assertThat(model.selectingFileSlot).isNull()
        assertThat(model.selectingFileName).isNull()
        assertThat(model.validationError).isEqualTo(KycValidationError.FileUnavailable)
        assertThat(model.onFileSelectionStarted(slot)).isTrue()
        assertThat(model.validationError).isNull()
    }

    private fun model(): KycDocumentCollectionModel {
        val fixture = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("kyc_requirements/source_of_funds_required.json")
        ).bufferedReader().use { it.readText() }
        val requirement = Json.decodeFromString<RetrieveKycRequirementsResponse>(fixture)
            .requirements.toKycRequirements().userActionRequired.single()
        return KycDocumentCollectionModel(
            requireNotNull(requirement.document).copy(maxFilesPerDocumentType = 1),
            KycRequirementType.SourceOfFunds,
        )
    }
}
