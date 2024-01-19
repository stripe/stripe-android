@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.link.ui.inline

import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.LinkTerms
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberCollectionSection
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.Section
import com.stripe.android.uicore.elements.TextField
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldSection
import com.stripe.android.uicore.elements.menu.Checkbox
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

internal const val ProgressIndicatorTestTag = "CircularProgressIndicator"

@Preview
@Composable
private fun Preview() {
    DefaultLinkTheme {
        Surface {
            LinkInlineSignup(
                merchantName = "Example, Inc.",
                emailController = EmailConfig.createController("email@me.co"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("My Name"),
                signUpState = SignUpState.InputtingEmail,
                enabled = true,
                expanded = true,
                requiresNameCollection = true,
                errorMessage = null,
                toggleExpanded = {}
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LinkInlineSignup(
    linkConfigurationCoordinator: LinkConfigurationCoordinator,
    enabled: Boolean,
    onStateChanged: (LinkConfiguration, InlineSignupViewState) -> Unit,
    modifier: Modifier = Modifier
) {
    linkConfigurationCoordinator.component?.let { component ->
        val viewModel: InlineSignupViewModel = viewModel(
            factory = InlineSignupViewModel.Factory(component)
        )

        val viewState by viewModel.viewState.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()

        LaunchedEffect(viewState) {
            onStateChanged(component.configuration, viewState)
        }

        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        LaunchedEffect(viewState.signUpState) {
            if (viewState.signUpState == SignUpState.InputtingEmail && viewState.userInput != null) {
                focusManager.clearFocus(true)
                keyboardController?.hide()
            }
        }

        LinkInlineSignup(
            merchantName = viewState.merchantName,
            emailController = viewModel.emailController,
            phoneNumberController = viewModel.phoneController,
            nameController = viewModel.nameController,
            signUpState = viewState.signUpState,
            enabled = enabled,
            expanded = viewState.isExpanded,
            requiresNameCollection = viewModel.requiresNameCollection,
            errorMessage = errorMessage,
            toggleExpanded = viewModel::toggleExpanded,
            modifier = modifier
        )
    }
}

@Composable
internal fun LinkInlineSignup(
    merchantName: String,
    emailController: TextFieldController,
    phoneNumberController: PhoneNumberController,
    nameController: TextFieldController,
    signUpState: SignUpState,
    enabled: Boolean,
    expanded: Boolean,
    requiresNameCollection: Boolean,
    errorMessage: ErrorMessage?,
    toggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(expanded) {
        if (expanded) {
            focusRequester.requestFocus()
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
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.stripeShapes.roundedCornerShape)
                .alpha(contentAlpha),
        ) {
            Column(
                modifier = Modifier.clickable(enabled = enabled) { toggleExpanded() },
            ) {
                Row(
                    modifier = Modifier
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
                            color = MaterialTheme.colors.onSurface
                                .copy(alpha = contentAlpha)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded
            ) {
                Column {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
                            )
                    ) {
                        EmailCollectionSection(
                            enabled = enabled,
                            emailController = emailController,
                            signUpState = signUpState,
                            focusRequester = focusRequester
                        )

                        AnimatedVisibility(
                            visible = signUpState != SignUpState.InputtingPhoneOrName &&
                                errorMessage != null
                        ) {
                            ErrorText(
                                text = errorMessage
                                    ?.getMessage(LocalContext.current.resources)
                                    .orEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        AnimatedVisibility(
                            visible = signUpState == SignUpState.InputtingPhoneOrName
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                PhoneNumberCollectionSection(
                                    enabled = enabled,
                                    phoneNumberController = phoneNumberController,
                                    requestFocusWhenShown =
                                    phoneNumberController.initialPhoneNumber.isEmpty(),
                                    imeAction = if (requiresNameCollection) {
                                        ImeAction.Next
                                    } else {
                                        ImeAction.Done
                                    }
                                )

                                if (requiresNameCollection) {
                                    TextFieldSection(
                                        textFieldController = nameController,
                                        imeAction = ImeAction.Done,
                                        enabled = enabled
                                    )
                                }

                                AnimatedVisibility(visible = errorMessage != null) {
                                    ErrorText(
                                        text = errorMessage
                                            ?.getMessage(LocalContext.current.resources)
                                            .orEmpty(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                LinkTerms(
                                    isOptional = false,
                                    modifier = Modifier.padding(top = 4.dp),
                                    textAlign = TextAlign.Start,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("SpreadOperator")
@Composable
private fun EmailCollectionSection(
    enabled: Boolean,
    emailController: TextFieldController,
    signUpState: SignUpState,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val error by emailController.error.collectAsState(null)

    Section(
        title = null,
        error = error?.let {
            it.formatArgs?.let { args ->
                stringResource(
                    it.errorMessage,
                    *args
                )
            } ?: stringResource(it.errorMessage)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                textFieldController = emailController,
                imeAction = if (signUpState == SignUpState.InputtingPhoneOrName) {
                    ImeAction.Next
                } else {
                    ImeAction.Done
                },
                enabled = enabled && signUpState != SignUpState.VerifyingEmail,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .weight(1f)
            )
            if (signUpState == SignUpState.VerifyingEmail) {
                CircularProgressIndicator(
                    progress = 0.7f,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(start = 0.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
                        .semantics {
                            testTag = ProgressIndicatorTestTag
                        },
                    color = MaterialTheme.linkColors.progressIndicator,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.stripe_link_logo),
                    contentDescription = stringResource(id = R.string.stripe_link),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .semantics {
                            testTag = "LinkLogoIcon"
                        },
                    tint = MaterialTheme.linkColors.inlineLinkLogo
                )
            }
        }
    }
}
