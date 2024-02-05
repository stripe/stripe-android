@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.link.ui.inline

import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.LinkTerms
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.ui.core.CircularProgressIndicator
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElementUI
import com.stripe.android.uicore.elements.Section
import com.stripe.android.uicore.elements.SectionController
import com.stripe.android.uicore.elements.TextField
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes
import kotlinx.coroutines.job

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LinkOptionalInlineSignup(
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
            if (viewState.signUpState == SignUpState.InputtingPrimaryField && viewState.userInput != null) {
                focusManager.clearFocus(true)
                keyboardController?.hide()
            }
        }

        LinkOptionalInlineSignup(
            sectionController = viewModel.sectionController,
            emailController = viewModel.emailController,
            phoneNumberController = viewModel.phoneController,
            nameController = viewModel.nameController,
            signUpState = viewState.signUpState,
            isShowingPhoneFirst = viewState.isShowingPhoneFirst,
            enabled = enabled,
            requiresNameCollection = viewModel.requiresNameCollection,
            errorMessage = errorMessage,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LinkOptionalInlineSignup(
    sectionController: SectionController,
    emailController: TextFieldController,
    phoneNumberController: PhoneNumberController,
    nameController: TextFieldController,
    isShowingPhoneFirst: Boolean,
    signUpState: SignUpState,
    enabled: Boolean,
    requiresNameCollection: Boolean,
    errorMessage: ErrorMessage?,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        val bringTermsIntoViewRequester = remember { BringIntoViewRequester() }
        val emailFocusRequester = remember { FocusRequester() }
        val phoneFocusRequester = remember { FocusRequester() }
        val nameFocusRequester = remember { FocusRequester() }

        var didShowAllFields by rememberSaveable { mutableStateOf(false) }
        val sectionError by sectionController.error.collectAsState(null)

        if (signUpState == SignUpState.InputtingRemainingFields) {
            LaunchedEffect(signUpState) {
                bringTermsIntoViewRequester.bringIntoView()

                val isMissingEmail = emailController.initialValue.isNullOrBlank()
                val isMissingPhone = emailController.initialValue.isNullOrBlank()

                val nextFocusRequester = if (isShowingPhoneFirst && isMissingEmail) {
                    emailFocusRequester
                } else if (!isShowingPhoneFirst && isMissingPhone) {
                    phoneFocusRequester
                } else {
                    nameFocusRequester.takeIf { requiresNameCollection }
                }

                nextFocusRequester?.requestFocus()
            }
        }

        Section(
            title = null,
            error = sectionError?.errorMessage?.let { stringResource(it) },
        ) {
            if (isShowingPhoneFirst) {
                PhoneNumberElementUI(
                    enabled = enabled,
                    controller = phoneNumberController,
                    moveToNextFieldOnceComplete = true,
                    imeAction = if (signUpState == SignUpState.InputtingRemainingFields) {
                        ImeAction.Next
                    } else {
                        ImeAction.Done
                    },
                    focusRequester = phoneFocusRequester,
                    trailingIcon = { LinkLogo() },
                )
            } else {
                EmailCollection(
                    enabled = enabled,
                    emailController = emailController,
                    signUpState = signUpState,
                    imeAction = if (signUpState == SignUpState.InputtingRemainingFields) {
                        ImeAction.Next
                    } else {
                        ImeAction.Done
                    },
                    focusRequester = emailFocusRequester,
                    trailingIcon = { LinkLogo() },
                )
            }

            AnimatedVisibility(
                visible = signUpState != SignUpState.InputtingRemainingFields && errorMessage != null,
            ) {
                ErrorText(
                    text = errorMessage
                        ?.getMessage(LocalContext.current.resources)
                        .orEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(visible = didShowAllFields || signUpState == SignUpState.InputtingRemainingFields) {
                LaunchedEffect(Unit) {
                    didShowAllFields = true
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Divider(
                        color = MaterialTheme.stripeColors.componentDivider,
                        thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.stripeShapes.borderStrokeWidth.dp
                        )
                    )

                    if (isShowingPhoneFirst) {
                        EmailCollection(
                            enabled = enabled,
                            emailController = emailController,
                            signUpState = signUpState,
                            imeAction = if (requiresNameCollection) {
                                ImeAction.Next
                            } else {
                                ImeAction.Done
                            },
                            focusRequester = emailFocusRequester,
                        )
                    } else {
                        PhoneNumberElementUI(
                            enabled = enabled,
                            controller = phoneNumberController,
                            moveToNextFieldOnceComplete = requiresNameCollection,
                            requestFocusWhenShown = phoneNumberController.initialPhoneNumber.isEmpty(),
                            imeAction = if (requiresNameCollection) {
                                ImeAction.Next
                            } else {
                                ImeAction.Done
                            },
                            focusRequester = phoneFocusRequester,
                        )
                    }

                    Divider(
                        color = MaterialTheme.stripeColors.componentDivider,
                        thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.stripeShapes.borderStrokeWidth.dp
                        )
                    )

                    if (requiresNameCollection) {
                        TextField(
                            textFieldController = nameController,
                            imeAction = ImeAction.Done,
                            enabled = enabled,
                            focusRequester = nameFocusRequester,
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
                }
            }
        }

        LinkTerms(
            isOptional = true,
            isShowingPhoneFirst = isShowingPhoneFirst,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(top = 4.dp)
                .bringIntoViewRequester(bringTermsIntoViewRequester),
        )
    }
}

@Composable
internal fun EmailCollection(
    enabled: Boolean,
    emailController: TextFieldController,
    signUpState: SignUpState,
    imeAction: ImeAction,
    focusRequester: FocusRequester = remember { FocusRequester() },
    requestFocusWhenShown: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            textFieldController = emailController,
            imeAction = imeAction,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )

        if (signUpState == SignUpState.VerifyingEmail) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = 0.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
                    .semantics {
                        testTag = ProgressIndicatorTestTag
                    },
                color = MaterialTheme.colors.primary,
                strokeWidth = 2.dp
            )
        }

        trailingIcon?.invoke()
    }

    if (requestFocusWhenShown) {
        LaunchedEffect(Unit) {
            coroutineContext.job.invokeOnCompletion {
                focusRequester.requestFocus()
            }
        }
    }
}

@Composable
private fun LinkLogo() {
    Icon(
        painter = painterResource(id = R.drawable.stripe_link_logo),
        contentDescription = stringResource(id = R.string.stripe_link),
        modifier = Modifier
            .padding(end = 12.dp)
            .semantics {
                testTag = "LinkLogoIcon"
            },
        tint = MaterialTheme.stripeColors.placeholderText,
    )
}

@Preview
@Composable
private fun PreviewInitial() {
    DefaultLinkTheme {
        Surface {
            LinkOptionalInlineSignup(
                sectionController = SectionController(null, emptyList()),
                emailController = EmailConfig.createController(""),
                phoneNumberController = PhoneNumberController.createPhoneNumberController(""),
                nameController = NameConfig.createController(""),
                signUpState = SignUpState.InputtingPrimaryField,
                enabled = true,
                isShowingPhoneFirst = false,
                requiresNameCollection = true,
                errorMessage = null,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewInitialWithPhoneFirst() {
    DefaultLinkTheme {
        Surface {
            LinkOptionalInlineSignup(
                sectionController = SectionController(null, emptyList()),
                emailController = EmailConfig.createController(""),
                phoneNumberController = PhoneNumberController.createPhoneNumberController(""),
                nameController = NameConfig.createController(""),
                signUpState = SignUpState.InputtingPrimaryField,
                enabled = true,
                isShowingPhoneFirst = true,
                requiresNameCollection = true,
                errorMessage = null,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewFilledOut() {
    DefaultLinkTheme {
        Surface {
            LinkOptionalInlineSignup(
                sectionController = SectionController(null, emptyList()),
                emailController = EmailConfig.createController("email@me.co"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("My Name"),
                signUpState = SignUpState.InputtingRemainingFields,
                enabled = true,
                isShowingPhoneFirst = false,
                requiresNameCollection = true,
                errorMessage = null,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
