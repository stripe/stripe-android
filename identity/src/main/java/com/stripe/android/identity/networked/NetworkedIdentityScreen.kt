package com.stripe.android.identity.networked

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.identity.R
import com.stripe.android.link.ui.LinkHostedSheet
import com.stripe.android.uicore.utils.collectAsState

/** Shows Identity's steps of a Networked Identity attempt in Link's own sheet. */
@Composable
internal fun NetworkedIdentitySheet(viewModel: NetworkedIdentityViewModel) {
    val state by viewModel.state.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val isSheetVisible by viewModel.isSheetVisible.collectAsState()
    val actions = remember(viewModel) { viewModel.screenActions() }
    // Closing the sheet, or tapping outside it, cancels the attempt.
    LinkHostedSheet(visible = isSheetVisible, onClose = viewModel::cancel) {
        NetworkedIdentityScreen(state = state, mode = mode, actions = actions)
    }
}

/** The content of Identity's Link sheet for Networked Identity. */
@Composable
@Suppress("LongMethod")
internal fun NetworkedIdentityScreen(
    state: NetworkedIdentityState,
    mode: NetworkedIdentityMode,
    actions: NetworkedIdentityScreenActions,
) {
    BackHandler(enabled = state.isInProgress, onBack = actions.onCancel)
    if (!state.isInProgress) return

    val title = stringResource(state.title)
    // Link's sheet draws the surface, the header and the horizontal padding around this content.
    Surface(color = Color.Transparent, contentColor = MaterialTheme.colors.onSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .semantics { paneTitle = title }
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NI_TITLE_TAG)
                        .semantics { heading() },
                )
                Spacer(Modifier.height(24.dp))
                NetworkedIdentityBody(state = state, actions = actions)
            }
            NetworkedIdentityActions(state = state, mode = mode, actions = actions)
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun NetworkedIdentityBody(
    state: NetworkedIdentityState,
    actions: NetworkedIdentityScreenActions,
) {
    when (state) {
        NetworkedIdentityState.DocumentsPending -> {
            NetworkedIdentityLoading(stringResource(R.string.stripe_identity_link_documents_loading))
        }
        is NetworkedIdentityState.SelectDocument -> {
            BodyText(stringResource(R.string.stripe_identity_link_documents_body))
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().selectableGroup()) {
                state.documents.forEach { document ->
                    NetworkedIdentityDocumentRow(
                        document = document,
                        selected = document.id == state.selectedDocumentId,
                        onClick = { actions.onSelectDocument(document.id) },
                    )
                }
            }
        }
        is NetworkedIdentityState.SharingDocument -> {
            NetworkedIdentityLoading(stringResource(R.string.stripe_identity_link_sharing))
        }
        is NetworkedIdentityState.DocumentShared -> {
            BodyText(stringResource(R.string.stripe_identity_link_shared_body))
            Spacer(Modifier.height(16.dp))
            NetworkedIdentityDocumentRow(document = state.document, selected = true, onClick = {})
        }
        NetworkedIdentityState.SavePending -> {
            NetworkedIdentityLoading(stringResource(R.string.stripe_identity_link_saving))
        }
        NetworkedIdentityState.SavePrepared -> {
            BodyText(stringResource(R.string.stripe_identity_link_saved_body))
        }
        NetworkedIdentityState.SkipPending -> {
            NetworkedIdentityLoading(stringResource(R.string.stripe_identity_link_skipping))
        }
        is NetworkedIdentityState.SaveFailed -> {
            BodyText(stringResource(R.string.stripe_identity_link_save_failed_body))
            state.details?.let { details ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = details,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag(NI_ERROR_TAG),
                )
            }
        }
        NetworkedIdentityState.Idle,
        NetworkedIdentityState.Preparing,
        NetworkedIdentityState.LinkAuthentication,
        NetworkedIdentityState.FullCaptureFallback,
        NetworkedIdentityState.Cancelled -> Unit
    }
}

@Composable
private fun NetworkedIdentityDocumentRow(
    document: NetworkedIdentityDocument,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val inspection = LocalInspectionMode.current
    LaunchedEffect(selected) {
        if (selected && !inspection) focusRequester.requestFocus()
    }
    val type = stringResource(
        when (document.documentType) {
            NetworkedIdentityDocumentType.PASSPORT -> R.string.stripe_passport
            NetworkedIdentityDocumentType.DRIVING_LICENSE -> R.string.stripe_driver_license
            NetworkedIdentityDocumentType.ID_CARD -> R.string.stripe_id_card
            NetworkedIdentityDocumentType.UNKNOWN -> R.string.stripe_identity_link_documents_title
        }
    )
    val label = document.redactedDocumentNumber?.let {
        stringResource(R.string.stripe_identity_link_document_number, type, it)
    } ?: type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = if (selected) 1f else 0.15f),
                shape = RoundedCornerShape(12.dp),
            )
            .focusRequester(focusRequester)
            // Selectable normally refuses input focus in touch mode. The selected ID also needs
            // to receive programmatic focus when selection changes.
            .focusProperties { canFocus = true }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(NI_DOCUMENT_TAG_PREFIX + document.id),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Icon(
            painter = painterResource(if (selected) R.drawable.stripe_check_mark else R.drawable.stripe_document_icon),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun NetworkedIdentityActions(
    state: NetworkedIdentityState,
    mode: NetworkedIdentityMode,
    actions: NetworkedIdentityScreenActions,
) {
    when {
        state is NetworkedIdentityState.SelectDocument -> {
            PrimaryButton(
                text = stringResource(R.string.stripe_identity_link_share),
                enabled = state.selectedDocumentId != null,
                testTag = NI_SHARE_TAG,
                onClick = actions.onShareDocument,
            )
        }
        state is NetworkedIdentityState.SaveFailed -> {
            PrimaryButton(
                text = stringResource(R.string.stripe_identity_link_close),
                enabled = true,
                testTag = NI_CONTINUE_TAG,
                onClick = actions.onCancel,
            )
        }
        state is NetworkedIdentityState.DocumentShared || state == NetworkedIdentityState.SavePrepared -> {
            PrimaryButton(
                text = stringResource(
                    if (state == NetworkedIdentityState.SavePrepared) {
                        R.string.stripe_identity_link_done
                    } else {
                        R.string.stripe_identity_link_continue
                    }
                ),
                enabled = true,
                testTag = NI_CONTINUE_TAG,
                onClick = actions.onContinue,
            )
        }
    }
    if (mode == NetworkedIdentityMode.Reuse && state.offersManualCapture) {
        TextButton(
            onClick = actions.onManualCapture,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                contentColor = MaterialTheme.colors.onSurface,
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(NI_MANUAL_TAG),
        ) {
            Text(
                stringResource(R.string.stripe_identity_link_manual),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.onSurface,
            contentColor = MaterialTheme.colors.surface,
        ),
        elevation = null,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(testTag),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun NetworkedIdentityLoading(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp).testTag(NI_LOADING_TAG),
            color = MaterialTheme.colors.onSurface,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.height(16.dp))
        BodyText(label)
    }
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        style = MaterialTheme.typography.body1,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().testTag(NI_BODY_TAG),
    )
}

private val NetworkedIdentityState.offersManualCapture: Boolean
    get() = this !is NetworkedIdentityState.SharingDocument && this !is NetworkedIdentityState.DocumentShared &&
        this != NetworkedIdentityState.SkipPending

private val NetworkedIdentityState.title: Int
    get() = when (this) {
        NetworkedIdentityState.SkipPending -> R.string.stripe_identity_link_manual
        is NetworkedIdentityState.DocumentShared -> R.string.stripe_identity_link_shared_title
        NetworkedIdentityState.SavePending,
        NetworkedIdentityState.SavePrepared -> R.string.stripe_identity_link_saved_title
        is NetworkedIdentityState.SaveFailed -> R.string.stripe_identity_link_save_failed_title
        else -> R.string.stripe_identity_link_documents_title
    }

internal const val NI_TITLE_TAG = "NetworkedIdentityTitle"
internal const val NI_BODY_TAG = "NetworkedIdentityBody"
internal const val NI_ERROR_TAG = "NetworkedIdentityError"
internal const val NI_CONTINUE_TAG = "NetworkedIdentityContinue"
internal const val NI_SHARE_TAG = "NetworkedIdentityShare"
internal const val NI_MANUAL_TAG = "NetworkedIdentityManual"
internal const val NI_LOADING_TAG = "NetworkedIdentityLoading"
internal const val NI_DOCUMENT_TAG_PREFIX = "NetworkedIdentityDocument_"
