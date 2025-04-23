package com.stripe.android.link.ui.update

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.SecondaryButton
import com.stripe.android.paymentsheet.R

@Composable
internal fun UpdateScreen(viewModel: UpdateViewModel) {
    UpdateScreenBody(
        onUpdateClicked = viewModel::onUpdateClicked,
        onCancelClicked = viewModel::onCancelClicked,
    )
}

@Composable
internal fun UpdateScreenBody(
    onUpdateClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    ScrollableTopLevelColumn {
        Text(
            modifier = Modifier
                .padding(bottom = 32.dp),
            text = stringResource(R.string.stripe_wallet_update_card),
            style = MaterialTheme.typography.h2
        )

        PrimaryButton(
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            label = stringResource(R.string.stripe_wallet_update_card),
            state = PrimaryButtonState.Enabled,
            onButtonClick = onUpdateClicked
        )

        SecondaryButton(
            modifier = Modifier.padding(bottom = 16.dp),
            label = stringResource(com.stripe.android.R.string.stripe_cancel),
            enabled = true,
            onClick = onCancelClicked
        )
    }
}
