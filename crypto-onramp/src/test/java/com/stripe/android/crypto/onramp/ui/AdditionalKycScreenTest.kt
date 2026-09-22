package com.stripe.android.crypto.onramp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
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
            "We may request proof of address for larger transactions."
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
        composeRule.onNodeWithTag("PrimaryButtonTag").assertIsNotEnabled()

        assertThat(changedAnswer).isEqualTo("purchase_purpose" to "For investment")
    }

    @Test
    fun `proof of address editor selects type and chooses a file`() = runScenario(
        state = screenState(
            page = AdditionalKycCollectionPage.DocumentEditor,
            requirementType = AdditionalKycRequirementType.ProofOfAddress,
        ),
    ) {
        composeRule.onNodeWithText("PDF, JPEG, or PNG, up to 5 MB per file.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(additionalKycSubtypePickerTag(0)).performClick()
        composeRule.onNodeWithText("Document type").assertIsDisplayed()
        composeRule.onNodeWithText("Utility provider document description").assertIsDisplayed()
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
            "This file type isn’t supported. Choose a file in one of the accepted formats."
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
    fun `document submission shows success and continues on done`() = runScenario(
        state = screenState(
            page = AdditionalKycCollectionPage.Submitted,
            submissionState = AdditionalKycSubmissionState.Submitted,
            completedDocumentCount = 1,
        ),
    ) {
        composeRule.onNodeWithText("Document uploaded successfully").assertIsDisplayed()
        composeRule.onNodeWithText(
            "You can continue while we verify your document in the background."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Submitted for review").assertDoesNotExist()
        composeRule.onNodeWithText("Done").performClick()
        assertThat(continued).isTrue()
        assertThat(closed).isFalse()
    }

    @Test
    fun `pending documents retain review message and close on done`() = runScenario(
        state = screenState(page = AdditionalKycCollectionPage.Pending),
    ) {
        composeRule.onNodeWithText("Submitted for review").assertIsDisplayed()
        composeRule.onNodeWithText(
            "We’re reviewing your documents. We’ll let you know when verification is complete."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Document uploaded successfully").assertDoesNotExist()
        composeRule.onNodeWithText("Done").performClick()
        assertThat(closed).isTrue()
        assertThat(continued).isFalse()
    }

    @Test
    fun `submitting blocks editing and shows progress`() = runScenario(
        state = screenState(
            page = AdditionalKycCollectionPage.DocumentEditor,
            submissionState = AdditionalKycSubmissionState.Submitting,
            canSubmit = false,
        ),
    ) {
        composeRule.onNodeWithTag("PrimaryButtonTag").assertIsNotEnabled()
        composeRule.onNodeWithTag(
            testTag = "CircularProgressIndicator",
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun `unavailable action closes instead of offering support`() = runScenario(
        state = screenState(page = AdditionalKycCollectionPage.Unavailable),
    ) {
        composeRule.onNodeWithText("Contact support").assertDoesNotExist()
        composeRule.onNodeWithText("Close").performClick()
        assertThat(closed).isTrue()
        assertThat(submitted).isFalse()
    }

    @Test
    fun `large text introduction scrolls while continue remains visible`() = runScenario(
        state = screenState(page = AdditionalKycCollectionPage.Context),
        fontScale = 2f,
        screenHeight = 500.dp,
    ) {
        composeRule.onNodeWithText("Tell us about your source of funds")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithText(
            "We may request source of funds for larger transactions."
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsDisplayed().performClick()
        assertThat(continued).isTrue()
    }

    @Test
    fun `large text upload success message scrolls while done remains visible`() = runScenario(
        state = screenState(
            page = AdditionalKycCollectionPage.Submitted,
            completedDocumentCount = 1,
        ),
        fontScale = 2f,
        screenHeight = 500.dp,
    ) {
        composeRule.onNodeWithText("Document uploaded successfully")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithText(
            "You can continue while we verify your document in the background."
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun `large text error message scrolls while close remains visible`() = runScenario(
        state = screenState(page = AdditionalKycCollectionPage.Unavailable),
        fontScale = 2f,
        screenHeight = 500.dp,
    ) {
        composeRule.onNodeWithText("Something went wrong")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithText("Please try again later.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Close").assertIsDisplayed()
    }

    private fun runScenario(
        state: AdditionalKycScreenState,
        fontScale: Float = 1f,
        screenHeight: Dp = Dp.Infinity,
        block: TestScenario.() -> Unit,
    ) {
        val scenario = TestScenario()
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Box {
                    Box(Modifier.heightIn(max = screenHeight)) {
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
                }
            }
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
                fileRequirements = "PDF, JPEG, or PNG, up to 5 MB per file.",
                instructions = listOf("Upload documents that support your transaction activity"),
                maxFileSizeMegabytes = 5,
                minDocumentTypes = 1,
                maxDocumentTypes = 10,
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
                        description = "Document description",
                        isEnabled = true,
                    ),
                    AdditionalKycDocumentSubtypeState(
                        id = "utility_bill",
                        label = "Utility bill",
                        description = "Utility provider document description",
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
