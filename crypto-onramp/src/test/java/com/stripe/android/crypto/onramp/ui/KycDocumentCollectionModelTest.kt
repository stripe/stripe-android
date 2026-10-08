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

    @Test
    fun `adding another source excludes the existing source type`() = runScenario(
        maxFilesPerDocumentType = 2,
    ) {
        onAddDocuments()
        onFileSelected(0, File("salary.pdf"), "salary.pdf", "file_salary")
        finishEditing()

        assertThat(onAddDocuments()).isTrue()

        val slot = requireNotNull(editingDocumentSlot)
        val state = requireNotNull(buildState()).slots.first { it.index == slot }
        assertThat(state.subtypes.map { it.id }).containsExactly("bank_statement")
        assertThat(onDocumentSubtypeSelected(slot, "payslip")).isFalse()
        assertThat(requireNotNull(buildState()).slots.first { it.index == slot }.selectedSubtypeId)
            .isEqualTo("bank_statement")
    }

    @Test
    fun `editing retains its own type at the file limit and excludes other source types`() = runScenario {
        onAddDocuments()
        onFileSelected(0, File("salary.pdf"), "salary.pdf", "file_salary")
        finishEditing()
        onAddDocuments()
        val bankSlot = requireNotNull(editingDocumentSlot)
        onFileSelected(bankSlot, File("bank.pdf"), "bank.pdf", "file_bank")
        finishEditing()

        assertThat(onEditDocuments(0)).isTrue()

        val slot = requireNotNull(editingDocumentSlot)
        val state = requireNotNull(buildState()).slots.first { it.index == slot }
        assertThat(state.subtypes.map { it.id }).containsExactly("payslip")
        assertThat(state.subtypes.single().isEnabled).isTrue()
        assertThat(canSelectFile(slot)).isFalse()
        assertThat(onDocumentSubtypeSelected(slot, "bank_statement")).isFalse()
        assertThat(createSubmission().map { it.documentSubtype }).containsExactly("payslip", "bank_statement")
    }

    @Test
    fun `changing an existing source type keeps all its uploaded files together`() = runScenario(
        maxFilesPerDocumentType = 2,
    ) {
        onAddDocuments()
        onFileSelected(0, File("salary.pdf"), "salary.pdf", "file_salary")
        onFileSelected(requireNotNull(editingDocumentSlot), File("bonus.pdf"), "bonus.pdf", "file_bonus")
        finishEditing()
        onEditDocuments(0)

        assertThat(onDocumentSubtypeSelected(requireNotNull(editingDocumentSlot), "bank_statement")).isTrue()

        val documents = createSubmission()
        assertThat(documents).hasSize(1)
        assertThat(documents.single().documentSubtype).isEqualTo("bank_statement")
        assertThat(documents.single().uploadedFileIds).containsExactly("file_salary", "file_bonus").inOrder()
        assertThat(requireNotNull(buildState()).slots.map { it.selectedSubtypeId })
            .containsExactly("bank_statement", "bank_statement", "bank_statement")
    }

    @Test
    fun `removing a source makes its type available again`() = runScenario {
        onAddDocuments()
        onFileSelected(0, File("salary.pdf"), "salary.pdf", "file_salary")
        finishEditing()
        onAddDocuments()
        val bankSlot = requireNotNull(editingDocumentSlot)
        onFileSelected(bankSlot, File("bank.pdf"), "bank.pdf", "file_bank")
        finishEditing()
        onEditDocuments(bankSlot)
        assertThat(requireNotNull(buildState()).slots.first { it.index == editingDocumentSlot }.subtypes.map { it.id })
            .containsExactly("bank_statement")

        onFileRemoved(0, isEditing = true)

        assertThat(requireNotNull(buildState()).slots.first { it.index == editingDocumentSlot }.subtypes.map { it.id })
            .containsExactly("payslip", "bank_statement").inOrder()
    }

    private fun runScenario(
        maxFilesPerDocumentType: Int = 1,
        block: KycDocumentCollectionModel.() -> Unit,
    ) {
        model(maxFilesPerDocumentType).block()
    }

    private fun model(maxFilesPerDocumentType: Int = 1): KycDocumentCollectionModel {
        val fixture = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("kyc_requirements/source_of_funds_required.json")
        ).bufferedReader().use { it.readText() }
        val requirement = Json.decodeFromString<RetrieveKycRequirementsResponse>(fixture)
            .requirements.toKycRequirements().userActionRequired.single()
        return KycDocumentCollectionModel(
            requireNotNull(requirement.document).copy(maxFilesPerDocumentType = maxFilesPerDocumentType),
            KycRequirementType.SourceOfFunds,
        )
    }
}
