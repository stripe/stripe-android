package com.stripe.android.link.onramp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.ui.PrimaryButtonTag
import com.stripe.android.link.ui.ProgressIndicatorTestTag
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AdditionalKycScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `proof of address context matches first collection screen`() = runScenario(
        state = screenState(
            page = AdditionalKycCollectionPage.Context,
            requirementType = AdditionalKycRequirementType.ProofOfAddress,
        ),
    ) {
        composeRule.onNodeWithText("Upload your proof of address").assertIsDisplayed()
        composeRule.onNodeWithText(
            "We’re required to confirm your address to enable spending over €1,000."
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(ADDITIONAL_KYC_CANCEL_BUTTON_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(ADDITIONAL_KYC_BACK_BUTTON_TAG).assertDoesNotExist()
        composeRule.onNodeWithText("Continue").performClick()

        assertThat(continued).isTrue()
    }

    @Test
    fun `questionnaire excludes funding sources and forwards answers`() = runScenario(
        state = screenState(
            page = AdditionalKycCollectionPage.Questionnaire,
            questions = listOf(
                question("purchase_purpose", "Why are you purchasing cryptocurrency through swapped.com?"),
                question("third_party_advised", "Has anyone advised you to purchase cryptocurrency?"),
                question("funding_sources", "How are you funding your transactions?"),
            ),
            canContinue = false,
        ),
    ) {
        composeRule.onNodeWithTag(additionalKycQuestionTag("purchase_purpose"))
            .performTextReplacement("For investment")
        composeRule.onNodeWithTag(additionalKycQuestionTag("funding_sources")).assertDoesNotExist()
        composeRule.onNodeWithTag(PrimaryButtonTag).assertIsNotEnabled()

        assertThat(changedAnswer).isEqualTo("purchase_purpose" to "For investment")
    }

    @Test
    fun `proof of address editor selects type and chooses a file`() = runScenario(
        state = screenState(
            page = AdditionalKycCollectionPage.DocumentEditor,
            requirementType = AdditionalKycRequirementType.ProofOfAddress,
        ),
    ) {
        composeRule.onNodeWithTag(additionalKycSubtypePickerTag(0)).performClick()
        composeRule.onNodeWithText("Document type").assertIsDisplayed()
        composeRule.onNodeWithText("Electricity, water, gas, internet, phone bill").assertIsDisplayed()
        composeRule.onNodeWithTag(additionalKycSubtypeOptionTag(0, "utility_bill")).performClick()
        composeRule.onNodeWithTag(additionalKycChooseFileTag(0))
            .performScrollTo()
            .performClick()

        assertThat(selectedSubtype).isEqualTo(0 to "utility_bill")
        assertThat(chosenFileSlot).isEqualTo(0)
    }

    @Test
    fun `uploaded proof of address hides upload action and enables submit`() = runScenario(
        state = screenState(
            page = AdditionalKycCollectionPage.DocumentEditor,
            requirementType = AdditionalKycRequirementType.ProofOfAddress,
            document = documentState(
                slots = listOf(documentSlot(index = 0, fileName = "electricity-bill.pdf")),
                editingSlotIndex = 0,
            ),
            canSubmit = true,
            completedDocumentCount = 1,
        ),
    ) {
        composeRule.onNodeWithTag(additionalKycFileNameTag(0)).assertIsDisplayed()
        composeRule.onNodeWithTag(additionalKycChooseFileTag(0)).assertDoesNotExist()
        composeRule.onNodeWithText("Submit").performClick()

        assertThat(submitted).isTrue()
    }

    @Test
    fun `source overview groups documents and forwards add and edit`() = runScenario(
        state = screenState(
            page = AdditionalKycCollectionPage.DocumentOverview,
            document = documentState(
                slots = listOf(
                    documentSlot(index = 0, fileName = "payslip-feb.pdf"),
                    documentSlot(index = 1, fileName = "payslip-mar.pdf"),
                ),
                editingSlotIndex = null,
            ),
            canSubmit = true,
            completedDocumentCount = 2,
        ),
    ) {
        composeRule.onNodeWithText("payslip-feb.pdf", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(additionalKycDocumentGroupTag(0)).performClick()
        composeRule.onNodeWithTag(ADDITIONAL_KYC_ADD_DOCUMENTS_TAG).performClick()

        assertThat(editedDocumentSlot).isEqualTo(0)
        assertThat(addDocuments).isTrue()
    }

    @Test
    fun `unsupported proof document renders inline error card`() = runScenario(
        state = screenState(
            page = AdditionalKycCollectionPage.DocumentEditor,
            requirementType = AdditionalKycRequirementType.ProofOfAddress,
            validationError = AdditionalKycValidationError.UnsupportedFileType,
            validationFileName = "electricity-bill.docx",
            canSubmit = false,
        ),
    ) {
        composeRule.onNodeWithTag(ADDITIONAL_KYC_VALIDATION_ERROR_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("electricity-bill.docx").assertIsDisplayed()
        composeRule.onNodeWithText(
            "This file type isn’t supported. Upload a PDF, JPEG, or PNG file."
        ).assertIsDisplayed()
    }

    @Test
    fun `submitted screen renders review copy and done action`() = runScenario(
        state = screenState(
            page = AdditionalKycCollectionPage.Submitted,
            submissionState = AdditionalKycSubmissionState.Submitted,
        ),
    ) {
        composeRule.onNodeWithTag(ADDITIONAL_KYC_SUBMITTED_TITLE_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(
            "We’re reviewing your documents. We’ll let you know when verification is complete."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()

        assertThat(continued).isTrue()
    }

    @Test
    fun `submitting blocks editing and shows progress`() = runScenario(
        state = screenState(
            page = AdditionalKycCollectionPage.DocumentEditor,
            submissionState = AdditionalKycSubmissionState.Submitting,
            canSubmit = false,
        ),
    ) {
        composeRule.onNodeWithTag(PrimaryButtonTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(
            testTag = ProgressIndicatorTestTag,
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    private fun runScenario(
        state: AdditionalKycScreenState,
        block: TestScenario.() -> Unit,
    ) {
        val scenario = TestScenario()
        composeRule.setContent {
            AdditionalKycScreen(
                appearance = null,
                state = state,
                onClose = { scenario.closed = true },
                onBack = { scenario.wentBack = true },
                onQuestionAnswerChanged = { questionId, answer ->
                    scenario.changedAnswer = questionId to answer
                },
                onDocumentSubtypeSelected = { slotIndex, subtypeId ->
                    scenario.selectedSubtype = slotIndex to subtypeId
                },
                onChooseFile = { scenario.chosenFileSlot = it },
                onRemoveFile = { scenario.removedFileSlot = it },
                onAddDocuments = { scenario.addDocuments = true },
                onEditDocuments = { scenario.editedDocumentSlot = it },
                onSubmit = { scenario.submitted = true },
                onContinue = { scenario.continued = true },
            )
        }
        scenario.block()
    }

    private data class TestScenario(
        var closed: Boolean = false,
        var wentBack: Boolean = false,
        var changedAnswer: Pair<String, String>? = null,
        var selectedSubtype: Pair<Int, String>? = null,
        var chosenFileSlot: Int? = null,
        var removedFileSlot: Int? = null,
        var addDocuments: Boolean = false,
        var editedDocumentSlot: Int? = null,
        var submitted: Boolean = false,
        var continued: Boolean = false,
    )

    private companion object {
        fun screenState(
            page: AdditionalKycCollectionPage,
            requirementType: AdditionalKycRequirementType = AdditionalKycRequirementType.SourceOfFunds,
            questions: List<AdditionalKycQuestionState> = emptyList(),
            document: AdditionalKycDocumentState? = documentState(),
            validationError: AdditionalKycValidationError? = null,
            validationFileName: String? = null,
            canSubmit: Boolean = false,
            canContinue: Boolean = true,
            submissionState: AdditionalKycSubmissionState = AdditionalKycSubmissionState.Collecting,
            completedDocumentCount: Int = 0,
        ): AdditionalKycScreenState {
            return AdditionalKycScreenState(
                page = page,
                requirementType = requirementType,
                errorMessages = emptyList(),
                questions = questions,
                document = document,
                validationError = validationError,
                validationFileName = validationFileName,
                selectingFileSlot = null,
                selectingFileName = null,
                canSubmit = canSubmit,
                canContinue = canContinue,
                isCollectionAvailable = true,
                submissionState = submissionState,
                currentRequirement = 1,
                totalRequirements = 1,
                hasMoreRequirements = false,
                pendingRequirements = emptyList(),
                completedDocumentCount = completedDocumentCount,
            )
        }

        fun question(id: String, prompt: String): AdditionalKycQuestionState {
            return AdditionalKycQuestionState(
                id = id,
                prompt = prompt,
                answer = "",
                required = true,
            )
        }

        fun documentState(
            slots: List<AdditionalKycDocumentSlotState> = listOf(documentSlot()),
            editingSlotIndex: Int? = 0,
        ): AdditionalKycDocumentState {
            return AdditionalKycDocumentState(
                acceptedFormats = listOf("pdf", "jpeg", "png"),
                instructions = listOf("Upload documents that support your transaction activity"),
                maxFileSizeMegabytes = 5,
                minDocuments = 1,
                maxDocuments = 10,
                editingSlotIndex = editingSlotIndex,
                slots = slots,
            )
        }

        fun documentSlot(
            index: Int = 0,
            fileName: String? = null,
        ): AdditionalKycDocumentSlotState {
            return AdditionalKycDocumentSlotState(
                index = index,
                subtypes = listOf(
                    AdditionalKycDocumentSubtypeState(
                        id = "salary",
                        label = "Salary",
                        isEnabled = true,
                    ),
                    AdditionalKycDocumentSubtypeState(
                        id = "utility_bill",
                        label = "Utility bill",
                        isEnabled = true,
                    ),
                ),
                selectedSubtypeId = "salary",
                selectedSubtypeLabel = "Salary",
                fileName = fileName,
            )
        }
    }
}
