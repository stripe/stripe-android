package com.stripe.android.link.onramp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
    fun `renders partner content and forwards collection actions`() {
        var closed = false
        var submitted = false
        var changedAnswer: Pair<String, String>? = null
        var selectedSubtype: Pair<Int, String>? = null
        var chooseFileSlot: Int? = null
        var removedFileSlot: Int? = null

        composeRule.setContent {
            AdditionalKycScreen(
                appearance = null,
                state = screenState(),
                onClose = { closed = true },
                onQuestionAnswerChanged = { questionId, answer ->
                    changedAnswer = questionId to answer
                },
                onDocumentSubtypeSelected = { slotIndex, subtypeId ->
                    selectedSubtype = slotIndex to subtypeId
                },
                onChooseFile = { slotIndex -> chooseFileSlot = slotIndex },
                onRemoveFile = { slotIndex -> removedFileSlot = slotIndex },
                onSubmit = { submitted = true },
                onContinue = {},
            )
        }

        composeRule.onNodeWithText("Source of funds").assertIsDisplayed()
        composeRule.onNodeWithText("The previous document was rejected").assertIsDisplayed()
        composeRule.onNodeWithText("Show your full name and address", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Accepted formats: pdf, jpeg")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Maximum file size: 5 MB")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(additionalKycFileNameTag(0))
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag(additionalKycQuestionTag("funding_sources"))
            .performScrollTo()
            .performTextInput("Salary")
        composeRule.onNodeWithTag(additionalKycSubtypePickerTag(0))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(additionalKycSubtypeOptionTag(0, "payslip")).performClick()
        composeRule.onNodeWithTag(additionalKycChooseFileTag(0))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(additionalKycRemoveFileTag(0))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(PrimaryButtonTag).performClick()
        composeRule.onNodeWithTag(ADDITIONAL_KYC_CANCEL_BUTTON_TAG).performClick()

        assertThat(changedAnswer).isEqualTo("funding_sources" to "Salary")
        assertThat(selectedSubtype).isEqualTo(0 to "payslip")
        assertThat(chooseFileSlot).isEqualTo(0)
        assertThat(removedFileSlot).isEqualTo(0)
        assertThat(submitted).isTrue()
        assertThat(closed).isTrue()
    }

    @Test
    fun `renders localized validation message`() {
        composeRule.setContent {
            AdditionalKycScreen(
                appearance = null,
                state = screenState().copy(
                    validationError = AdditionalKycValidationError.MissingRequiredAnswers,
                    canSubmit = false,
                ),
                onClose = {},
                onQuestionAnswerChanged = { _, _ -> },
                onDocumentSubtypeSelected = { _, _ -> },
                onChooseFile = {},
                onRemoveFile = {},
                onSubmit = {},
                onContinue = {},
            )
        }

        composeRule.onNodeWithTag(ADDITIONAL_KYC_VALIDATION_ERROR_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Answer all required questions.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `oversized file shows configured size limit`() {
        runScenario(
            state = screenState().copy(
                validationError = AdditionalKycValidationError.FileTooLarge,
                canSubmit = false,
            ),
            onClose = {},
        ) {
            composeRule.onNodeWithTag(ADDITIONAL_KYC_VALIDATION_ERROR_TAG)
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithText("Choose a file that’s 5 MB or smaller.")
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun `submitting blocks editing and shows progress`() {
        composeRule.setContent {
            AdditionalKycScreen(
                appearance = null,
                state = screenState().copy(
                    submissionState = AdditionalKycSubmissionState.Submitting,
                    canSubmit = false,
                ),
                onClose = {},
                onQuestionAnswerChanged = { _, _ -> },
                onDocumentSubtypeSelected = { _, _ -> },
                onChooseFile = {},
                onRemoveFile = {},
                onSubmit = {},
                onContinue = {},
            )
        }

        composeRule.onNodeWithTag(additionalKycQuestionTag("funding_sources"))
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule.onNodeWithTag(additionalKycChooseFileTag(0))
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule.onNodeWithTag(ADDITIONAL_KYC_CANCEL_BUTTON_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(
            testTag = ProgressIndicatorTestTag,
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun `failed submission shows retry action`() {
        var retried = false
        composeRule.setContent {
            AdditionalKycScreen(
                appearance = null,
                state = screenState().copy(
                    submissionState = AdditionalKycSubmissionState.Failed,
                ),
                onClose = {},
                onQuestionAnswerChanged = { _, _ -> },
                onDocumentSubtypeSelected = { _, _ -> },
                onChooseFile = {},
                onRemoveFile = {},
                onSubmit = { retried = true },
                onContinue = {},
            )
        }

        composeRule.onNodeWithTag(ADDITIONAL_KYC_SUBMISSION_ERROR_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.onNodeWithTag(PrimaryButtonTag).performClick()

        assertThat(retried).isTrue()
    }

    @Test
    fun `submitted requirement with another remaining continues`() {
        var continued = false
        composeRule.setContent {
            AdditionalKycScreen(
                appearance = null,
                state = screenState().copy(
                    submissionState = AdditionalKycSubmissionState.Submitted,
                    canSubmit = false,
                    currentRequirement = 1,
                    totalRequirements = 2,
                    hasMoreRequirements = true,
                ),
                onClose = {},
                onQuestionAnswerChanged = { _, _ -> },
                onDocumentSubtypeSelected = { _, _ -> },
                onChooseFile = {},
                onRemoveFile = {},
                onSubmit = {},
                onContinue = { continued = true },
            )
        }

        composeRule.onNodeWithTag(ADDITIONAL_KYC_SUBMITTED_TITLE_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
        composeRule.onNodeWithTag(additionalKycQuestionTag("funding_sources")).assertDoesNotExist()
        composeRule.onNodeWithTag(ADDITIONAL_KYC_CANCEL_BUTTON_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(PrimaryButtonTag).performClick()

        assertThat(continued).isTrue()
    }

    @Test
    fun `partner requirement shows waiting for review state`() {
        var closed = false

        runScenario(
            state = screenState().copy(
                questions = emptyList(),
                document = null,
                canSubmit = false,
                isCollectionAvailable = false,
                pendingRequirements = listOf(
                    AdditionalKycPendingRequirementState(
                        requirementType = AdditionalKycRequirementType.ProofOfAddress,
                        status = AdditionalKycPendingRequirementStatus.WaitingForReview,
                    )
                ),
            ),
            onClose = { closed = true },
        ) {
            composeRule.onNodeWithTag(additionalKycPendingRequirementTag(0)).assertIsDisplayed()
            composeRule.onNodeWithText("Proof of address").assertIsDisplayed()
            composeRule.onNodeWithText("Waiting for review").assertIsDisplayed()
            composeRule.onNodeWithText("Close").assertIsDisplayed()
            composeRule.onNodeWithTag(additionalKycQuestionTag("funding_sources")).assertDoesNotExist()
            composeRule.onNodeWithText("Done").performClick()

            assertThat(closed).isTrue()
        }
    }

    @Test
    fun `Stripe requirement shows processing state`() {
        runScenario(
            state = screenState().copy(
                questions = emptyList(),
                document = null,
                canSubmit = false,
                isCollectionAvailable = false,
                pendingRequirements = listOf(
                    AdditionalKycPendingRequirementState(
                        requirementType = AdditionalKycRequirementType.SourceOfFunds,
                        status = AdditionalKycPendingRequirementStatus.Processing,
                    )
                ),
            ),
            onClose = {},
        ) {
            composeRule.onNodeWithTag(additionalKycPendingRequirementTag(0)).assertIsDisplayed()
            composeRule.onNodeWithText("Source of funds").assertIsDisplayed()
            composeRule.onNodeWithText("Processing").assertIsDisplayed()
            composeRule.onNodeWithText("Your information is still being processed.").assertIsDisplayed()
            composeRule.onNodeWithTag(additionalKycChooseFileTag(0)).assertDoesNotExist()
        }
    }

    private fun runScenario(
        state: AdditionalKycScreenState,
        onClose: () -> Unit,
        block: () -> Unit,
    ) {
        composeRule.setContent {
            AdditionalKycScreen(
                appearance = null,
                state = state,
                onClose = onClose,
                onQuestionAnswerChanged = { _, _ -> },
                onDocumentSubtypeSelected = { _, _ -> },
                onChooseFile = {},
                onRemoveFile = {},
                onSubmit = {},
                onContinue = {},
            )
        }

        block()
    }

    private companion object {
        fun screenState(): AdditionalKycScreenState {
            return AdditionalKycScreenState(
                requirementType = AdditionalKycRequirementType.SourceOfFunds,
                errorMessages = listOf("The previous document was rejected"),
                questions = listOf(
                    AdditionalKycQuestionState(
                        id = "funding_sources",
                        prompt = "How are you funding your transactions?",
                        answer = "",
                        required = true,
                    )
                ),
                document = AdditionalKycDocumentState(
                    acceptedFormats = listOf("pdf", "jpeg"),
                    instructions = listOf("Show your full name and address"),
                    maxFileSizeMegabytes = 5,
                    slots = listOf(
                        AdditionalKycDocumentSlotState(
                            index = 0,
                            subtypes = listOf(
                                AdditionalKycDocumentSubtypeState(
                                    id = "bank_statement",
                                    label = "Bank statement",
                                    isEnabled = true,
                                ),
                                AdditionalKycDocumentSubtypeState(
                                    id = "payslip",
                                    label = "Payslip",
                                    isEnabled = true,
                                ),
                            ),
                            selectedSubtypeLabel = "Bank statement",
                            fileName = "statement.pdf",
                        )
                    ),
                ),
                validationError = null,
                selectingFileSlot = null,
                canSubmit = true,
                isCollectionAvailable = true,
                submissionState = AdditionalKycSubmissionState.Collecting,
                currentRequirement = 1,
                totalRequirements = 1,
                hasMoreRequirements = false,
                pendingRequirements = emptyList(),
            )
        }
    }
}
