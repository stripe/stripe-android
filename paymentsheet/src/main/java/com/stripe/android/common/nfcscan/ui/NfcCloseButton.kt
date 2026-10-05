package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.stripeColors

@Composable
internal fun NfcCloseButton(
    onPress: () -> Unit,
) {
    IconButton(
        onClick = onPress,
        modifier = Modifier
            .size(CloseButtonSize)
            .testTag(NFC_CLOSE_BUTTON_TEST_TAG),
    ) {
        Icon(
            painter = painterResource(R.drawable.stripe_ic_paymentsheet_close),
            contentDescription = stringResource(R.string.stripe_paymentsheet_close),
            modifier = Modifier.size(CloseIconSize),
            tint = MaterialTheme.stripeColors.appBarIcon,
        )
    }
}

private val CloseButtonSize = 48.dp
private val CloseIconSize = 12.dp

internal val NfcCloseButtonTapTargetInset = (CloseButtonSize - CloseIconSize) / 2

internal const val NFC_CLOSE_BUTTON_TEST_TAG = "NFC_CLOSE_BUTTON_TEST_TAG"
