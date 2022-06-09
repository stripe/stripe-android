package com.stripe.android.link.ui.inline

import android.content.Context
import android.util.AttributeSet
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.R
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.LinkTerms
import com.stripe.android.link.ui.signup.EmailCollectionSection
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.PhoneNumberCollectionSection
import com.stripe.android.ui.core.elements.PhoneNumberController
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import com.stripe.android.ui.core.elements.TextFieldController
import com.stripe.android.ui.core.elements.menu.Checkbox
import com.stripe.android.ui.core.getBorderStroke
import com.stripe.android.ui.core.paymentsColors
import kotlinx.coroutines.flow.MutableStateFlow

@Preview
@Composable
private fun Preview() {
    DefaultLinkTheme {
        Surface {
            LinkInlineSignup(
                merchantName = "Example, Inc.",
                emailController = SimpleTextFieldController.createEmailSectionController("email@me.co"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                signUpState = SignUpState.InputtingEmail,
                enabled = true,
                expanded = true,
                toggleExpanded = {},
                onUserInteracted = {}
            )
        }
    }
}

@Composable
private fun LinkInlineSignup(
    injector: NonFallbackInjector,
    enabled: Boolean,
    onUserInteracted: () -> Unit,
    onSelected: (Boolean) -> Unit,
    onUserInput: (UserInput?) -> Unit
) {
    val viewModel: InlineSignupViewModel = viewModel(
        factory = InlineSignupViewModel.Factory(injector)
    )

    val signUpState by viewModel.signUpState.collectAsState(SignUpState.InputtingEmail)
    val isExpanded by viewModel.isExpanded.collectAsState(false)
    val userInput by viewModel.userInput.collectAsState()

    onSelected(isExpanded)
    onUserInput(userInput)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(signUpState) {
        if (signUpState == SignUpState.InputtingEmail && userInput != null) {
            focusManager.clearFocus(true)
            keyboardController?.hide()
        }
    }

    LinkInlineSignup(
        merchantName = viewModel.merchantName,
        emailController = viewModel.emailController,
        phoneNumberController = viewModel.phoneController,
        signUpState = signUpState,
        enabled = enabled,
        expanded = isExpanded,
        toggleExpanded = viewModel::toggleExpanded,
        onUserInteracted = onUserInteracted
    )
}

@Composable
internal fun LinkInlineSignup(
    merchantName: String,
    emailController: TextFieldController,
    phoneNumberController: PhoneNumberController,
    signUpState: SignUpState,
    enabled: Boolean,
    expanded: Boolean,
    toggleExpanded: () -> Unit,
    onUserInteracted: () -> Unit
) {
    CompositionLocalProvider(
        LocalContentAlpha provides if (enabled) ContentAlpha.high else ContentAlpha.disabled,
    ) {
        PaymentsTheme {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        border = MaterialTheme.getBorderStroke(isSelected = false),
                        shape = MaterialTheme.shapes.medium
                    )
                    .background(
                        color = MaterialTheme.paymentsColors.component,
                        shape = MaterialTheme.shapes.medium
                    )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            toggleExpanded()
                            onUserInteracted()
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
                            signUpState = signUpState
                        )

                        AnimatedVisibility(
                            visible = signUpState == SignUpState.InputtingPhone
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                PhoneNumberCollectionSection(
                                    enabled = enabled,
                                    phoneNumberController = phoneNumberController,
                                    requestFocusWhenShown =
                                    phoneNumberController.initialPhoneNumber.isEmpty()
                                )
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkInlineSignupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    var linkLauncher: LinkPaymentLauncher? = null

    /**
     *  Keep track of whether the user has interacted with the inline signup UI, so that it's not
     *  hidden when the current Link account changes.
     */
    var hasUserInteracted = false

    /**
     * The collected input from the user, always valid unless null.
     * When not null, enough information has been collected to proceed with the payment flow.
     * This means that the user has entered an email that already has a link account and just
     * needs verification, or entered a new email and phone number.
     */
    val userInput = MutableStateFlow<UserInput?>(null)

    val isSelected = MutableStateFlow(false)

    private var enabledState by mutableStateOf(isEnabled)

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        enabledState = enabled
    }

    @Composable
    override fun Content() {
        linkLauncher?.injector?.let {
            PaymentsTheme {
                LinkInlineSignup(
                    injector = it,
                    enabled = enabledState,
                    onUserInteracted = { hasUserInteracted = true },
                    onSelected = { isSelected.value = it },
                    onUserInput = { userInput.value = it }
                )
            }
        }
    }
}
