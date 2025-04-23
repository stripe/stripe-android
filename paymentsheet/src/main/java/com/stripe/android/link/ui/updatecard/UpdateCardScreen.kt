package com.stripe.android.link.ui.updatecard

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.SecondaryButton
import com.stripe.android.paymentsheet.ui.CardDetailsEditUI
import com.stripe.android.paymentsheet.ui.EditCardDetailsInteractor
import com.stripe.android.R as StripeR

@Composable
internal fun UpdateCardScreen(viewModel: UpdateCardScreenViewModel) {
    UpdateCardScreenBody(
        interactor = viewModel.interactor,
        onUpdateClicked = viewModel::onUpdateClicked,
        onCancelClicked = viewModel::onCancelClicked,
    )
}

@Composable
internal fun UpdateCardScreenBody(
    interactor: EditCardDetailsInteractor,
    onUpdateClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    ScrollableTopLevelColumn {
        Text(
            modifier = Modifier
                .padding(bottom = 32.dp),
            text = stringResource(R.string.stripe_link_update_card_title),
            style = MaterialTheme.typography.h2
        )

        StripeThemeForLink {
            CardDetailsEditUI(
                editCardDetailsInteractor = interactor,
            )
        }

        PrimaryButton(
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            label = stringResource(R.string.stripe_link_update_card_confirm_cta),
            state = PrimaryButtonState.Enabled,
            onButtonClick = onUpdateClicked
        )

        SecondaryButton(
            label = stringResource(StripeR.string.stripe_cancel),
            enabled = true,
            onClick = onCancelClicked
        )
    }
}
