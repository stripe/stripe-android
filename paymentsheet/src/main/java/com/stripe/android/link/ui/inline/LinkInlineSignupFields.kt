package com.stripe.android.link.ui.inline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElementUI
import com.stripe.android.uicore.elements.Section
import com.stripe.android.uicore.elements.TextField
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

@Composable
internal fun LinkInlineSignupFields(
    sectionError: Int?,
    emailController: TextFieldController,
    phoneNumberController: PhoneNumberController,
    nameController: TextFieldController,
    signUpState: SignUpState,
    enabled: Boolean,
    isShowingPhoneFirst: Boolean,
    requiresNameCollection: Boolean,
    errorMessage: ErrorMessage?,
    didShowAllFields: Boolean,
    onShowingAllFields: () -> Unit,
    modifier: Modifier = Modifier,
    emailFocusRequester: FocusRequester = remember { FocusRequester() },
    phoneFocusRequester: FocusRequester = remember { FocusRequester() },
    nameFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    Section(
        title = null,
        error = sectionError?.let { stringResource(it) },
        modifier = modifier,
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
                onShowingAllFields()
            }

            Column(modifier = Modifier.fillMaxWidth().testTag(LINK_INLINE_SIGNUP_REMAINING_FIELDS_TEST_TAG)) {
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
}

internal const val LINK_INLINE_SIGNUP_REMAINING_FIELDS_TEST_TAG = "LinkInlineSignupRemainingFields"
