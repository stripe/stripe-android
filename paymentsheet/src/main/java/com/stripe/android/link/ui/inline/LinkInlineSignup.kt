@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.link.ui.inline

import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.LinkTerms
import com.stripe.android.link.ui.LinkTermsType
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.link.ui.signup.SignUpState.InputtingRemainingFields
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.elements.SectionController
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.menu.Checkbox
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch
import com.stripe.android.uicore.R as StripeUiCoreR

internal const val ProgressIndicatorTestTag = "CircularProgressIndicator"

@Composable
internal fun LinkInlineSignup(
    viewModel: InlineSignupViewModel,
    enabled: Boolean,
    onStateChanged: (InlineSignupViewState) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewState by viewModel.viewState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(viewState) {
        onStateChanged(viewState)
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(viewState.signUpState) {
        if (viewState.signUpState == SignUpState.InputtingPrimaryField && viewState.userInput != null) {
            focusManager.clearFocus(true)
            keyboardController?.hide()
        }
    }

    LinkInlineSignup(
        merchantName = viewState.merchantName,
        sectionController = viewModel.sectionController,
        emailController = viewModel.emailController,
        phoneNumberController = viewModel.phoneController,
        nameController = viewModel.nameController,
        signUpState = viewState.signUpState,
        enabled = enabled,
        expanded = viewState.isExpanded,
        requiresNameCollection = viewModel.requiresNameCollection,
        allowsDefaultOptIn = viewState.allowsDefaultOptIn,
        linkSignUpOptInFeatureEnabled = viewState.linkSignUpOptInFeatureEnabled,
        didAskToChangeSignupDetails = viewState.didAskToChangeSignupDetails,
        errorMessage = errorMessage?.resolve(),
        toggleExpanded = viewModel::toggleExpanded,
        changeSignupDetails = viewModel::changeSignupDetails,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LinkInlineSignup(
    merchantName: String,
    sectionController: SectionController,
    emailController: TextFieldController,
    phoneNumberController: PhoneNumberController,
    nameController: TextFieldController,
    signUpState: SignUpState,
    enabled: Boolean,
    expanded: Boolean,
    requiresNameCollection: Boolean,
    allowsDefaultOptIn: Boolean,
    linkSignUpOptInFeatureEnabled: Boolean,
    didAskToChangeSignupDetails: Boolean,
    errorMessage: String?,
    toggleExpanded: () -> Unit,
    changeSignupDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val emailFocusRequester = remember { FocusRequester() }
    val bringFullSignUpIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(expanded) {
        if (expanded && !allowsDefaultOptIn && !linkSignUpOptInFeatureEnabled) {
            emailFocusRequester.requestFocus()
        }
    }

    val contentAlpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled
    val shape = if (allowsDefaultOptIn || linkSignUpOptInFeatureEnabled) {
        // We render the content inline for default opt-in. A large corner radius would cut into the content.
        RectangleShape
    } else {
        MaterialTheme.stripeShapes.roundedCornerShape
    }

    val boxModifier = if (allowsDefaultOptIn || linkSignUpOptInFeatureEnabled) {
        modifier
    } else {
        modifier
            .border(
                border = MaterialTheme.getBorderStroke(isSelected = false),
                shape = shape,
            )
            .background(
                color = MaterialTheme.stripeColors.component,
                shape = shape,
            )
    }

    Box(
        modifier = boxModifier
            .onFocusEvent { state ->
                if (state.hasFocus && expanded) {
                    scope.launch {
                        bringFullSignUpIntoViewRequester.bringIntoView()
                    }
                }
            }
            .bringIntoViewRequester(bringFullSignUpIntoViewRequester),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .alpha(contentAlpha),
        ) {
            LinkCheckbox(
                merchantName = merchantName,
                expanded = expanded,
                enabled = enabled,
                contentAlpha = contentAlpha,
                defaultOptIn = allowsDefaultOptIn,
                linkSignUpOptInFeatureEnabled = linkSignUpOptInFeatureEnabled,
                toggleExpanded = toggleExpanded
            )

            if (linkSignUpOptInFeatureEnabled.not()) {
                LinkFields(
                    expanded = expanded,
                    enabled = enabled,
                    signUpState = signUpState,
                    requiresNameCollection = requiresNameCollection,
                    allowsDefaultOptIn = allowsDefaultOptIn,
                    didAskToChangeSignupDetails = didAskToChangeSignupDetails,
                    errorMessage = errorMessage,
                    sectionController = sectionController,
                    emailController = emailController,
                    phoneNumberController = phoneNumberController,
                    nameController = nameController,
                    emailFocusRequester = emailFocusRequester,
                    changeSignupDetails = changeSignupDetails,
                )
            }
        }
    }
}

@Composable
private fun LinkCheckbox(
    merchantName: String,
    expanded: Boolean,
    enabled: Boolean,
    contentAlpha: Float,
    defaultOptIn: Boolean,
    toggleExpanded: () -> Unit,
    linkSignUpOptInFeatureEnabled: Boolean,
) {
    val simplifiedCheckbox = linkSignUpOptInFeatureEnabled || defaultOptIn

    val label = if (simplifiedCheckbox) {
        stringResource(id = R.string.stripe_inline_sign_up_header_default_opt_in)
    } else {
        stringResource(id = R.string.stripe_inline_sign_up_header)
    }

    val sublabel = if (!simplifiedCheckbox) {
        stringResource(R.string.stripe_sign_up_message, merchantName)
    } else {
        null
    }

    Row(
        verticalAlignment = if (simplifiedCheckbox) Alignment.CenterVertically else Alignment.Top,
        modifier = Modifier
            .clickable(enabled = enabled) { toggleExpanded() }
            .padding(if (simplifiedCheckbox) 0.dp else 16.dp)
    ) {
        Checkbox(
            checked = expanded,
            onCheckedChange = null, // needs to be null for accessibility on row click to work
            modifier = Modifier.padding(end = 8.dp),
            enabled = enabled
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colors.onSurface.copy(alpha = contentAlpha)
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.stripeColors.subtitle
                )
            }
        }
    }
}

@Composable
internal fun LinkDefaultOptIn(
    enabled: Boolean,
    email: String,
    phoneNumber: String,
    modifier: Modifier = Modifier,
    onChange: () -> Unit,
) {
    SectionCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = email,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = phoneNumber,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.stripeColors.subtitle,
                    maxLines = 1,
                )
            }

            Text(
                text = stringResource(id = StripeUiCoreR.string.stripe_change),
                color = MaterialTheme.colors.primary,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(enabled = enabled, onClick = onChange),
            )
        }
    }
}

@Composable
internal fun LinkFields(
    expanded: Boolean,
    enabled: Boolean,
    signUpState: SignUpState,
    requiresNameCollection: Boolean,
    allowsDefaultOptIn: Boolean,
    didAskToChangeSignupDetails: Boolean,
    errorMessage: String?,
    sectionController: SectionController,
    emailController: TextFieldController,
    phoneNumberController: PhoneNumberController,
    nameController: TextFieldController,
    emailFocusRequester: FocusRequester,
    changeSignupDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var didShowAllFields by rememberSaveable { mutableStateOf(false) }

    val sectionError by sectionController.error.collectAsState()

    val columnModifier = if (allowsDefaultOptIn) {
        Modifier
    } else {
        Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp,
        )
    }

    val hasEmail = emailController.initialValue?.isBlank() == false
    val hasPhoneNumber = phoneNumberController.initialPhoneNumber.isNotBlank()
    val showDefaultOptIn = allowsDefaultOptIn && hasEmail && hasPhoneNumber && !didAskToChangeSignupDetails

    AnimatedVisibility(visible = expanded, modifier = modifier.fillMaxWidth()) {
        Column(modifier = columnModifier) {
            if (showDefaultOptIn) {
                LinkDefaultOptIn(
                    enabled = enabled,
                    email = emailController.initialValue.orEmpty(),
                    phoneNumber = phoneNumberController.formatLocalNumber(),
                    onChange = changeSignupDetails,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                LinkInlineSignupFields(
                    sectionError = sectionError?.errorMessage,
                    emailController = emailController,
                    phoneNumberController = phoneNumberController,
                    nameController = nameController,
                    signUpState = signUpState,
                    enabled = enabled,
                    isShowingPhoneFirst = false,
                    emailFocusRequester = emailFocusRequester,
                    requiresNameCollection = requiresNameCollection,
                    allowsDefaultOptIn = allowsDefaultOptIn,
                    errorMessage = errorMessage,
                    didShowAllFields = didShowAllFields,
                    onShowingAllFields = { didShowAllFields = true },
                    modifier = Modifier.padding(top = if (allowsDefaultOptIn) 8.dp else 0.dp),
                )
            }

            AnimatedVisibility(visible = allowsDefaultOptIn || signUpState == InputtingRemainingFields) {
                LinkTerms(
                    type = if (allowsDefaultOptIn) LinkTermsType.InlineWithDefaultOptIn else LinkTermsType.Inline,
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    DefaultLinkTheme {
        Surface {
            LinkInlineSignup(
                merchantName = "Example, Inc.",
                sectionController = SectionController(null, emptyList()),
                emailController = EmailConfig.createController("email@me.co"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("My Name"),
                signUpState = InputtingRemainingFields,
                enabled = true,
                expanded = true,
                requiresNameCollection = true,
                allowsDefaultOptIn = false,
                linkSignUpOptInFeatureEnabled = false,
                didAskToChangeSignupDetails = false,
                errorMessage = null,
                toggleExpanded = {},
                changeSignupDetails = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDOI() {
    DefaultLinkTheme {
        Surface {
            LinkInlineSignup(
                merchantName = "Example, Inc.",
                sectionController = SectionController(null, emptyList()),
                emailController = EmailConfig.createController(""),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("My Name"),
                signUpState = InputtingRemainingFields,
                enabled = true,
                expanded = true,
                requiresNameCollection = true,
                allowsDefaultOptIn = true,
                linkSignUpOptInFeatureEnabled = false,
                didAskToChangeSignupDetails = false,
                errorMessage = null,
                toggleExpanded = {},
                changeSignupDetails = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSignInFeature() {
    DefaultLinkTheme {
        Surface {
            LinkInlineSignup(
                merchantName = "Example, Inc.",
                sectionController = SectionController(null, emptyList()),
                emailController = EmailConfig.createController(""),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("My Name"),
                signUpState = InputtingRemainingFields,
                enabled = true,
                expanded = true,
                requiresNameCollection = false,
                allowsDefaultOptIn = false,
                linkSignUpOptInFeatureEnabled = true,
                didAskToChangeSignupDetails = false,
                errorMessage = null,
                toggleExpanded = {},
                changeSignupDetails = {},
            )
        }
    }
}
