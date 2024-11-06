package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.stripe.android.R
import com.stripe.android.uicore.stripeColors

@Composable
internal fun UpdatePaymentMethodUI(interactor: UpdatePaymentMethodInteractor, modifier: Modifier) {
    val horizontalPadding = dimensionResource(
        id = com.stripe.android.paymentsheet.R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )

    Column(
        modifier = modifier.padding(horizontal = horizontalPadding)
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = "•••• •••• •••• ${interactor.card.last4}",
            enabled = false,
            label = {
                Label(
                    text = stringResource(id = R.string.stripe_acc_label_card_number),
                    modifier = modifier
                )
            },
            trailingIcon = {
                PaymentMethodIconFromResource(
                    iconRes = interactor
                        .displayableSavedPaymentMethod
                        .paymentMethod
                        .getSavedPaymentMethodIcon(forVerticalMode = true),
                    colorFilter = null,
                    alignment = Alignment.Center,
                    modifier = Modifier,
                )
            },
            onValueChange = {}
        )
    }
}

@Composable
private fun Label(
    text: String,
    modifier: Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.stripeColors.placeholderText.copy(alpha = ContentAlpha.disabled),
        style = MaterialTheme.typography.subtitle1
    )
}
