@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.link.ui.inline

import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.stripe.android.uicore.elements.SectionController
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.menu.Checkbox
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch

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
    val textInputService = LocalTextInputService.current

    LaunchedEffect(viewState.signUpState) {
        if (viewState.signUpState == SignUpState.InputtingPrimaryField && viewState.userInput != null) {
            focusManager.clearFocus(true)
            @Suppress("DEPRECATION")
            textInputService?.hideSoftwareKeyboard()
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
        errorMessage = errorMessage?.resolve(),
        toggleExpanded = viewModel::toggleExpanded,
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
    errorMessage: String?,
    toggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val emailFocusRequester = remember { FocusRequester() }
    val bringFullSignUpIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(expanded) {
        if (expanded) {
            emailFocusRequester.requestFocus()
        }
    }

    val contentAlpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled

    Box(
        modifier = modifier
            .border(
                border = MaterialTheme.getBorderStroke(isSelected = false),
                shape = MaterialTheme.stripeShapes.roundedCornerShape,
            )
            .background(
                color = MaterialTheme.stripeColors.component,
                shape = MaterialTheme.stripeShapes.roundedCornerShape,
            )
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
                .clip(MaterialTheme.stripeShapes.roundedCornerShape)
                .alpha(contentAlpha),
        ) {
            LinkCheckbox(
                merchantName = merchantName,
                expanded = expanded,
                enabled = enabled,
                contentAlpha = contentAlpha,
                toggleExpanded = toggleExpanded
            )

            LinkFields(
                expanded = expanded,
                enabled = enabled,
                signUpState = signUpState,
                requiresNameCollection = requiresNameCollection,
                errorMessage = errorMessage,
                sectionController = sectionController,
                emailController = emailController,
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                emailFocusRequester = emailFocusRequester,
            )
        }
    }
}

@Composable
private fun LinkCheckbox(
    merchantName: String,
    expanded: Boolean,
    enabled: Boolean,
    contentAlpha: Float,
    toggleExpanded: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(enabled = enabled) { toggleExpanded() }
            .padding(16.dp)
    ) {
        Checkbox(
            checked = expanded,
            onCheckedChange = null, // needs to be null for accessibility on row click to work
            modifier = Modifier.padding(end = 8.dp),
            enabled = enabled
        )
        Column {
            Text(
                text = stringResource(id = R.string.stripe_inline_sign_up_header),
                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colors.onSurface
                    .copy(alpha = contentAlpha)
            )
            Text(
                text = stringResource(R.string.stripe_sign_up_message, merchantName),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.stripeColors.subtitle
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
    errorMessage: String?,
    sectionController: SectionController,
    emailController: TextFieldController,
    phoneNumberController: PhoneNumberController,
    nameController: TextFieldController,
    emailFocusRequester: FocusRequester,
) {
    var didShowAllFields by rememberSaveable { mutableStateOf(false) }

    val sectionError by sectionController.error.collectAsState()

    AnimatedVisibility(visible = expanded, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            )
        ) {
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
                errorMessage = errorMessage,
                didShowAllFields = didShowAllFields,
                onShowingAllFields = { didShowAllFields = true },
            )

            AnimatedVisibility(visible = signUpState == InputtingRemainingFields) {
                LinkTerms(
                    type = LinkTermsType.Inline,
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
                errorMessage = null,
                toggleExpanded = {}
            )
        }
    }
}
