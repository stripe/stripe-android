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
import com.stripe.android.uicore.elements.PhoneNumberCollectionSection
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.TextField
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState

/**
 * Link sheet UI for Networked Identity. The owner retains the coordinator across configuration changes
 * and explicitly abandons it on permanent dismissal. Composition disposal is not a flow cancellation
 * signal. No input, OTP, or credentials are placed in saved instance state.
 */
@Composable
internal fun NetworkedIdentityScreen(
    state: NetworkedIdentityState,
    mode: NetworkedIdentityMode,
    savedItems: List<String>,
    actions: NetworkedIdentityScreenActions,
) {
    val emailController = remember { EmailConfig.createController(initialValue = null) }
    val phoneController = remember { PhoneNumberController.createPhoneNumberController() }
    LaunchedEffect(state) {
        if (state is NetworkedIdentityState.ReauthenticationRequired || !state.isSheetVisible) {
            emailController.onValueChange("")
        }
    }
    NetworkedIdentityScreenContent(
        state = state,
        mode = mode,
        savedItems = savedItems,
        emailController = emailController,
        phoneController = phoneController,
        actions = actions,
    )
}

/** The input controllers are injectable for rendering the same UI in visual tests. */
@Composable
@Suppress("LongMethod", "NestedBlockDepth")
internal fun NetworkedIdentityScreenContent(
    state: NetworkedIdentityState,
    mode: NetworkedIdentityMode,
    savedItems: List<String>,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    actions: NetworkedIdentityScreenActions,
) {
    val focusManager = LocalFocusManager.current
    val dismiss = {
        focusManager.clearFocus(force = true)
        actions.onCancel()
    }
    BackHandler(enabled = state.isSheetVisible, onBack = dismiss)
    if (!state.isSheetVisible) return

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
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { paneTitle = title }
            ) {
                NetworkedIdentityHeader(onCancel = dismiss)
                BoxWithConstraints(modifier = Modifier.weight(1f, fill = false)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
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
                            mode = mode,
                            savedItems = savedItems,
                            emailController = emailController,
                            phoneController = phoneController,
                            actions = actions,
                        )
                    }
                }
                NetworkedIdentityActions(
                    state = state,
                    mode = mode,
                    emailController = emailController,
                    phoneController = phoneController,
                    actions = actions.copy(
                        onSubmitEmail = {
                            focusManager.clearFocus(force = true)
                            actions.onSubmitEmail(it)
                        },
                        onSubmitPhone = { phoneNumber, country ->
                            focusManager.clearFocus(force = true)
                            actions.onSubmitPhone(phoneNumber, country)
                        },
                        onManualCapture = {
                            focusManager.clearFocus(force = true)
                            actions.onManualCapture()
                        },
                    ),
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
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun NetworkedIdentityBody(
    state: NetworkedIdentityState,
    mode: NetworkedIdentityMode,
    savedItems: List<String>,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    actions: NetworkedIdentityScreenActions,
) {
    when (state) {
        NetworkedIdentityState.CollectEmail,
        NetworkedIdentityState.LookupPending,
        NetworkedIdentityState.ReauthenticationRequired -> {
            BodyText(
                stringResource(
                    when {
                        state is NetworkedIdentityState.ReauthenticationRequired ->
                            R.string.stripe_identity_link_reauth_body
                        mode == NetworkedIdentityMode.Save -> R.string.stripe_identity_link_save_body
                        else -> R.string.stripe_identity_link_email_body
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
        is NetworkedIdentityState.CollectPhone,
        is NetworkedIdentityState.SignUpPending -> {
            val email = (state as? NetworkedIdentityState.CollectPhone)?.email
                ?: (state as NetworkedIdentityState.SignUpPending).email
            BodyText(stringResource(R.string.stripe_identity_link_save_body))
            Spacer(Modifier.height(16.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.fillMaxWidth().testTag(NI_PHONE_EMAIL_TAG),
            )
            Spacer(Modifier.height(12.dp))
            PhoneNumberCollectionSection(
                enabled = state is NetworkedIdentityState.CollectPhone,
                phoneNumberController = phoneController,
                modifier = Modifier.testTag(NI_PHONE_TAG),
            )
            (state as? NetworkedIdentityState.CollectPhone)?.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.fillMaxWidth().testTag(NI_ERROR_TAG),
                )
            }
        }
        NetworkedIdentityState.Preparing -> {
            NetworkedIdentityLoading(stringResource(R.string.stripe_identity_link_preparing))
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
                onSubmitOtp = actions.onSubmitOtp,
                onResendOtp = actions.onResendOtp,
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
        NetworkedIdentityState.Saved -> {
            BodyText(stringResource(R.string.stripe_identity_link_saved_body))
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth()) {
                savedItems.forEach { item -> NetworkedIdentitySavedItemRow(item) }
            }
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
    onResendOtp: () -> Unit,
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
    } else {
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onResendOtp, modifier = Modifier.testTag(NI_RESEND_TAG)) {
            Text(
                text = stringResource(R.string.stripe_identity_link_resend),
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
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
private fun NetworkedIdentitySavedItemRow(item: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(NI_SAVED_ITEM_TAG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.stripe_check_mark),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = item, style = MaterialTheme.typography.body1)
    }
}

@Composable
@Suppress("LongMethod")
private fun NetworkedIdentityActions(
    state: NetworkedIdentityState,
    mode: NetworkedIdentityMode,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    actions: NetworkedIdentityScreenActions,
) {
    when {
        state.isEmail -> {
            val email by emailController.fieldValue.collectAsState()
            val complete by emailController.isComplete.collectAsState()
            PrimaryButton(
                text = stringResource(R.string.stripe_identity_link_continue),
                enabled = complete && state !is NetworkedIdentityState.LookupPending,
                loading = state is NetworkedIdentityState.LookupPending,
                testTag = NI_CONTINUE_TAG,
                onClick = { actions.onSubmitEmail(email) },
            )
        }
        state is NetworkedIdentityState.CollectPhone || state is NetworkedIdentityState.SignUpPending -> {
            val complete by phoneController.isComplete.collectAsState()
            PrimaryButton(
                text = stringResource(R.string.stripe_identity_link_continue),
                enabled = complete && state is NetworkedIdentityState.CollectPhone,
                loading = state is NetworkedIdentityState.SignUpPending,
                testTag = NI_CONTINUE_TAG,
                onClick = {
                    actions.onSubmitPhone(phoneController.rawFieldValue.value, phoneController.getCountryCode())
                },
            )
        }
        state is NetworkedIdentityState.SelectDocument -> {
            PrimaryButton(
                text = stringResource(R.string.stripe_identity_link_share),
                enabled = state.selectedDocumentId != null,
                loading = false,
                testTag = NI_SHARE_TAG,
                onClick = actions.onShareDocument,
            )
        }
        state is NetworkedIdentityState.SaveFailed -> {
            PrimaryButton(
                text = stringResource(R.string.stripe_identity_link_close),
                enabled = true,
                loading = false,
                testTag = NI_CONTINUE_TAG,
                onClick = actions.onCancel,
            )
        }
        state is NetworkedIdentityState.DocumentShared || state == NetworkedIdentityState.Saved -> {
            PrimaryButton(
                text = stringResource(R.string.stripe_identity_link_continue),
                enabled = true,
                loading = false,
                testTag = NI_CONTINUE_TAG,
                onClick = actions.onContinue,
            )
        }
    }
    if (mode == NetworkedIdentityMode.Reuse && state.offersManualCapture) {
        val plain = state is NetworkedIdentityState.AwaitingOtp ||
            state is NetworkedIdentityState.OtpConfirmPending || state is NetworkedIdentityState.OtpStartPending
        TextButton(
            onClick = actions.onManualCapture,
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
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
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
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).testTag(NI_LOADING_TAG),
                color = MaterialTheme.colors.onSurface,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
        }
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

private val NetworkedIdentityState.isEmail: Boolean
    get() = this is NetworkedIdentityState.CollectEmail || this is NetworkedIdentityState.LookupPending ||
        this is NetworkedIdentityState.ReauthenticationRequired

private val NetworkedIdentityState.offersManualCapture: Boolean
    get() = this !is NetworkedIdentityState.SharingDocument && this !is NetworkedIdentityState.DocumentShared

private val NetworkedIdentityState.title: Int
    get() = when (this) {
        NetworkedIdentityState.Preparing,
        NetworkedIdentityState.CollectEmail,
        NetworkedIdentityState.LookupPending,
        is NetworkedIdentityState.CollectPhone,
        is NetworkedIdentityState.SignUpPending -> R.string.stripe_identity_link_email_title
        NetworkedIdentityState.ReauthenticationRequired -> R.string.stripe_identity_link_reauth_title
        is NetworkedIdentityState.AwaitingOtp,
        is NetworkedIdentityState.OtpConfirmPending,
        NetworkedIdentityState.OtpStartPending -> R.string.stripe_identity_link_otp_title
        is NetworkedIdentityState.DocumentShared -> R.string.stripe_identity_link_shared_title
        NetworkedIdentityState.SavePending,
        NetworkedIdentityState.Saved -> R.string.stripe_identity_link_saved_title
        is NetworkedIdentityState.SaveFailed -> R.string.stripe_identity_link_save_failed_title
        else -> R.string.stripe_identity_link_documents_title
    }

private val lightErrorColor = Color(0xFFB3261E)
private val darkErrorColor = Color(0xFFFFB4AB)

internal const val NI_TITLE_TAG = "NetworkedIdentityTitle"
internal const val NI_BODY_TAG = "NetworkedIdentityBody"
internal const val NI_EMAIL_TAG = "NetworkedIdentityEmail"
internal const val NI_PHONE_TAG = "NetworkedIdentityPhone"
internal const val NI_PHONE_EMAIL_TAG = "NetworkedIdentityPhoneEmail"
internal const val NI_OTP_TAG = "NetworkedIdentityOtp"
internal const val NI_RESEND_TAG = "NetworkedIdentityResend"
internal const val NI_ERROR_TAG = "NetworkedIdentityError"
internal const val NI_CONTINUE_TAG = "NetworkedIdentityContinue"
internal const val NI_SHARE_TAG = "NetworkedIdentityShare"
internal const val NI_MANUAL_TAG = "NetworkedIdentityManual"
internal const val NI_CLOSE_TAG = "NetworkedIdentityClose"
internal const val NI_LOADING_TAG = "NetworkedIdentityLoading"
internal const val NI_SAVED_ITEM_TAG = "NetworkedIdentitySavedItem"
internal const val NI_DOCUMENT_TAG_PREFIX = "NetworkedIdentityDocument_"
