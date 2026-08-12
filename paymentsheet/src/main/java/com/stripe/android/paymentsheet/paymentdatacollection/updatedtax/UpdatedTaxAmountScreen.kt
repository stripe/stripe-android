package com.stripe.android.paymentsheet.paymentdatacollection.updatedtax

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayDisplayItem
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.format.CurrencyFormatter
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.stripeFormInsets

@Composable
internal fun UpdatedTaxAmountScreen(
    displayItems: List<GooglePayDisplayItem>,
    currency: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(MaterialTheme.stripeFormInsets.getOuterFormInsets()),
    ) {
        UpdatedTaxAmountHeader(onDismiss)
        H4Text(
            text = stringResource(R.string.stripe_paymentsheet_confirm_updated_total),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.stripe_paymentsheet_updated_tax_message),
            style = MaterialTheme.typography.body1,
        )
        Spacer(modifier = Modifier.height(24.dp))
        displayItems.forEachIndexed { index, item ->
            if (index == displayItems.lastIndex) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.label.resolve(context),
                    style = MaterialTheme.typography.body1,
                )
                Text(
                    text = CurrencyFormatter.format(item.price, currency),
                    style = MaterialTheme.typography.body1,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 20.dp)
                .testTag(UPDATED_TAX_AMOUNT_CONFIRM_BUTTON),
            contentAlignment = Alignment.Center,
        ) {
            PrimaryButton(
                label = stringResource(R.string.stripe_paymentsheet_confirm),
                locked = false,
                enabled = true,
                onClick = onConfirm,
            )
        }
    }
}

@Composable
private fun UpdatedTaxAmountHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.offset(16.dp, -8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.stripe_ic_paymentsheet_close),
                contentDescription = null,
            )
        }
    }
}

internal const val UPDATED_TAX_AMOUNT_CONFIRM_BUTTON = "UPDATED_TAX_AMOUNT_CONFIRM_BUTTON"
