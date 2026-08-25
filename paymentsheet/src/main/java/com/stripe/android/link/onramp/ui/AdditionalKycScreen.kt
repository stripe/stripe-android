package com.stripe.android.link.onramp.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.paymentsheet.R

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AdditionalKycScreen(
    appearance: LinkAppearance.State?,
    state: AdditionalKycScreenState,
    onClose: () -> Unit,
    onQuestionAnswerChanged: (questionId: String, answer: String) -> Unit,
    onDocumentSubtypeSelected: (slotIndex: Int, subtypeId: String) -> Unit,
    onChooseFile: (slotIndex: Int) -> Unit,
    onRemoveFile: (slotIndex: Int) -> Unit,
    onSubmit: () -> Unit,
    onContinue: () -> Unit,
) {
    val canClose = state.submissionState != AdditionalKycSubmissionState.Submitting &&
        state.submissionState != AdditionalKycSubmissionState.Submitted
    BackHandler {
        if (canClose) {
            onClose()
        }
    }

    DefaultLinkTheme(appearance = appearance) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LinkTheme.colors.surfaceBackdrop.copy(alpha = 0.32f))
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxSize(),
                color = LinkTheme.colors.surfacePrimary,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                elevation = 8.dp,
            ) {
                AdditionalKycContent(
                    state = state,
                    onClose = onClose,
                    onQuestionAnswerChanged = onQuestionAnswerChanged,
                    onDocumentSubtypeSelected = onDocumentSubtypeSelected,
                    onChooseFile = onChooseFile,
                    onRemoveFile = onRemoveFile,
                    onSubmit = onSubmit,
                    onContinue = onContinue,
                )
            }
        }
    }
}

@Composable
private fun AdditionalKycContent(
    state: AdditionalKycScreenState,
    onClose: () -> Unit,
    onQuestionAnswerChanged: (questionId: String, answer: String) -> Unit,
    onDocumentSubtypeSelected: (slotIndex: Int, subtypeId: String) -> Unit,
    onChooseFile: (slotIndex: Int) -> Unit,
    onRemoveFile: (slotIndex: Int) -> Unit,
    onSubmit: () -> Unit,
    onContinue: () -> Unit,
) {
    val hasPendingRequirements = state.pendingRequirements.isNotEmpty()

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        AdditionalKycHeader(
            showClose = state.submissionState != AdditionalKycSubmissionState.Submitting &&
                state.submissionState != AdditionalKycSubmissionState.Submitted,
            closeLabelRes = if (hasPendingRequirements) {
                R.string.stripe_link_onramp_additional_kyc_close
            } else {
                R.string.stripe_link_onramp_additional_kyc_cancel
            },
            onClose = onClose,
        )

        AdditionalKycBody(
            state = state,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            onQuestionAnswerChanged = onQuestionAnswerChanged,
            onDocumentSubtypeSelected = onDocumentSubtypeSelected,
            onChooseFile = onChooseFile,
            onRemoveFile = onRemoveFile,
        )

        if (
            state.isCollectionAvailable ||
            state.submissionState == AdditionalKycSubmissionState.Submitted ||
            hasPendingRequirements
        ) {
            AdditionalKycPrimaryButton(
                state = state,
                onClose = onClose,
                onSubmit = onSubmit,
                onContinue = onContinue,
            )
        }
    }
}

@Composable
private fun AdditionalKycBody(
    state: AdditionalKycScreenState,
    modifier: Modifier,
    onQuestionAnswerChanged: (questionId: String, answer: String) -> Unit,
    onDocumentSubtypeSelected: (slotIndex: Int, subtypeId: String) -> Unit,
    onChooseFile: (slotIndex: Int) -> Unit,
    onRemoveFile: (slotIndex: Int) -> Unit,
) {
    when {
        state.submissionState == AdditionalKycSubmissionState.Submitted -> {
            AdditionalKycSubmittedContent(
                hasMoreRequirements = state.hasMoreRequirements,
                modifier = modifier,
            )
        }
        state.pendingRequirements.isNotEmpty() -> {
            AdditionalKycPendingContent(
                pendingRequirements = state.pendingRequirements,
                modifier = modifier,
            )
        }
        else -> {
            AdditionalKycForm(
                state = state,
                modifier = modifier,
                onQuestionAnswerChanged = onQuestionAnswerChanged,
                onDocumentSubtypeSelected = onDocumentSubtypeSelected,
                onChooseFile = onChooseFile,
                onRemoveFile = onRemoveFile,
            )
        }
    }
}

@Composable
private fun AdditionalKycForm(
    state: AdditionalKycScreenState,
    modifier: Modifier,
    onQuestionAnswerChanged: (questionId: String, answer: String) -> Unit,
    onDocumentSubtypeSelected: (slotIndex: Int, subtypeId: String) -> Unit,
    onChooseFile: (slotIndex: Int) -> Unit,
    onRemoveFile: (slotIndex: Int) -> Unit,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = requirementTitle(state.requirementType),
            style = LinkTheme.typography.title,
            color = LinkTheme.colors.textPrimary,
        )

        if (state.totalRequirements > 1) {
            Text(
                text = stringResource(
                    R.string.stripe_link_onramp_additional_kyc_step,
                    state.currentRequirement,
                    state.totalRequirements,
                ),
                style = LinkTheme.typography.caption,
                color = LinkTheme.colors.textTertiary,
            )
        }

        Text(
            text = stringResource(R.string.stripe_link_onramp_additional_kyc_explanation),
            style = LinkTheme.typography.body,
            color = LinkTheme.colors.textSecondary,
        )

        state.errorMessages.forEach { error ->
            ErrorText(
                text = error,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            )
        }

        if (state.submissionState == AdditionalKycSubmissionState.Failed) {
            ErrorText(
                text = stringResource(R.string.stripe_link_onramp_additional_kyc_submission_failed),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ADDITIONAL_KYC_SUBMISSION_ERROR_TAG),
                horizontalArrangement = Arrangement.Start,
            )
        }

        if (state.isCollectionAvailable) {
            AdditionalKycCollectionSections(
                state = state,
                onQuestionAnswerChanged = onQuestionAnswerChanged,
                onDocumentSubtypeSelected = onDocumentSubtypeSelected,
                onChooseFile = onChooseFile,
                onRemoveFile = onRemoveFile,
            )
        } else {
            ErrorText(
                text = stringResource(R.string.stripe_link_onramp_additional_kyc_unavailable),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            )
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun AdditionalKycCollectionSections(
    state: AdditionalKycScreenState,
    onQuestionAnswerChanged: (questionId: String, answer: String) -> Unit,
    onDocumentSubtypeSelected: (slotIndex: Int, subtypeId: String) -> Unit,
    onChooseFile: (slotIndex: Int) -> Unit,
    onRemoveFile: (slotIndex: Int) -> Unit,
) {
    QuestionnaireSection(
        questions = state.questions,
        enabled = state.submissionState != AdditionalKycSubmissionState.Submitting,
        showRequiredError = state.validationError ==
            AdditionalKycValidationError.MissingRequiredAnswers,
        onQuestionAnswerChanged = onQuestionAnswerChanged,
    )

    state.document?.let { document ->
        DocumentSection(
            document = document,
            enabled = state.submissionState != AdditionalKycSubmissionState.Submitting,
            selectingFileSlot = state.selectingFileSlot,
            onDocumentSubtypeSelected = onDocumentSubtypeSelected,
            onChooseFile = onChooseFile,
            onRemoveFile = onRemoveFile,
        )
    }

    state.validationError?.let { validationError ->
        ErrorText(
            text = validationErrorMessage(
                error = validationError,
                maxFileSizeMegabytes = state.document?.maxFileSizeMegabytes,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ADDITIONAL_KYC_VALIDATION_ERROR_TAG),
            horizontalArrangement = Arrangement.Start,
        )
    }
}

@Composable
private fun AdditionalKycPrimaryButton(
    state: AdditionalKycScreenState,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
    onContinue: () -> Unit,
) {
    val isSubmitted = state.submissionState == AdditionalKycSubmissionState.Submitted
    val hasPendingRequirements = state.pendingRequirements.isNotEmpty()
    PrimaryButton(
        label = if (hasPendingRequirements) {
            stringResource(R.string.stripe_link_onramp_additional_kyc_done)
        } else {
            when (state.submissionState) {
                AdditionalKycSubmissionState.Failed ->
                    stringResource(R.string.stripe_link_onramp_additional_kyc_retry)
                AdditionalKycSubmissionState.Submitted -> if (state.hasMoreRequirements) {
                    stringResource(R.string.stripe_link_onramp_additional_kyc_continue)
                } else {
                    stringResource(R.string.stripe_link_onramp_additional_kyc_done)
                }
                AdditionalKycSubmissionState.Collecting,
                AdditionalKycSubmissionState.Submitting ->
                    stringResource(R.string.stripe_link_onramp_additional_kyc_submit)
            }
        },
        state = when {
            state.submissionState == AdditionalKycSubmissionState.Submitting -> PrimaryButtonState.Processing
            state.canSubmit || isSubmitted || hasPendingRequirements -> PrimaryButtonState.Enabled
            else -> PrimaryButtonState.Disabled
        },
        onButtonClick = when {
            hasPendingRequirements -> onClose
            isSubmitted -> onContinue
            else -> onSubmit
        },
        allowedDisabledClicks = state.submissionState != AdditionalKycSubmissionState.Submitting,
        onDisabledButtonClick = onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag(ADDITIONAL_KYC_SUBMIT_BUTTON_TAG),
    )
}

@Composable
private fun AdditionalKycPendingContent(
    pendingRequirements: List<AdditionalKycPendingRequirementState>,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.stripe_link_onramp_additional_kyc_pending_title),
            style = LinkTheme.typography.title,
            color = LinkTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.stripe_link_onramp_additional_kyc_pending_explanation),
            style = LinkTheme.typography.body,
            color = LinkTheme.colors.textSecondary,
        )

        pendingRequirements.forEachIndexed { index, requirement ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = LinkTheme.colors.surfaceSecondary,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(16.dp)
                    .testTag(additionalKycPendingRequirementTag(index)),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = requirementTitle(requirement.requirementType),
                    style = LinkTheme.typography.bodyEmphasized,
                    color = LinkTheme.colors.textPrimary,
                )
                Text(
                    text = pendingRequirementTitle(requirement.status),
                    style = LinkTheme.typography.detailEmphasized,
                    color = LinkTheme.colors.textPrimary,
                )
                Text(
                    text = pendingRequirementMessage(requirement.status),
                    style = LinkTheme.typography.detail,
                    color = LinkTheme.colors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun AdditionalKycSubmittedContent(
    hasMoreRequirements: Boolean,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.stripe_link_onramp_additional_kyc_submitted_title),
            modifier = Modifier.testTag(ADDITIONAL_KYC_SUBMITTED_TITLE_TAG),
            style = LinkTheme.typography.title,
            color = LinkTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (hasMoreRequirements) {
                stringResource(R.string.stripe_link_onramp_additional_kyc_submitted_next)
            } else {
                stringResource(R.string.stripe_link_onramp_additional_kyc_submitted_done)
            },
            style = LinkTheme.typography.body,
            color = LinkTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun AdditionalKycHeader(
    showClose: Boolean,
    closeLabelRes: Int,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        if (showClose) {
            TextButton(
                onClick = onClose,
                modifier = Modifier.testTag(ADDITIONAL_KYC_CANCEL_BUTTON_TAG),
            ) {
                Text(
                    text = stringResource(closeLabelRes),
                    style = LinkTheme.typography.detailEmphasized,
                    color = LinkTheme.colors.textSecondary,
                )
            }
        } else {
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun QuestionnaireSection(
    questions: List<AdditionalKycQuestionState>,
    enabled: Boolean,
    showRequiredError: Boolean,
    onQuestionAnswerChanged: (questionId: String, answer: String) -> Unit,
) {
    if (questions.isEmpty()) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(R.string.stripe_link_onramp_additional_kyc_questions_title)

        questions.forEachIndexed { index, question ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (question.required) {
                        stringResource(
                            R.string.stripe_link_onramp_additional_kyc_required_question,
                            question.prompt,
                        )
                    } else {
                        question.prompt
                    },
                    style = LinkTheme.typography.detailEmphasized,
                    color = LinkTheme.colors.textPrimary,
                )

                OutlinedTextField(
                    value = question.answer,
                    onValueChange = { answer ->
                        onQuestionAnswerChanged(question.id, answer)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(additionalKycQuestionTag(question.id)),
                    placeholder = {
                        Text(stringResource(R.string.stripe_link_onramp_additional_kyc_answer_placeholder))
                    },
                    textStyle = LinkTheme.typography.body,
                    enabled = enabled,
                    isError = showRequiredError && question.required && question.answer.isBlank(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (index == questions.lastIndex) ImeAction.Done else ImeAction.Next,
                    ),
                    minLines = 2,
                    maxLines = 4,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = LinkTheme.colors.textPrimary,
                        cursorColor = LinkTheme.colors.textBrand,
                        focusedBorderColor = LinkTheme.colors.borderSelected,
                        unfocusedBorderColor = LinkTheme.colors.borderDefault,
                        errorBorderColor = LinkTheme.colors.borderCritical,
                        placeholderColor = LinkTheme.colors.textTertiary,
                        backgroundColor = LinkTheme.colors.surfacePrimary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DocumentSection(
    document: AdditionalKycDocumentState,
    enabled: Boolean,
    selectingFileSlot: Int?,
    onDocumentSubtypeSelected: (slotIndex: Int, subtypeId: String) -> Unit,
    onChooseFile: (slotIndex: Int) -> Unit,
    onRemoveFile: (slotIndex: Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(R.string.stripe_link_onramp_additional_kyc_documents_title)

        document.instructions.forEach { instruction ->
            Text(
                text = "\u2022 $instruction",
                style = LinkTheme.typography.detail,
                color = LinkTheme.colors.textSecondary,
            )
        }

        if (document.acceptedFormats.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.stripe_link_onramp_additional_kyc_accepted_formats,
                    document.acceptedFormats.joinToString(),
                ),
                style = LinkTheme.typography.caption,
                color = LinkTheme.colors.textTertiary,
            )
        }

        document.maxFileSizeMegabytes?.let { maxFileSizeMegabytes ->
            Text(
                text = stringResource(
                    R.string.stripe_link_onramp_additional_kyc_maximum_file_size,
                    maxFileSizeMegabytes,
                ),
                style = LinkTheme.typography.caption,
                color = LinkTheme.colors.textTertiary,
            )
        }

        document.slots.forEach { slot ->
            DocumentSlot(
                slot = slot,
                enabled = enabled,
                isSelectingFile = selectingFileSlot == slot.index,
                onDocumentSubtypeSelected = onDocumentSubtypeSelected,
                onChooseFile = onChooseFile,
                onRemoveFile = onRemoveFile,
            )
        }
    }
}

@Composable
private fun DocumentSlot(
    slot: AdditionalKycDocumentSlotState,
    enabled: Boolean,
    isSelectingFile: Boolean,
    onDocumentSubtypeSelected: (slotIndex: Int, subtypeId: String) -> Unit,
    onChooseFile: (slotIndex: Int) -> Unit,
    onRemoveFile: (slotIndex: Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = LinkTheme.colors.surfaceSecondary,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(
                R.string.stripe_link_onramp_additional_kyc_document_number,
                slot.index + 1,
            ),
            style = LinkTheme.typography.bodyEmphasized,
            color = LinkTheme.colors.textPrimary,
        )

        if (slot.subtypes.isNotEmpty()) {
            DocumentSubtypePicker(
                slot = slot,
                enabled = enabled,
                onDocumentSubtypeSelected = onDocumentSubtypeSelected,
            )
        }

        DocumentFileButton(
            slot = slot,
            enabled = enabled,
            isSelectingFile = isSelectingFile,
            onChooseFile = onChooseFile,
        )

        slot.fileName?.let { fileName ->
            SelectedDocumentFile(
                slotIndex = slot.index,
                fileName = fileName,
                enabled = enabled,
                onRemoveFile = onRemoveFile,
            )
        }
    }
}

@Composable
private fun DocumentFileButton(
    slot: AdditionalKycDocumentSlotState,
    enabled: Boolean,
    isSelectingFile: Boolean,
    onChooseFile: (slotIndex: Int) -> Unit,
) {
    OutlinedButton(
        onClick = { onChooseFile(slot.index) },
        enabled = enabled && !isSelectingFile,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(additionalKycChooseFileTag(slot.index)),
        border = BorderStroke(1.dp, LinkTheme.colors.borderDefault),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = LinkTheme.colors.surfacePrimary,
            contentColor = LinkTheme.colors.textPrimary,
            disabledContentColor = LinkTheme.colors.textTertiary,
        ),
    ) {
        Text(
            text = when {
                isSelectingFile -> stringResource(R.string.stripe_link_onramp_additional_kyc_adding_file)
                slot.fileName != null -> stringResource(R.string.stripe_link_onramp_additional_kyc_replace_file)
                else -> stringResource(R.string.stripe_link_onramp_additional_kyc_choose_file)
            },
            style = LinkTheme.typography.detailEmphasized,
        )
    }
}

@Composable
private fun SelectedDocumentFile(
    slotIndex: Int,
    fileName: String,
    enabled: Boolean,
    onRemoveFile: (slotIndex: Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = fileName,
            modifier = Modifier
                .weight(1f)
                .testTag(additionalKycFileNameTag(slotIndex)),
            style = LinkTheme.typography.detail,
            color = LinkTheme.colors.textPrimary,
        )
        TextButton(
            onClick = { onRemoveFile(slotIndex) },
            enabled = enabled,
            modifier = Modifier.testTag(additionalKycRemoveFileTag(slotIndex)),
        ) {
            Text(
                text = stringResource(R.string.stripe_link_onramp_additional_kyc_remove_file),
                style = LinkTheme.typography.detailEmphasized,
                color = LinkTheme.colors.textCritical,
            )
        }
    }
}

@Composable
private fun DocumentSubtypePicker(
    slot: AdditionalKycDocumentSlotState,
    enabled: Boolean,
    onDocumentSubtypeSelected: (slotIndex: Int, subtypeId: String) -> Unit,
) {
    var expanded by remember(slot.index) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(additionalKycSubtypePickerTag(slot.index)),
            border = BorderStroke(1.dp, LinkTheme.colors.borderDefault),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = LinkTheme.colors.surfacePrimary,
                contentColor = LinkTheme.colors.textPrimary,
            ),
        ) {
            Text(
                text = slot.selectedSubtypeLabel
                    ?: stringResource(R.string.stripe_link_onramp_additional_kyc_select_document_type),
                modifier = Modifier.fillMaxWidth(),
                style = LinkTheme.typography.detail,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(LinkTheme.colors.surfacePrimary),
        ) {
            slot.subtypes.forEach { subtype ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onDocumentSubtypeSelected(slot.index, subtype.id)
                    },
                    enabled = subtype.isEnabled,
                    modifier = Modifier.testTag(
                        additionalKycSubtypeOptionTag(slot.index, subtype.id)
                    ),
                ) {
                    Text(
                        text = subtype.label,
                        style = LinkTheme.typography.detail,
                        color = if (subtype.isEnabled) {
                            LinkTheme.colors.textPrimary
                        } else {
                            LinkTheme.colors.textTertiary
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(stringRes: Int) {
    Text(
        text = stringResource(stringRes),
        style = LinkTheme.typography.bodyEmphasized,
        color = LinkTheme.colors.textPrimary,
    )
}

@Composable
private fun requirementTitle(type: AdditionalKycRequirementType): String {
    val titleRes = when (type) {
        AdditionalKycRequirementType.ProofOfAddress ->
            R.string.stripe_link_onramp_additional_kyc_proof_of_address_title
        AdditionalKycRequirementType.SourceOfFunds ->
            R.string.stripe_link_onramp_additional_kyc_source_of_funds_title
        AdditionalKycRequirementType.AdditionalVerification ->
            R.string.stripe_link_onramp_additional_kyc_title
    }
    return stringResource(titleRes)
}

@Composable
private fun pendingRequirementTitle(status: AdditionalKycPendingRequirementStatus): String {
    val titleRes = when (status) {
        AdditionalKycPendingRequirementStatus.WaitingForReview ->
            R.string.stripe_link_onramp_additional_kyc_waiting_for_review_title
        AdditionalKycPendingRequirementStatus.Processing ->
            R.string.stripe_link_onramp_additional_kyc_processing_title
    }
    return stringResource(titleRes)
}

@Composable
private fun pendingRequirementMessage(status: AdditionalKycPendingRequirementStatus): String {
    val messageRes = when (status) {
        AdditionalKycPendingRequirementStatus.WaitingForReview ->
            R.string.stripe_link_onramp_additional_kyc_waiting_for_review_message
        AdditionalKycPendingRequirementStatus.Processing ->
            R.string.stripe_link_onramp_additional_kyc_processing_message
    }
    return stringResource(messageRes)
}

@Composable
private fun validationErrorMessage(
    error: AdditionalKycValidationError,
    maxFileSizeMegabytes: Int?,
): String {
    return when (error) {
        AdditionalKycValidationError.MissingRequiredAnswers ->
            stringResource(R.string.stripe_link_onramp_additional_kyc_missing_answers)
        AdditionalKycValidationError.MissingDocumentType ->
            stringResource(R.string.stripe_link_onramp_additional_kyc_missing_document_type)
        AdditionalKycValidationError.MissingDocuments ->
            stringResource(R.string.stripe_link_onramp_additional_kyc_missing_documents)
        AdditionalKycValidationError.DuplicateDocumentType ->
            stringResource(R.string.stripe_link_onramp_additional_kyc_duplicate_document_type)
        AdditionalKycValidationError.UnsupportedFileType ->
            stringResource(R.string.stripe_link_onramp_additional_kyc_unsupported_file_type)
        AdditionalKycValidationError.FileUnavailable ->
            stringResource(R.string.stripe_link_onramp_additional_kyc_file_unavailable)
        AdditionalKycValidationError.FileTooLarge -> if (maxFileSizeMegabytes == null) {
            stringResource(R.string.stripe_link_onramp_additional_kyc_file_too_large)
        } else {
            stringResource(
                R.string.stripe_link_onramp_additional_kyc_file_too_large_with_limit,
                maxFileSizeMegabytes,
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AdditionalKycScreenState(
    val requirementType: AdditionalKycRequirementType,
    val errorMessages: List<String>,
    val questions: List<AdditionalKycQuestionState>,
    val document: AdditionalKycDocumentState?,
    val validationError: AdditionalKycValidationError?,
    val selectingFileSlot: Int?,
    val canSubmit: Boolean,
    val isCollectionAvailable: Boolean,
    val submissionState: AdditionalKycSubmissionState,
    val currentRequirement: Int,
    val totalRequirements: Int,
    val hasMoreRequirements: Boolean,
    val pendingRequirements: List<AdditionalKycPendingRequirementState>,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AdditionalKycPendingRequirementState(
    val requirementType: AdditionalKycRequirementType,
    val status: AdditionalKycPendingRequirementStatus,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class AdditionalKycPendingRequirementStatus {
    WaitingForReview,
    Processing,
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class AdditionalKycSubmissionState {
    Collecting,
    Submitting,
    Failed,
    Submitted,
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class AdditionalKycRequirementType {
    ProofOfAddress,
    SourceOfFunds,
    AdditionalVerification,
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AdditionalKycQuestionState(
    val id: String,
    val prompt: String,
    val answer: String,
    val required: Boolean,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AdditionalKycDocumentState(
    val acceptedFormats: List<String>,
    val instructions: List<String>,
    val maxFileSizeMegabytes: Int?,
    val slots: List<AdditionalKycDocumentSlotState>,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AdditionalKycDocumentSlotState(
    val index: Int,
    val subtypes: List<AdditionalKycDocumentSubtypeState>,
    val selectedSubtypeLabel: String?,
    val fileName: String?,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AdditionalKycDocumentSubtypeState(
    val id: String,
    val label: String,
    val isEnabled: Boolean,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class AdditionalKycValidationError {
    MissingRequiredAnswers,
    MissingDocumentType,
    MissingDocuments,
    DuplicateDocumentType,
    UnsupportedFileType,
    FileUnavailable,
    FileTooLarge,
}

internal fun additionalKycQuestionTag(questionId: String): String =
    "AdditionalKycQuestion-$questionId"

internal fun additionalKycSubtypePickerTag(slotIndex: Int): String =
    "AdditionalKycSubtypePicker-$slotIndex"

internal fun additionalKycSubtypeOptionTag(slotIndex: Int, subtypeId: String): String =
    "AdditionalKycSubtypeOption-$slotIndex-$subtypeId"

internal fun additionalKycChooseFileTag(slotIndex: Int): String =
    "AdditionalKycChooseFile-$slotIndex"

internal fun additionalKycRemoveFileTag(slotIndex: Int): String =
    "AdditionalKycRemoveFile-$slotIndex"

internal fun additionalKycFileNameTag(slotIndex: Int): String =
    "AdditionalKycFileName-$slotIndex"

internal fun additionalKycPendingRequirementTag(index: Int): String =
    "AdditionalKycPendingRequirement-$index"

internal const val ADDITIONAL_KYC_CANCEL_BUTTON_TAG = "AdditionalKycCancelButton"
internal const val ADDITIONAL_KYC_SUBMIT_BUTTON_TAG = "AdditionalKycSubmitButton"
internal const val ADDITIONAL_KYC_VALIDATION_ERROR_TAG = "AdditionalKycValidationError"
internal const val ADDITIONAL_KYC_SUBMISSION_ERROR_TAG = "AdditionalKycSubmissionError"
internal const val ADDITIONAL_KYC_SUBMITTED_TITLE_TAG = "AdditionalKycSubmittedTitle"
