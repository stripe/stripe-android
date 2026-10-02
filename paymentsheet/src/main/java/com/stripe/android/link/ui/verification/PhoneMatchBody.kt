package com.stripe.android.link.ui.verification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.SectionStyle
import com.stripe.android.uicore.elements.PhoneNumberCollectionSection
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.ui.core.R as StripeUiCoreR

/**
 * Asks the user to confirm the full phone number on their Link account before an email code can be sent.
 */
@Composable
internal fun ColumnScope.PhoneMatchContent(
    phoneNumberLastTwoDigits: String?,
    phoneNumberController: PhoneNumberController,
    isProcessing: Boolean,
    errorMessage: ResolvableString?,
    onContinueClick: () -> Unit,
) {
    val isComplete by phoneNumberController.isComplete.collectAsState()

    Text(
        text = stringResource(R.string.stripe_link_auth_phone_match_title),
        modifier = Modifier
            .testTag(PHONE_MATCH_TITLE_TAG)
            .padding(vertical = 4.dp),
        textAlign = TextAlign.Center,
        style = LinkTheme.typography.title,
        color = LinkTheme.colors.textPrimary
    )

    Spacer(modifier = Modifier.size(8.dp))

    Text(
        text = if (phoneNumberLastTwoDigits != null) {
            stringResource(R.string.stripe_link_auth_phone_match_message, phoneNumberLastTwoDigits)
        } else {
            stringResource(R.string.stripe_link_auth_phone_match_message_no_hint)
        },
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = LinkTheme.typography.body,
        color = LinkTheme.colors.textTertiary
    )

    Spacer(modifier = Modifier.size(24.dp))

    StripeThemeForLink(sectionStyle = SectionStyle.Bordered) {
        PhoneNumberCollectionSection(
            modifier = Modifier.testTag(PHONE_MATCH_INPUT_TAG),
            enabled = !isProcessing,
            phoneNumberController = phoneNumberController,
            requestFocusWhenShown = true,
            imeAction = ImeAction.Done,
        )
    }

    AnimatedVisibility(visible = errorMessage != null) {
        ErrorText(
            text = errorMessage?.resolve(LocalContext.current).orEmpty(),
            modifier = Modifier
                .padding(top = 16.dp)
                .testTag(PHONE_MATCH_ERROR_TAG)
                .fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.size(24.dp))

    PrimaryButton(
        modifier = Modifier.testTag(PHONE_MATCH_CONTINUE_TAG),
        label = stringResource(StripeUiCoreR.string.stripe_continue_button_label),
        state = when {
            isProcessing -> PrimaryButtonState.Processing
            isComplete -> PrimaryButtonState.Enabled
            else -> PrimaryButtonState.Disabled
        },
        onButtonClick = onContinueClick,
    )
}

internal const val PHONE_MATCH_TITLE_TAG = "phone_match_title"
internal const val PHONE_MATCH_INPUT_TAG = "phone_match_input"
internal const val PHONE_MATCH_ERROR_TAG = "phone_match_error"
internal const val PHONE_MATCH_CONTINUE_TAG = "phone_match_continue"
