package com.stripe.android.link.ui.signup

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.LinkSpinner
import com.stripe.android.link.ui.LinkTerms
import com.stripe.android.link.ui.LinkTermsType
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ProgressIndicatorTestTag
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.utils.LINK_DEFAULT_ANIMATION_DELAY_MILLIS
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.SectionStyle
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberCollectionSection
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.TextField
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldSection
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.delay
import com.stripe.android.ui.core.R as PaymentsUiCoreR

@Composable
internal fun SignUpScreen(
    viewModel: SignUpViewModel,
) {
    val signUpScreenState by viewModel.state.collectAsState()

    SignUpBody(
        emailController = viewModel.emailController,
        phoneNumberController = viewModel.phoneNumberController,
        nameController = viewModel.nameController,
        signUpScreenState = signUpScreenState,
        onSignUpClick = viewModel::onSignUpClick,
        onSuggestedEmailClick = viewModel::onSuggestedEmailClick
    )
}

@Composable
internal fun SignUpBody(
    emailController: TextFieldController,
    phoneNumberController: PhoneNumberController,
    nameController: TextFieldController,
    signUpScreenState: SignUpScreenState,
    onSignUpClick: () -> Unit,
    onSuggestedEmailClick: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val isSigningUp = signUpScreenState.signUpState == SignUpState.InputtingRemainingFields

    var didFocusField by rememberSaveable { mutableStateOf(false) }
    val emailFocusRequester = remember { FocusRequester() }

    if (!didFocusField && signUpScreenState.signUpState == SignUpState.InputtingPrimaryField) {
        LaunchedEffect(Unit) {
            delay(LINK_DEFAULT_ANIMATION_DELAY_MILLIS)
            emailFocusRequester.requestFocus()
            didFocusField = true
        }
    }

    ScrollableTopLevelColumn {
        SignUpHeader()
        StripeThemeForLink(sectionStyle = SectionStyle.Bordered) {
            EmailCollectionSection(
                canEditForm = signUpScreenState.canEditForm,
                canEditEmail = signUpScreenState.canEditEmail,
                emailController = emailController,
                signUpState = signUpScreenState.signUpState,
                focusRequester = emailFocusRequester,
            )
        }
        AnimatedVisibility(
            visible = signUpScreenState.suggestedEmail != null &&
                signUpScreenState.signUpState != SignUpState.VerifyingEmail,
            modifier = Modifier.fillMaxWidth(),
        ) {
            EmailSuggestion(
                suggestedEmail = signUpScreenState.suggestedEmail.orEmpty(),
                onSuggestedEmailClick = onSuggestedEmailClick
            )
        }
        AnimatedVisibility(
            visible = !isSigningUp && signUpScreenState.errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ErrorText(
                text = signUpScreenState.errorMessage?.resolve(LocalContext.current).orEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SIGN_UP_ERROR_TAG)
            )
        }
        AnimatedVisibility(visible = isSigningUp) {
            SecondaryFields(
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                signUpScreenState = signUpScreenState,
            )
        }

        SignUpButton(
            isSigningUp = isSigningUp,
            signUpScreenState = signUpScreenState,
            onSignUpClick = onSignUpClick,
            keyboardController = keyboardController
        )
    }
}

@VisibleForTesting
@Composable
internal fun EmailCollectionSection(
    canEditForm: Boolean,
    canEditEmail: Boolean,
    emailController: TextFieldController,
    signUpState: SignUpState,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    var focused by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        TextFieldSection(
            textFieldController = emailController,
            isSelected = focused,
            modifier = Modifier
                .padding(vertical = 8.dp),
        ) {
            TextField(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused },
                textFieldController = emailController,
                imeAction = if (signUpState == SignUpState.InputtingRemainingFields) {
                    ImeAction.Next
                } else {
                    ImeAction.Done
                },
                enabled = canEditForm &&
                    canEditEmail &&
                    signUpState != SignUpState.VerifyingEmail,
            )
        }
        if (signUpState == SignUpState.VerifyingEmail) {
            Row {
                LinkSpinner(
                    filledColor = LinkTheme.colors.iconPrimary,
                    strokeWidth = 4.dp,
                    modifier = Modifier
                        .size(20.dp)
                        .semantics { testTag = ProgressIndicatorTestTag },
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun SecondaryFields(
    phoneNumberController: PhoneNumberController,
    nameController: TextFieldController,
    signUpScreenState: SignUpScreenState,
) {
    var emailFocused by rememberSaveable { mutableStateOf(false) }
    var nameFocused by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        StripeThemeForLink(sectionStyle = SectionStyle.Bordered) {
            PhoneNumberCollectionSection(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .onFocusChanged { emailFocused = it.isFocused },
                enabled = signUpScreenState.canEditForm,
                isSelected = emailFocused,
                phoneNumberController = phoneNumberController,
                requestFocusWhenShown = phoneNumberController.initialPhoneNumber.isEmpty(),
                imeAction = if (signUpScreenState.requiresNameCollection) {
                    ImeAction.Next
                } else {
                    ImeAction.Done
                }
            )

            if (signUpScreenState.requiresNameCollection) {
                TextFieldSection(
                    modifier = Modifier
                        .onFocusChanged { nameFocused = it.isFocused }
                        .padding(vertical = 8.dp),
                    isSelected = nameFocused,
                    textFieldController = nameController,
                ) {
                    TextField(
                        modifier = Modifier.padding(vertical = 4.dp),
                        textFieldController = nameController,
                        imeAction = ImeAction.Done,
                        enabled = signUpScreenState.canEditForm,
                    )
                }
            }

            LinkTerms(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                textAlign = TextAlign.Center,
                type = LinkTermsType.Full,
            )
        }
        AnimatedVisibility(visible = signUpScreenState.errorMessage != null) {
            ErrorText(
                text = signUpScreenState.errorMessage?.resolve(LocalContext.current).orEmpty(),
                modifier = Modifier
                    .testTag(SIGN_UP_ERROR_TAG)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ColumnScope.SignUpHeader() {
    Text(
        text = stringResource(R.string.stripe_link_sign_up_header_v2),
        modifier = Modifier
            .testTag(SIGN_UP_HEADER_TAG)
            .padding(vertical = 4.dp),
        textAlign = TextAlign.Center,
        style = LinkTheme.typography.title,
        color = LinkTheme.colors.textPrimary
    )
    Text(
        text = stringResource(R.string.stripe_link_sign_up_message_v2),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 30.dp),
        textAlign = TextAlign.Center,
        style = LinkTheme.typography.body,
        color = LinkTheme.colors.textTertiary
    )
}

@Composable
private fun SignUpButton(
    isSigningUp: Boolean,
    signUpScreenState: SignUpScreenState,
    onSignUpClick: () -> Unit,
    keyboardController: SoftwareKeyboardController?
) {
    PrimaryButton(
        modifier = Modifier.padding(vertical = 16.dp),
        label = if (isSigningUp) {
            stringResource(PaymentsUiCoreR.string.stripe_continue_button_label)
        } else {
            stringResource(R.string.stripe_link_log_in_or_sign_up)
        },
        state = when {
            signUpScreenState.isSubmitting -> PrimaryButtonState.Processing
            signUpScreenState.signUpEnabled -> PrimaryButtonState.Enabled
            else -> PrimaryButtonState.Disabled
        },
        onButtonClick = {
            onSignUpClick()
            keyboardController?.hide()
        }
    )
}

@Composable
private fun EmailSuggestion(
    suggestedEmail: String,
    onSuggestedEmailClick: (String) -> Unit
) {
    val updateText = stringResource(R.string.stripe_link_email_suggestion_update)
    val fullText = stringResource(R.string.stripe_link_email_suggestion, suggestedEmail, updateText)

    val annotatedString = buildEmailSuggestionAnnotatedString(
        fullText = fullText,
        updateText = updateText,
        onSuggestedEmailClick = { onSuggestedEmailClick(suggestedEmail) }
    )

    Text(
        text = annotatedString,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("emailSuggestionUpdateTag"),
        style = LinkTheme.typography.detail.copy(textAlign = TextAlign.Center)
    )
}

@Composable
private fun buildEmailSuggestionAnnotatedString(
    fullText: String,
    updateText: String,
    onSuggestedEmailClick: () -> Unit
) = buildAnnotatedString {
    val updateStartIndex = fullText.indexOf(updateText)
    val baseStyle = detailSpanStyle(LinkTheme.colors.textSecondary)
    val clickableStyle = detailSpanStyle(LinkTheme.colors.textBrand)

    if (updateStartIndex >= 0) {
        withStyle(baseStyle) {
            append(fullText.substring(0, updateStartIndex))
        }

        withLink(
            LinkAnnotation.Clickable(
                tag = "update",
                linkInteractionListener = { onSuggestedEmailClick() }
            )
        ) {
            withStyle(clickableStyle) {
                append(updateText)
            }
        }

        withStyle(baseStyle) {
            append(fullText.substring(updateStartIndex + updateText.length))
        }
    } else {
        withStyle(baseStyle) {
            append(fullText)
        }
    }
}

@Composable
private fun detailSpanStyle(color: androidx.compose.ui.graphics.Color) = SpanStyle(
    color = color,
    fontSize = LinkTheme.typography.detail.fontSize,
    fontFamily = LinkTheme.typography.detail.fontFamily,
    fontWeight = LinkTheme.typography.detail.fontWeight
)

internal const val SIGN_UP_HEADER_TAG = "signUpHeaderTag"
internal const val SIGN_UP_ERROR_TAG = "signUpErrorTag"

@Preview
@Composable
private fun SignUpScreenLoadingPreview() {
    DefaultLinkTheme {
        SignUpBody(
            emailController = EmailConfig.createController("email@email.com"),
            phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
            nameController = NameConfig.createController("My Name"),
            signUpScreenState = SignUpScreenState(
                merchantName = "Example, Inc.",
                signUpEnabled = false,
                signUpState = SignUpState.VerifyingEmail,
                requiresNameCollection = true,
                canEditEmail = true,
            ),
            onSignUpClick = {},
            onSuggestedEmailClick = {}
        )
    }
}

@Preview
@Composable
private fun SignUpScreenPreview() {
    DefaultLinkTheme {
        SignUpBody(
            emailController = EmailConfig.createController("email"),
            phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
            nameController = NameConfig.createController("My Name"),
            signUpScreenState = SignUpScreenState(
                merchantName = "Example, Inc.",
                signUpEnabled = false,
                signUpState = SignUpState.InputtingRemainingFields,
                requiresNameCollection = true,
                canEditEmail = true,
            ),
            onSignUpClick = {},
            onSuggestedEmailClick = {}
        )
    }
}
