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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
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
import com.stripe.android.link.ui.signup.PhoneCollectionSection
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.SectionFieldElement
import com.stripe.android.ui.core.elements.menu.Checkbox
import kotlinx.coroutines.flow.MutableStateFlow

@Preview
@Composable
private fun Preview() {
    LinkInlineSignup(
        merchantName = "Example, Inc.",
        emailElement = EmailSpec.transform(mapOf(IdentifierSpec.Email to "email")),
        signUpState = SignUpState.InputtingEmail,
        isExpanded = true,
        toggleExpanded = {},
        onPhoneInputCompleted = {},
        onUserInteracted = {}
    )
}

@Composable
private fun LinkInlineSignup(
    injector: NonFallbackInjector,
    onUserInteracted: () -> Unit,
    onReady: (Boolean) -> Unit
) {
    val viewModel: InlineSignupViewModel = viewModel(
        factory = InlineSignupViewModel.Factory(injector)
    )

    val signUpState by viewModel.signUpState.collectAsState(SignUpState.InputtingEmail)
    val isExpanded by viewModel.isExpanded.collectAsState(false)

    LinkInlineSignup(
        merchantName = viewModel.merchantName,
        emailElement = viewModel.emailElement,
        signUpState = signUpState,
        isExpanded = isExpanded,
        toggleExpanded = viewModel::toggleExpanded,
        onPhoneInputCompleted = viewModel::onPhoneInputCompleted,
        onUserInteracted = onUserInteracted
    )
}

@Composable
private fun LinkInlineSignup(
    merchantName: String,
    emailElement: SectionFieldElement,
    signUpState: SignUpState,
    isExpanded: Boolean,
    toggleExpanded: () -> Unit,
    onPhoneInputCompleted: (String) -> Unit,
    onUserInteracted: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    DefaultLinkTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = PaymentsTheme.colors.colorComponentBorder,
                    shape = PaymentsTheme.shapes.material.medium
                )
                .background(
                    color = PaymentsTheme.colors.component,
                    shape = PaymentsTheme.shapes.material.medium
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
                    checked = isExpanded,
                    onCheckedChange = null, // needs to be null for accessibility on row click to work
                    modifier = Modifier.padding(end = 8.dp),
                    enabled = true
                )
                Column {
                    Text(
                        text = stringResource(id = R.string.inline_sign_up_header),
                        style = PaymentsTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
                        color = PaymentsTheme.colors.material.onSurface
                    )
                    Text(
                        text = stringResource(R.string.sign_up_message, merchantName),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        style = PaymentsTheme.typography.body1,
                        color = PaymentsTheme.colors.material.onSurface
                    )
                }
            }
            AnimatedVisibility(
                visible = isExpanded,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    EmailCollectionSection(emailElement = emailElement, signUpState = signUpState)

                    AnimatedVisibility(
                        visible = signUpState == SignUpState.InputtingPhone
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // TODO(brnunes-stripe): Migrate to phone number collection element
                            PhoneCollectionSection(
                                phoneNumber = phoneNumber,
                                onPhoneNumberChanged = {
                                    phoneNumber = it
                                    if (phoneNumber.length == 10) {
                                        onPhoneInputCompleted(phoneNumber)
                                        keyboardController?.hide()
                                    }
                                }
                            )
                            LinkTerms(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 24.dp),
                                textAlign = TextAlign.Left
                            )
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
     * Whether enough information has been collected to proceed with the payment flow.
     * This will be true when the user has entered an email that already has a link account and just
     * needs verification, or when they entered a new email and phone number.
     */
    val isReady = MutableStateFlow(true)

    @Composable
    override fun Content() {
        linkLauncher?.injector?.let {
            PaymentsTheme {
                LinkInlineSignup(
                    injector = it,
                    onUserInteracted = { hasUserInteracted = true },
                    onReady = { isReady.value = it }
                )
            }
        }
    }
}
