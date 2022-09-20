package com.stripe.android.link.ui.inline

import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.LinkTerms
import com.stripe.android.link.ui.signup.EmailCollectionSection
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.PhoneNumberCollectionSection
import com.stripe.android.ui.core.elements.PhoneNumberController
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import com.stripe.android.ui.core.elements.TextFieldController
import com.stripe.android.ui.core.elements.TextFieldSection
import com.stripe.android.ui.core.elements.menu.Checkbox
import com.stripe.android.ui.core.getBorderStroke
import com.stripe.android.ui.core.paymentsColors

@Preview
@Composable
private fun Preview() {
    DefaultLinkTheme {
        Surface {
            LinkInlineSignup(
                merchantName = "Example, Inc.",
                emailController = SimpleTextFieldController.createEmailSectionController("email@me.co"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = SimpleTextFieldController.createNameSectionController("My Name"),
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LinkInlineSignup(
    linkPaymentLauncher: LinkPaymentLauncher,
    enabled: Boolean,
    onStateChanged: (InlineSignupViewState) -> Unit
) {
    linkPaymentLauncher.injector?.let { injector ->
        val viewModel: InlineSignupViewModel = viewModel(
            factory = InlineSignupViewModel.Factory(injector)
        )

        val viewState by viewModel.viewState.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()

        LaunchedEffect(viewState) {
            onStateChanged(viewState)
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
            merchantName = viewModel.merchantName,
            emailController = viewModel.emailController,
            phoneNumberController = viewModel.phoneController,
            nameController = viewModel.nameController,
            signUpState = viewState.signUpState,
            enabled = enabled,
            expanded = viewState.isExpanded,
            requiresNameCollection = viewModel.requiresNameCollection,
            errorMessage = errorMessage,
            toggleExpanded = viewModel::toggleExpanded
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
    toggleExpanded: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(expanded) {
        if (expanded) {
            focusRequester.requestFocus()
        }
    }

    CompositionLocalProvider(
        LocalContentAlpha provides if (enabled) ContentAlpha.high else ContentAlpha.disabled
    ) {
        PaymentsTheme {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        border = MaterialTheme.getBorderStroke(isSelected = false),
                        shape = MaterialTheme.linkShapes.medium
                    )
                    .background(
                        color = MaterialTheme.paymentsColors.component,
                        shape = MaterialTheme.linkShapes.medium
                    )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            toggleExpanded()
                        }
                ) {
                    Checkbox(
                        checked = expanded,
                        onCheckedChange = null, // needs to be null for accessibility on row click to work
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = enabled
                    )
                    Column {
                        Text(
                            text = stringResource(id = R.string.inline_sign_up_header),
                            style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colors.onSurface
                                .copy(alpha = LocalContentAlpha.current)
                        )
                        Text(
                            text = stringResource(R.string.sign_up_message, merchantName),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onSurface
                                .copy(alpha = LocalContentAlpha.current)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = expanded,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
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
                                text = errorMessage?.getMessage(LocalContext.current.resources)
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
                                        text = errorMessage?.getMessage(LocalContext.current.resources)
                                            .orEmpty(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                LinkTerms(
                                    modifier = Modifier.padding(top = 8.dp),
                                    textAlign = TextAlign.Left
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
