package com.stripe.android.link.onramp.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.paymentsheet.R
import java.util.Locale

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AdditionalKycScreen(
    appearance: LinkAppearance.State?,
    state: AdditionalKycScreenState,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onQuestionAnswerChanged: (questionId: String, answer: String) -> Unit,
    onDocumentSubtypeSelected: (slotIndex: Int, subtypeId: String) -> Unit,
    onChooseFile: (slotIndex: Int) -> Unit,
    onRemoveFile: (slotIndex: Int) -> Unit,
    onAddDocuments: () -> Unit,
    onEditDocuments: (slotIndex: Int) -> Unit,
    onSubmit: () -> Unit,
    onContinue: () -> Unit,
) {
    var selectorSlotIndex by remember { mutableStateOf<Int?>(null) }
    val canNavigate = state.submissionState != AdditionalKycSubmissionState.Submitting &&
        state.page !in setOf(
            AdditionalKycCollectionPage.Submitted,
            AdditionalKycCollectionPage.Pending,
        )

    BackHandler {
        if (selectorSlotIndex != null) {
            selectorSlotIndex = null
        } else if (canNavigate) {
            onBack()
        }
    }

    DefaultLinkTheme(appearance = appearance) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LinkTheme.colors.surfaceBackdrop.copy(alpha = 0.20f))
                .statusBarsPadding()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = LinkTheme.colors.surfacePrimary,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                elevation = 8.dp,
            ) {
                if (selectorSlotIndex != null) {
                    val slot = state.document?.slots?.firstOrNull { it.index == selectorSlotIndex }
                    if (slot != null) {
                        DocumentTypeSelector(
                            requirementType = state.requirementType,
                            slot = slot,
                            onClose = { selectorSlotIndex = null },
                            onSelected = { subtypeId ->
                                onDocumentSubtypeSelected(slot.index, subtypeId)
                                selectorSlotIndex = null
                            },
                        )
                    }
                } else {
                    AdditionalKycContent(
                        state = state,
                        onClose = onClose,
                        onBack = onBack,
                        onQuestionAnswerChanged = onQuestionAnswerChanged,
                        onShowDocumentTypes = { selectorSlotIndex = it },
                        onChooseFile = onChooseFile,
                        onRemoveFile = onRemoveFile,
                        onAddDocuments = onAddDocuments,
                        onEditDocuments = onEditDocuments,
                        onSubmit = onSubmit,
                        onContinue = onContinue,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdditionalKycContent(
    state: AdditionalKycScreenState,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onQuestionAnswerChanged: (questionId: String, answer: String) -> Unit,
    onShowDocumentTypes: (slotIndex: Int) -> Unit,
    onChooseFile: (slotIndex: Int) -> Unit,
    onRemoveFile: (slotIndex: Int) -> Unit,
    onAddDocuments: () -> Unit,
    onEditDocuments: (slotIndex: Int) -> Unit,
    onSubmit: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SheetHandle()
        val showHeader = state.page !in setOf(
            AdditionalKycCollectionPage.Submitted,
            AdditionalKycCollectionPage.Pending,
            AdditionalKycCollectionPage.Unavailable,
        )
        if (showHeader) {
            AdditionalKycHeader(
                showBack = state.page != AdditionalKycCollectionPage.Context,
                onBack = onBack,
                onClose = onClose,
            )
        } else {
            Spacer(Modifier.height(54.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (state.page) {
                AdditionalKycCollectionPage.Context -> ContextContent(state.requirementType)
                AdditionalKycCollectionPage.Questionnaire -> QuestionnaireContent(
                    state = state,
                    onQuestionAnswerChanged = onQuestionAnswerChanged,
                )
                AdditionalKycCollectionPage.DocumentOverview -> DocumentOverviewContent(
                    state = state,
                    onAddDocuments = onAddDocuments,
                    onEditDocuments = onEditDocuments,
                )
                AdditionalKycCollectionPage.DocumentEditor -> DocumentEditorContent(
                    state = state,
                    onShowDocumentTypes = onShowDocumentTypes,
                    onChooseFile = onChooseFile,
                    onRemoveFile = onRemoveFile,
                )
                AdditionalKycCollectionPage.Submitted,
                AdditionalKycCollectionPage.Pending,
                -> SubmittedContent()
                AdditionalKycCollectionPage.Unavailable -> UnavailableContent()
            }
        }

        AdditionalKycPrimaryButton(
            state = state,
            onClose = onClose,
            onSubmit = onSubmit,
            onContinue = onContinue,
        )
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .background(
                    color = LinkTheme.colors.borderDefault,
                    shape = RoundedCornerShape(2.dp),
                )
        )
    }
}

@Composable
private fun AdditionalKycHeader(
    showBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            HeaderIcon(
                iconRes = R.drawable.stripe_link_chevron_left_kyc,
                contentDescription = stringResource(R.string.stripe_link_onramp_additional_kyc_back),
                testTag = ADDITIONAL_KYC_BACK_BUTTON_TAG,
                onClick = onBack,
            )
        } else {
            Spacer(Modifier.size(44.dp))
        }
        HeaderIcon(
            iconRes = R.drawable.stripe_link_close_kyc,
            contentDescription = stringResource(R.string.stripe_link_onramp_additional_kyc_cancel),
            testTag = ADDITIONAL_KYC_CANCEL_BUTTON_TAG,
            onClick = onClose,
        )
    }
}

@Composable
private fun HeaderIcon(
    iconRes: Int,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(LinkTheme.colors.surfacePrimary)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = LinkTheme.colors.iconPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ContextContent(requirementType: AdditionalKycRequirementType) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(LinkTheme.colors.surfaceSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (requirementType == AdditionalKycRequirementType.ProofOfAddress) {
                        R.drawable.stripe_link_location
                    } else {
                        R.drawable.stripe_link_wallet
                    }
                ),
                contentDescription = null,
                tint = LinkTheme.colors.iconPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (requirementType == AdditionalKycRequirementType.ProofOfAddress) {
                stringResource(R.string.stripe_link_onramp_additional_kyc_proof_of_address_context_title)
            } else {
                stringResource(R.string.stripe_link_onramp_additional_kyc_source_of_funds_context_title)
            },
            style = LinkTheme.typography.title,
            color = LinkTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (requirementType == AdditionalKycRequirementType.ProofOfAddress) {
                stringResource(R.string.stripe_link_onramp_additional_kyc_proof_of_address_context_message)
            } else {
                stringResource(R.string.stripe_link_onramp_additional_kyc_source_of_funds_context_message)
            },
            style = LinkTheme.typography.body,
            color = LinkTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun QuestionnaireContent(
    state: AdditionalKycScreenState,
    onQuestionAnswerChanged: (questionId: String, answer: String) -> Unit,
) {
    val questions = state.questions.filterNot { question ->
        state.document != null && question.id == FUNDING_SOURCES_QUESTION_ID
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenTitle(state.requirementType)
        questions.forEachIndexed { index, question ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = question.prompt,
                    style = LinkTheme.typography.detail,
                    color = LinkTheme.colors.textPrimary,
                )
                OutlinedTextField(
                    value = question.answer,
                    onValueChange = { answer -> onQuestionAnswerChanged(question.id, answer) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(additionalKycQuestionTag(question.id)),
                    label = { Text(stringResource(R.string.stripe_link_onramp_additional_kyc_answer)) },
                    textStyle = LinkTheme.typography.body,
                    singleLine = false,
                    minLines = 1,
                    maxLines = 3,
                    isError = state.validationError == AdditionalKycValidationError.MissingRequiredAnswers &&
                        question.required && question.answer.isBlank(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (index == questions.lastIndex) ImeAction.Done else ImeAction.Next,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = LinkTheme.colors.textPrimary,
                        cursorColor = LinkTheme.colors.textPrimary,
                        focusedBorderColor = LinkTheme.colors.borderSelected,
                        unfocusedBorderColor = LinkTheme.colors.surfaceSecondary,
                        errorBorderColor = LinkTheme.colors.borderCritical,
                        backgroundColor = LinkTheme.colors.surfaceSecondary,
                        focusedLabelColor = LinkTheme.colors.textTertiary,
                        unfocusedLabelColor = LinkTheme.colors.textTertiary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ScreenTitle(requirementType: AdditionalKycRequirementType) {
    Text(
        text = if (requirementType == AdditionalKycRequirementType.ProofOfAddress) {
            stringResource(R.string.stripe_link_onramp_additional_kyc_proof_of_address_upload_title)
        } else {
            stringResource(R.string.stripe_link_onramp_additional_kyc_source_of_funds_context_title)
        },
        modifier = Modifier.fillMaxWidth(),
        style = LinkTheme.typography.title,
        color = LinkTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun DocumentOverviewContent(
    state: AdditionalKycScreenState,
    onAddDocuments: () -> Unit,
    onEditDocuments: (slotIndex: Int) -> Unit,
) {
    val completedSlots = state.document?.slots.orEmpty().filter { it.fileName != null }
    val groups = completedSlots.groupBy { it.selectedSubtypeId }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenTitle(state.requirementType)
        Text(
            text = stringResource(R.string.stripe_link_onramp_additional_kyc_funding_sources_prompt),
            style = LinkTheme.typography.detail,
            color = LinkTheme.colors.textPrimary,
        )
        SourceDocumentsCard(
            groups = groups,
            onAddDocuments = onAddDocuments,
            onEditDocuments = onEditDocuments,
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun SourceDocumentsCard(
    groups: Map<String?, List<AdditionalKycDocumentSlotState>>,
    onAddDocuments: () -> Unit,
    onEditDocuments: (slotIndex: Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LinkTheme.colors.surfaceSecondary, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        groups.values.forEach { slots ->
            val first = slots.first()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditDocuments(first.index) }
                    .padding(vertical = 4.dp)
                    .testTag(additionalKycDocumentGroupTag(first.index)),
            ) {
                Text(
                    text = first.selectedSubtypeLabel.orEmpty(),
                    style = LinkTheme.typography.detail,
                    color = LinkTheme.colors.textTertiary,
                )
                slots.forEach { slot ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(LinkTheme.colors.surfaceTertiary, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.stripe_link_document),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = slot.fileName.orEmpty(),
                                style = LinkTheme.typography.detail,
                                color = LinkTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        ChevronRight()
                    }
                }
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(LinkTheme.colors.borderDefault)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAddDocuments)
                .padding(vertical = 10.dp)
                .testTag(ADDITIONAL_KYC_ADD_DOCUMENTS_TAG),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(LinkTheme.colors.surfaceTertiary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.stripe_link_add),
                    contentDescription = null,
                    tint = LinkTheme.colors.iconPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.stripe_link_onramp_additional_kyc_add_documents),
                modifier = Modifier.weight(1f),
                style = LinkTheme.typography.body,
                color = LinkTheme.colors.textPrimary,
            )
            ChevronRight()
        }
    }
}

@Composable
private fun ChevronRight() {
    Icon(
        painter = painterResource(R.drawable.stripe_link_chevron_right_kyc),
        contentDescription = null,
        tint = LinkTheme.colors.iconTertiary,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
@Suppress("LongMethod")
private fun DocumentEditorContent(
    state: AdditionalKycScreenState,
    onShowDocumentTypes: (slotIndex: Int) -> Unit,
    onChooseFile: (slotIndex: Int) -> Unit,
    onRemoveFile: (slotIndex: Int) -> Unit,
) {
    val document = state.document ?: return
    val editingSlot = document.slots.firstOrNull { it.index == document.editingSlotIndex }
        ?: document.slots.firstOrNull { it.fileName == null }
        ?: document.slots.lastOrNull()
        ?: return
    val selectedSubtypeId = editingSlot.selectedSubtypeId
    val completedSlots = document.slots.filter { slot ->
        val matchesSelectedType = state.requirementType == AdditionalKycRequirementType.ProofOfAddress ||
            slot.selectedSubtypeId == selectedSubtypeId
        slot.fileName != null && matchesSelectedType
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(state.requirementType)
        if (state.requirementType == AdditionalKycRequirementType.ProofOfAddress) {
            Text(
                text = stringResource(R.string.stripe_link_onramp_additional_kyc_proof_of_address_upload_message),
                modifier = Modifier.fillMaxWidth(),
                style = LinkTheme.typography.body,
                color = LinkTheme.colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }

        DocumentTypeField(
            requirementType = state.requirementType,
            slot = editingSlot,
            onClick = { onShowDocumentTypes(editingSlot.index) },
        )

        if (state.validationError in setOf(
                AdditionalKycValidationError.UnsupportedFileType,
                AdditionalKycValidationError.FileTooLarge,
            )
        ) {
            FileErrorCard(state = state, slotIndex = editingSlot.index, onRemoveFile = onRemoveFile)
        } else {
            completedSlots.forEach { slot ->
                UploadedFileCard(slot = slot, onRemoveFile = onRemoveFile)
            }
            val showUpload = state.requirementType == AdditionalKycRequirementType.SourceOfFunds ||
                completedSlots.size < document.minDocuments
            if (showUpload && editingSlot.fileName == null) {
                UploadDocumentControl(
                    slot = editingSlot,
                    isUploading = state.selectingFileSlot == editingSlot.index,
                    uploadingFileName = state.selectingFileName,
                    enabled = selectedSubtypeId != null && state.selectingFileSlot == null,
                    onChooseFile = onChooseFile,
                )
            }
        }

        if (state.validationError == AdditionalKycValidationError.FileUnavailable) {
            InlineUploadError()
        }

        DocumentInstructions(
            requirementType = state.requirementType,
            subtypeId = selectedSubtypeId,
            serverInstructions = document.instructions,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DocumentTypeField(
    requirementType: AdditionalKycRequirementType,
    slot: AdditionalKycDocumentSlotState,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LinkTheme.colors.surfaceSecondary, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(additionalKycSubtypePickerTag(slot.index)),
    ) {
        Text(
            text = if (requirementType == AdditionalKycRequirementType.ProofOfAddress) {
                stringResource(R.string.stripe_link_onramp_additional_kyc_document_type)
            } else {
                stringResource(R.string.stripe_link_onramp_additional_kyc_funds_source)
            },
            style = LinkTheme.typography.caption,
            color = LinkTheme.colors.textTertiary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = slot.selectedSubtypeLabel
                    ?: stringResource(R.string.stripe_link_onramp_additional_kyc_select_document_type),
                modifier = Modifier.weight(1f),
                style = LinkTheme.typography.body,
                color = LinkTheme.colors.textPrimary,
            )
            Icon(
                painter = painterResource(R.drawable.stripe_link_chevron_down),
                contentDescription = null,
                tint = LinkTheme.colors.iconPrimary,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun UploadDocumentControl(
    slot: AdditionalKycDocumentSlotState,
    isUploading: Boolean,
    uploadingFileName: String?,
    enabled: Boolean,
    onChooseFile: (slotIndex: Int) -> Unit,
) {
    val borderColor = LinkTheme.colors.textTertiary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .dashedBorder(borderColor)
            .clickable(enabled = enabled) { onChooseFile(slot.index) }
            .padding(horizontal = 16.dp)
            .testTag(additionalKycChooseFileTag(slot.index)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isUploading) {
            Text("◔", style = LinkTheme.typography.title, color = LinkTheme.colors.textBrand)
        } else {
            Icon(
                painter = painterResource(R.drawable.stripe_link_upload),
                contentDescription = null,
                tint = if (enabled) LinkTheme.colors.iconPrimary else LinkTheme.colors.iconTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = if (isUploading && uploadingFileName != null) {
                    uploadingFileName
                } else {
                    stringResource(R.string.stripe_link_onramp_additional_kyc_upload_document)
                },
                style = LinkTheme.typography.body,
                color = if (enabled || isUploading) LinkTheme.colors.textPrimary else LinkTheme.colors.textTertiary,
            )
            Text(
                text = if (isUploading) {
                    stringResource(R.string.stripe_link_onramp_additional_kyc_uploading)
                } else {
                    stringResource(R.string.stripe_link_onramp_additional_kyc_file_requirements)
                },
                style = LinkTheme.typography.caption,
                color = LinkTheme.colors.textTertiary,
            )
        }
    }
}

@Composable
private fun UploadedFileCard(
    slot: AdditionalKycDocumentSlotState,
    onRemoveFile: (slotIndex: Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LinkTheme.colors.surfaceSecondary, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.stripe_link_check_circle),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = slot.fileName.orEmpty(),
                modifier = Modifier.testTag(additionalKycFileNameTag(slot.index)),
                style = LinkTheme.typography.body,
                color = LinkTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.stripe_link_onramp_additional_kyc_uploaded),
                style = LinkTheme.typography.caption,
                color = LinkTheme.colors.textTertiary,
            )
        }
        Icon(
            painter = painterResource(R.drawable.stripe_link_trash),
            contentDescription = stringResource(R.string.stripe_link_onramp_additional_kyc_remove_file),
            tint = LinkTheme.colors.iconPrimary,
            modifier = Modifier
                .size(16.dp)
                .clickable { onRemoveFile(slot.index) }
                .testTag(additionalKycRemoveFileTag(slot.index)),
        )
    }
}

@Composable
private fun FileErrorCard(
    state: AdditionalKycScreenState,
    slotIndex: Int,
    onRemoveFile: (slotIndex: Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LinkTheme.colors.textCritical.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(ADDITIONAL_KYC_VALIDATION_ERROR_TAG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.stripe_link_error_circle),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = state.validationFileName
                    ?: stringResource(R.string.stripe_link_onramp_additional_kyc_selected_document),
                style = LinkTheme.typography.body,
                color = LinkTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = validationErrorMessage(state.validationError, state.document?.maxFileSizeMegabytes),
                style = LinkTheme.typography.caption,
                color = LinkTheme.colors.textCritical,
            )
        }
        Icon(
            painter = painterResource(R.drawable.stripe_link_trash),
            contentDescription = stringResource(R.string.stripe_link_onramp_additional_kyc_remove_file),
            tint = LinkTheme.colors.iconPrimary,
            modifier = Modifier
                .size(16.dp)
                .clickable { onRemoveFile(slotIndex) },
        )
    }
}

@Composable
private fun InlineUploadError() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ADDITIONAL_KYC_VALIDATION_ERROR_TAG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.stripe_link_error_circle),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.stripe_link_onramp_additional_kyc_upload_failed),
            style = LinkTheme.typography.caption,
            color = LinkTheme.colors.textCritical,
        )
    }
}

@Composable
private fun DocumentInstructions(
    requirementType: AdditionalKycRequirementType,
    subtypeId: String?,
    serverInstructions: List<String>,
) {
    if (requirementType == AdditionalKycRequirementType.ProofOfAddress) {
        BulletList(
            items = listOf(
                stringResource(R.string.stripe_link_onramp_additional_kyc_proof_instruction_name_address),
                stringResource(R.string.stripe_link_onramp_additional_kyc_proof_instruction_id_valid),
                stringResource(R.string.stripe_link_onramp_additional_kyc_proof_instruction_recent),
                stringResource(R.string.stripe_link_onramp_additional_kyc_proof_instruction_corners),
                stringResource(R.string.stripe_link_onramp_additional_kyc_proof_instruction_digital),
            )
        )
        return
    }

    val normalizedSubtype = subtypeId.orEmpty().lowercase(Locale.ROOT)
    val isSalary = "salary" in normalizedSubtype || "payslip" in normalizedSubtype
    val isCompanyProfits = "company" in normalizedSubtype || "profit" in normalizedSubtype
    val documents = when {
        isSalary -> listOf(
            stringResource(R.string.stripe_link_onramp_additional_kyc_salary_document_payslips),
            stringResource(R.string.stripe_link_onramp_additional_kyc_salary_document_statements),
        )
        isCompanyProfits -> listOf(
            stringResource(R.string.stripe_link_onramp_additional_kyc_company_profits_document),
        )
        else -> serverInstructions
    }
    val criteria = when {
        isSalary -> listOf(
            stringResource(R.string.stripe_link_onramp_additional_kyc_salary_criteria_date),
            stringResource(R.string.stripe_link_onramp_additional_kyc_criteria_value),
            stringResource(R.string.stripe_link_onramp_additional_kyc_criteria_name),
            stringResource(R.string.stripe_link_onramp_additional_kyc_salary_criteria_payslip),
        )
        isCompanyProfits -> listOf(
            stringResource(R.string.stripe_link_onramp_additional_kyc_company_profits_criteria_date),
            stringResource(R.string.stripe_link_onramp_additional_kyc_criteria_value),
            stringResource(R.string.stripe_link_onramp_additional_kyc_criteria_name),
        )
        else -> emptyList()
    }
    if (documents.isNotEmpty()) {
        SectionHeading(R.string.stripe_link_onramp_additional_kyc_document_section_title)
        BulletList(documents)
    }
    if (criteria.isNotEmpty()) {
        SectionHeading(R.string.stripe_link_onramp_additional_kyc_acceptance_criteria)
        BulletList(criteria)
    }
}

@Composable
private fun SectionHeading(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = LinkTheme.typography.bodyEmphasized,
        color = LinkTheme.colors.textPrimary,
    )
}

@Composable
private fun BulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Row {
                Text("•", style = LinkTheme.typography.detail, color = LinkTheme.colors.textPrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item,
                    modifier = Modifier.weight(1f),
                    style = LinkTheme.typography.detail,
                    color = LinkTheme.colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun SubmittedContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(LinkTheme.colors.surfaceSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.stripe_link_clock),
                contentDescription = null,
                tint = LinkTheme.colors.iconPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.stripe_link_onramp_additional_kyc_submitted_title),
            modifier = Modifier.testTag(ADDITIONAL_KYC_SUBMITTED_TITLE_TAG),
            style = LinkTheme.typography.title,
            color = LinkTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.stripe_link_onramp_additional_kyc_submitted_review_message),
            style = LinkTheme.typography.body,
            color = LinkTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun UnavailableContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(LinkTheme.colors.textCritical, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.stripe_link_error_template),
                contentDescription = null,
                tint = LinkTheme.colors.iconWhite,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.stripe_link_onramp_additional_kyc_something_went_wrong),
            style = LinkTheme.typography.title,
            color = LinkTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.stripe_link_onramp_additional_kyc_try_again_later),
            style = LinkTheme.typography.body,
            color = LinkTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
@Suppress("LongMethod")
private fun DocumentTypeSelector(
    requirementType: AdditionalKycRequirementType,
    slot: AdditionalKycDocumentSlotState,
    onClose: () -> Unit,
    onSelected: (subtypeId: String) -> Unit,
) {
    BackHandler(onBack = onClose)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        SheetHandle()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(44.dp))
            Text(
                text = stringResource(R.string.stripe_link_onramp_additional_kyc_document_type),
                modifier = Modifier.weight(1f),
                style = LinkTheme.typography.bodyEmphasized,
                color = LinkTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            HeaderIcon(
                iconRes = R.drawable.stripe_link_close_kyc,
                contentDescription = stringResource(R.string.stripe_link_onramp_additional_kyc_close),
                testTag = ADDITIONAL_KYC_SELECTOR_CLOSE_TAG,
                onClick = onClose,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            slot.subtypes.forEach { subtype ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = subtype.isEnabled) { onSelected(subtype.id) }
                        .padding(vertical = 6.dp)
                        .testTag(additionalKycSubtypeOptionTag(slot.index, subtype.id)),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(
                                if (subtype.id == slot.selectedSubtypeId) {
                                    R.drawable.stripe_link_radio_filled
                                } else {
                                    R.drawable.stripe_link_radio_unfilled
                                }
                            ),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(width = 20.dp, height = 24.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.padding(top = 10.dp)) {
                        Text(
                            text = subtype.label,
                            style = LinkTheme.typography.bodyEmphasized,
                            color = LinkTheme.colors.textPrimary,
                        )
                        if (requirementType == AdditionalKycRequirementType.ProofOfAddress) {
                            proofOfAddressSubtypeDescription(subtype.id, subtype.label)?.let { description ->
                                Text(
                                    text = description,
                                    style = LinkTheme.typography.detail,
                                    color = LinkTheme.colors.textTertiary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun proofOfAddressSubtypeDescription(id: String, label: String): String? {
    val value = "$id $label".lowercase(Locale.ROOT)
    return when {
        "id" in value -> stringResource(R.string.stripe_link_onramp_additional_kyc_id_document_description)
        "government" in value -> stringResource(
            R.string.stripe_link_onramp_additional_kyc_government_document_description
        )
        "utility" in value -> stringResource(R.string.stripe_link_onramp_additional_kyc_utility_bill_description)
        "bank" in value -> stringResource(R.string.stripe_link_onramp_additional_kyc_bank_document_description)
        "lease" in value -> stringResource(R.string.stripe_link_onramp_additional_kyc_lease_agreement_description)
        else -> null
    }
}

@Composable
@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun AdditionalKycPrimaryButton(
    state: AdditionalKycScreenState,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
    onContinue: () -> Unit,
) {
    val document = state.document
    val editingSubtype = document?.slots
        ?.firstOrNull { it.index == document.editingSlotIndex }
        ?.selectedSubtypeId
    val editingDocumentCount = document?.slots.orEmpty().count { slot ->
        slot.fileName != null && slot.selectedSubtypeId == editingSubtype
    }
    val isSubmitted = state.page == AdditionalKycCollectionPage.Submitted
    val isPending = state.page == AdditionalKycCollectionPage.Pending
    val isUnavailable = state.page == AdditionalKycCollectionPage.Unavailable
    val isDocumentEditor = state.page == AdditionalKycCollectionPage.DocumentEditor
    val isQuestionnaireOnly = state.page == AdditionalKycCollectionPage.Questionnaire &&
        state.document == null
    val isSourceEditor = isDocumentEditor &&
        state.requirementType == AdditionalKycRequirementType.SourceOfFunds
    val label = when {
        isUnavailable -> stringResource(R.string.stripe_link_onramp_additional_kyc_contact_support)
        isSubmitted || isPending -> stringResource(R.string.stripe_link_onramp_additional_kyc_done)
        isSourceEditor -> stringResource(
            if (editingDocumentCount == 1) {
                R.string.stripe_link_onramp_additional_kyc_add_one_document
            } else {
                R.string.stripe_link_onramp_additional_kyc_add_multiple_documents
            },
            editingDocumentCount,
        )
        state.page in setOf(
            AdditionalKycCollectionPage.Context,
            AdditionalKycCollectionPage.Questionnaire,
        ) && !isQuestionnaireOnly -> stringResource(R.string.stripe_link_onramp_additional_kyc_continue)
        else -> stringResource(R.string.stripe_link_onramp_additional_kyc_submit)
    }
    val enabled = when {
        isUnavailable || isSubmitted || isPending -> true
        state.submissionState == AdditionalKycSubmissionState.Submitting -> false
        isSourceEditor -> state.canContinue
        state.page in setOf(
            AdditionalKycCollectionPage.Context,
            AdditionalKycCollectionPage.Questionnaire,
        ) && !isQuestionnaireOnly -> state.canContinue
        else -> state.canSubmit
    }
    val action = when {
        isUnavailable || isPending -> onClose
        isSubmitted -> onContinue
        isSourceEditor || state.page in setOf(
            AdditionalKycCollectionPage.Context,
            AdditionalKycCollectionPage.Questionnaire,
        ) && !isQuestionnaireOnly -> onContinue
        else -> onSubmit
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        PrimaryButton(
            label = label,
            state = when {
                state.submissionState == AdditionalKycSubmissionState.Submitting ->
                    PrimaryButtonState.Processing
                enabled -> PrimaryButtonState.Enabled
                else -> PrimaryButtonState.Disabled
            },
            onButtonClick = action,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ADDITIONAL_KYC_SUBMIT_BUTTON_TAG),
        )
    }
}

@Composable
private fun validationErrorMessage(
    error: AdditionalKycValidationError?,
    maxFileSizeMegabytes: Int?,
): String {
    return when (error) {
        AdditionalKycValidationError.UnsupportedFileType ->
            stringResource(R.string.stripe_link_onramp_additional_kyc_unsupported_file_type_design)
        AdditionalKycValidationError.FileTooLarge -> stringResource(
            R.string.stripe_link_onramp_additional_kyc_file_too_large_design,
            maxFileSizeMegabytes ?: DEFAULT_MAX_FILE_SIZE_MEGABYTES,
        )
        else -> stringResource(R.string.stripe_link_onramp_additional_kyc_upload_failed)
    }
}

private fun Modifier.dashedBorder(color: androidx.compose.ui.graphics.Color): Modifier = drawBehind {
    val strokeWidth = 1.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
        size = Size(size.width - strokeWidth, size.height - strokeWidth),
        cornerRadius = CornerRadius(10.dp.toPx()),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
            ),
        ),
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AdditionalKycScreenState(
    val page: AdditionalKycCollectionPage,
    val requirementType: AdditionalKycRequirementType,
    val errorMessages: List<String>,
    val questions: List<AdditionalKycQuestionState>,
    val document: AdditionalKycDocumentState?,
    val validationError: AdditionalKycValidationError?,
    val validationFileName: String?,
    val selectingFileSlot: Int?,
    val selectingFileName: String?,
    val canSubmit: Boolean,
    val canContinue: Boolean,
    val isCollectionAvailable: Boolean,
    val submissionState: AdditionalKycSubmissionState,
    val currentRequirement: Int,
    val totalRequirements: Int,
    val hasMoreRequirements: Boolean,
    val pendingRequirements: List<AdditionalKycPendingRequirementState>,
    val completedDocumentCount: Int,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class AdditionalKycCollectionPage {
    Context,
    Questionnaire,
    DocumentOverview,
    DocumentEditor,
    Submitted,
    Pending,
    Unavailable,
}

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
    val minDocuments: Int,
    val maxDocuments: Int,
    val editingSlotIndex: Int?,
    val slots: List<AdditionalKycDocumentSlotState>,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AdditionalKycDocumentSlotState(
    val index: Int,
    val subtypes: List<AdditionalKycDocumentSubtypeState>,
    val selectedSubtypeId: String?,
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

internal fun additionalKycDocumentGroupTag(slotIndex: Int): String =
    "AdditionalKycDocumentGroup-$slotIndex"

internal fun additionalKycPendingRequirementTag(index: Int): String =
    "AdditionalKycPendingRequirement-$index"

private const val FUNDING_SOURCES_QUESTION_ID = "funding_sources"
private const val DEFAULT_MAX_FILE_SIZE_MEGABYTES = 5
internal const val ADDITIONAL_KYC_CANCEL_BUTTON_TAG = "AdditionalKycCancelButton"
internal const val ADDITIONAL_KYC_BACK_BUTTON_TAG = "AdditionalKycBackButton"
internal const val ADDITIONAL_KYC_SELECTOR_CLOSE_TAG = "AdditionalKycSelectorClose"
internal const val ADDITIONAL_KYC_ADD_DOCUMENTS_TAG = "AdditionalKycAddDocuments"
internal const val ADDITIONAL_KYC_SUBMIT_BUTTON_TAG = "AdditionalKycSubmitButton"
internal const val ADDITIONAL_KYC_VALIDATION_ERROR_TAG = "AdditionalKycValidationError"
internal const val ADDITIONAL_KYC_SUBMISSION_ERROR_TAG = "AdditionalKycSubmissionError"
internal const val ADDITIONAL_KYC_SUBMITTED_TITLE_TAG = "AdditionalKycSubmittedTitle"
