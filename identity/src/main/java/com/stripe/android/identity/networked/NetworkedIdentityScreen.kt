package com.stripe.android.identity.networked

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.identity.R
import com.stripe.android.uicore.LocalColors
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementColors
import com.stripe.android.uicore.elements.OTPElementUI
import com.stripe.android.uicore.elements.TextField
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState

/**
 * Standalone reuse UI. The owner retains the coordinator across configuration changes and explicitly
 * abandons it on permanent dismissal. Composition disposal is not a flow cancellation signal.
 * No input, OTP, or credentials are placed in saved instance state.
 */
@Composable
internal fun NetworkedIdentityScreen(
    state: NetworkedIdentityState,
    onSubmitEmail: (String) -> Unit,
    onSubmitOtp: (String) -> Unit,
    onSelectDocument: (String) -> Unit,
    onManualCapture: () -> Unit,
    onCancel: () -> Unit,
) {
    val emailController = remember { EmailConfig.createController(initialValue = null) }
    LaunchedEffect(state) {
        if (state is NetworkedIdentityState.ReauthenticationRequired || state.isTerminal) {
            emailController.onValueChange("")
        }
    }
    NetworkedIdentityScreenContent(
        state = state,
        emailController = emailController,
        onSubmitEmail = onSubmitEmail,
        onSubmitOtp = onSubmitOtp,
        onSelectDocument = onSelectDocument,
        onManualCapture = onManualCapture,
        onCancel = onCancel,
    )
}

/** The email controller is injectable for rendering the same UI in visual tests. */
@Composable
@Suppress("LongMethod", "NestedBlockDepth")
internal fun NetworkedIdentityScreenContent(
    state: NetworkedIdentityState,
    emailController: TextFieldController,
    onSubmitEmail: (String) -> Unit,
    onSubmitOtp: (String) -> Unit,
    onSelectDocument: (String) -> Unit,
    onManualCapture: () -> Unit,
    onCancel: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val dismiss = {
        focusManager.clearFocus(force = true)
        onCancel()
    }
    BackHandler(enabled = !state.isTerminal, onBack = dismiss)
    if (state.isTerminal) return

    val title = stringResource(state.title)
    val neutral = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
    CompositionLocalProvider(
        LocalColors provides MaterialTheme.stripeColors.copy(
            component = neutral,
            componentBorder = Color.Transparent,
            textCursor = MaterialTheme.colors.onSurface,
        )
    ) {
        Surface(color = MaterialTheme.colors.surface) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { paneTitle = title }
            ) {
                NetworkedIdentityHeader(onCancel = dismiss)
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight)
                            .padding(vertical = 24.dp),
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
                        NetworkedIdentityBody(
                            state = state,
                            emailController = emailController,
                            onSubmitOtp = onSubmitOtp,
                            onSelectDocument = onSelectDocument,
                        )
                    }
                }
                NetworkedIdentityActions(
                    state = state,
                    emailController = emailController,
                    onSubmitEmail = {
                        focusManager.clearFocus(force = true)
                        onSubmitEmail(it)
                    },
                    onManualCapture = {
                        focusManager.clearFocus(force = true)
                        onManualCapture()
                    },
                )
            }
        }
    }
}

@Composable
private fun NetworkedIdentityHeader(onCancel: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.stripe_identity_link_logo),
            contentDescription = stringResource(R.string.stripe_identity_link),
            modifier = Modifier.width(72.dp).height(24.dp),
        )
        IconButton(onClick = onCancel, modifier = Modifier.size(48.dp).testTag(NI_CLOSE_TAG)) {
            Icon(
                painter = painterResource(R.drawable.stripe_close),
                contentDescription = stringResource(R.string.stripe_description_close),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                    .padding(6.dp)
                    .size(20.dp),
            )
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun NetworkedIdentityBody(
    state: NetworkedIdentityState,
    emailController: TextFieldController,
    onSubmitOtp: (String) -> Unit,
    onSelectDocument: (String) -> Unit,
) {
    when (state) {
        NetworkedIdentityState.CollectEmail,
        NetworkedIdentityState.LookupPending,
        NetworkedIdentityState.ReauthenticationRequired -> {
            BodyText(
                stringResource(
                    if (state is NetworkedIdentityState.ReauthenticationRequired) {
                        R.string.stripe_identity_link_reauth_body
                    } else {
                        R.string.stripe_identity_link_email_body
                    }
                )
            )
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.stripeColors.component) {
                TextField(
                    textFieldController = emailController,
                    enabled = state !is NetworkedIdentityState.LookupPending,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth().testTag(NI_EMAIL_TAG),
                )
            }
        }
        NetworkedIdentityState.OtpStartPending -> {
            NetworkedIdentityLoading(stringResource(R.string.stripe_identity_link_sending))
        }
        is NetworkedIdentityState.AwaitingOtp,
        is NetworkedIdentityState.OtpConfirmPending -> {
            val awaiting = state as? NetworkedIdentityState.AwaitingOtp
            val confirming = state as? NetworkedIdentityState.OtpConfirmPending
            // Keep one call site so the entered value survives pending/invalid transitions.
            NetworkedIdentityOtp(
                redactedPhoneNumber = awaiting?.redactedPhoneNumber ?: requireNotNull(confirming).redactedPhoneNumber,
                otpGeneration = awaiting?.otpGeneration ?: requireNotNull(confirming).otpGeneration,
                invalidCode = awaiting?.invalidCode == true,
                submitting = confirming != null,
                onSubmitOtp = onSubmitOtp,
            )
        }
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
                        onClick = { onSelectDocument(document.id) },
                    )
                }
            }
        }
        is NetworkedIdentityState.FullCaptureFallback,
        NetworkedIdentityState.Cancelled -> Unit
    }
}

@Composable
@Suppress("LongMethod")
private fun NetworkedIdentityOtp(
    redactedPhoneNumber: String,
    otpGeneration: Int,
    invalidCode: Boolean,
    submitting: Boolean,
    onSubmitOtp: (String) -> Unit,
) {
    val element = remember(otpGeneration) {
        OTPElement(FormFieldId.Generic("networked_identity_otp"), OTPController(otpLength = 6))
    }
    val otp by element.controller.fieldValue.collectAsState()
    val currentOnSubmitOtp by rememberUpdatedState(onSubmitOtp)
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val inspection = LocalInspectionMode.current

    LaunchedEffect(otpGeneration, invalidCode) {
        if (invalidCode) element.controller.reset()
        if (!inspection && !submitting) focusRequester.requestFocus()
    }
    // Submission is driven by a completed value changing, so returning an invalid-code response
    // cannot resubmit the old code. The coordinator also rejects duplicate pending requests.
    LaunchedEffect(otp) {
        if (otp.length == element.controller.otpLength && !submitting) {
            focusManager.clearFocus(force = true)
            currentOnSubmitOtp(otp)
        }
    }
    BodyText(stringResource(R.string.stripe_identity_link_otp_body, redactedPhoneNumber))
    Spacer(Modifier.height(32.dp))
    OTPElementUI(
        enabled = !submitting,
        element = element,
        modifier = Modifier.testTag(NI_OTP_TAG),
        boxShape = RoundedCornerShape(12.dp),
        boxSpacing = 6.dp,
        middleSpacing = 6.dp,
        otpInputPlaceholder = "",
        colors = OTPElementColors(
            selectedBorder = MaterialTheme.colors.onSurface,
            unselectedBorder = Color.Transparent,
            placeholder = MaterialTheme.stripeColors.placeholderText,
            background = Color.Transparent,
            selectedBackground = Color.Transparent,
        ),
        focusRequester = focusRequester,
    )
    if (invalidCode) {
        Text(
            text = stringResource(R.string.stripe_identity_link_otp_error),
            color = if (MaterialTheme.colors.isLight) lightErrorColor else darkErrorColor,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag(NI_ERROR_TAG)
                .semantics { liveRegion = LiveRegionMode.Assertive },
        )
    }
    if (submitting) {
        Spacer(Modifier.height(16.dp))
        NetworkedIdentityLoading(stringResource(R.string.stripe_identity_link_confirming))
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
private fun NetworkedIdentityActions(
    state: NetworkedIdentityState,
    emailController: TextFieldController,
    onSubmitEmail: (String) -> Unit,
    onManualCapture: () -> Unit,
) {
    if (state.isEmail) {
        val email by emailController.fieldValue.collectAsState()
        val complete by emailController.isComplete.collectAsState()
        Button(
            onClick = { onSubmitEmail(email) },
            enabled = complete && state !is NetworkedIdentityState.LookupPending,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.onSurface,
                contentColor = MaterialTheme.colors.surface,
            ),
            elevation = null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(NI_CONTINUE_TAG),
        ) {
            if (state is NetworkedIdentityState.LookupPending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp).testTag(NI_LOADING_TAG),
                    color = MaterialTheme.colors.onSurface,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.stripe_identity_link_continue), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
    }
    val plain = state is NetworkedIdentityState.AwaitingOtp ||
        state is NetworkedIdentityState.OtpConfirmPending || state is NetworkedIdentityState.OtpStartPending
    TextButton(
        onClick = onManualCapture,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = if (plain) Color.Transparent else MaterialTheme.stripeColors.component,
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
    // #TODO - Networked Identity: add progression only after the clone/attach endpoint, auth,
    // association-token lifetime and Identity submission contract are defined. Selection is not success.
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

private val NetworkedIdentityState.isTerminal: Boolean
    get() = this is NetworkedIdentityState.FullCaptureFallback || this is NetworkedIdentityState.Cancelled

private val NetworkedIdentityState.isEmail: Boolean
    get() = this is NetworkedIdentityState.CollectEmail || this is NetworkedIdentityState.LookupPending ||
        this is NetworkedIdentityState.ReauthenticationRequired

private val NetworkedIdentityState.title: Int
    get() = when (this) {
        NetworkedIdentityState.CollectEmail,
        NetworkedIdentityState.LookupPending -> R.string.stripe_identity_link_email_title
        NetworkedIdentityState.ReauthenticationRequired -> R.string.stripe_identity_link_reauth_title
        is NetworkedIdentityState.AwaitingOtp,
        is NetworkedIdentityState.OtpConfirmPending,
        NetworkedIdentityState.OtpStartPending -> R.string.stripe_identity_link_otp_title
        else -> R.string.stripe_identity_link_documents_title
    }

private val lightErrorColor = Color(0xFFB3261E)
private val darkErrorColor = Color(0xFFFFB4AB)

internal const val NI_TITLE_TAG = "NetworkedIdentityTitle"
internal const val NI_BODY_TAG = "NetworkedIdentityBody"
internal const val NI_EMAIL_TAG = "NetworkedIdentityEmail"
internal const val NI_OTP_TAG = "NetworkedIdentityOtp"
internal const val NI_ERROR_TAG = "NetworkedIdentityError"
internal const val NI_CONTINUE_TAG = "NetworkedIdentityContinue"
internal const val NI_MANUAL_TAG = "NetworkedIdentityManual"
internal const val NI_CLOSE_TAG = "NetworkedIdentityClose"
internal const val NI_LOADING_TAG = "NetworkedIdentityLoading"
internal const val NI_DOCUMENT_TAG_PREFIX = "NetworkedIdentityDocument_"
