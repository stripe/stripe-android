package com.stripe.android.link.ui.inline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.HyperlinkedText
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElementUI
import com.stripe.android.uicore.elements.Section
import com.stripe.android.uicore.elements.TextField
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

private val LinkLogoModifier = Modifier.padding(end = 16.dp)

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
    allowsDefaultOptIn: Boolean,
    errorMessage: String?,
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
                trailingIcon = if (!allowsDefaultOptIn) {
                    {
                        LinkLogo(LinkLogoModifier)
                    }
                } else {
                    null
                },
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
                trailingIcon = if (!allowsDefaultOptIn) {
                    {
                        LinkLogo(LinkLogoModifier)
                    }
                } else {
                    null
                },
            )
        }

        AnimatedVisibility(
            visible = signUpState != SignUpState.InputtingRemainingFields && errorMessage != null,
        ) {
            DefaultLinkTheme {
                LinkInlineErrorText(
                    text = errorMessage.orEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        AnimatedVisibility(
            visible = didShowAllFields || signUpState == SignUpState.InputtingRemainingFields,
            modifier = Modifier.fillMaxWidth(),
        ) {
            LaunchedEffect(Unit) {
                onShowingAllFields()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(LINK_INLINE_SIGNUP_REMAINING_FIELDS_TEST_TAG)
            ) {
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
                        requestFocusWhenShown = !allowsDefaultOptIn &&
                            phoneNumberController.initialPhoneNumber.isEmpty(),
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
                    LinkInlineErrorText(
                        text = errorMessage.orEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkInlineErrorText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 12.dp)
                .size(20.dp),
            painter = painterResource(id = R.drawable.stripe_ic_sail_warning_circle),
            contentDescription = null,
            tint = MaterialTheme.colors.error
        )
        HyperlinkedText(
            text = text,
            modifier = Modifier
                .padding(vertical = 12.dp),
            color = MaterialTheme.colors.error,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        )
    }
}

@Composable
@Preview
internal fun PreviewLinkInlineSignupFields() {
    LinkInlineSignupFields(
        sectionError = null,
        emailController = EmailConfig.createController(
            initialValue = "test@test.com",
        ),
        phoneNumberController = PhoneNumberController.createPhoneNumberController(),
        nameController = NameConfig.createController(initialValue = null),
        signUpState = SignUpState.InputtingRemainingFields,
        enabled = true,
        isShowingPhoneFirst = false,
        requiresNameCollection = false,
        errorMessage = "This is a large error!",
        didShowAllFields = false,
        allowsDefaultOptIn = false,
        onShowingAllFields = {},
    )
}

internal const val LINK_INLINE_SIGNUP_REMAINING_FIELDS_TEST_TAG = "LinkInlineSignupRemainingFields"
