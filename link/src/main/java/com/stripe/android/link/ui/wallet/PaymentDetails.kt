package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.link.R
import com.stripe.android.link.model.icon
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.ErrorTextStyle
import com.stripe.android.model.ConsumerPaymentDetails

@Composable
internal fun PaymentDetailsListItem(
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    enabled: Boolean,
    isSupported: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onMenuButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(enabled = enabled && isSupported, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            modifier = Modifier.padding(start = 20.dp, end = 6.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.linkColors.actionLabelLight,
                unselectedColor = MaterialTheme.linkColors.disabledText
            )
        )
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaymentDetails(paymentDetails = paymentDetails, enabled = isSupported)
                Spacer(modifier = Modifier.weight(1f))
                if (paymentDetails.isDefault) {
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .background(
                                color = MaterialTheme.colors.secondary,
                                shape = MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.wallet_default),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.linkColors.disabledText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            if (!isSupported) {
                ErrorText(
                    text = stringResource(id = R.string.wallet_unavailable),
                    style = ErrorTextStyle.Small,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)
                )
            }
        }
        IconButton(
            onClick = onMenuButtonClick,
            modifier = Modifier.padding(end = 6.dp),
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.edit),
                tint = MaterialTheme.linkColors.actionLabelLight
            )
        }
    }
    TabRowDefaults.Divider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.linkColors.componentDivider,
        thickness = 1.dp
    )
}

@Composable
internal fun PaymentDetails(
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    enabled: Boolean
) {
    when (paymentDetails) {
        is ConsumerPaymentDetails.Card -> {
            CardInfo(card = paymentDetails, enabled = enabled)
        }
        is ConsumerPaymentDetails.BankAccount -> {
            BankAccountInfo(bankAccount = paymentDetails, enabled = enabled)
        }
    }
}

@Composable
internal fun CardInfo(
    card: ConsumerPaymentDetails.Card,
    enabled: Boolean
) {
    CompositionLocalProvider(LocalContentAlpha provides if (enabled) 1f else 0.6f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = card.brand.icon),
                contentDescription = card.brand.displayName,
                modifier = Modifier
                    .width(38.dp)
                    .padding(horizontal = 6.dp),
                alpha = LocalContentAlpha.current
            )
            Text(
                text = "•••• ",
                color = MaterialTheme.colors.onPrimary
                    .copy(alpha = LocalContentAlpha.current)
            )
            Text(
                text = card.last4,
                color = MaterialTheme.colors.onPrimary
                    .copy(alpha = LocalContentAlpha.current),
                style = MaterialTheme.typography.h6
            )
        }
    }
}

@Composable
internal fun BankAccountInfo(
    bankAccount: ConsumerPaymentDetails.BankAccount,
    enabled: Boolean
) {
    CompositionLocalProvider(LocalContentAlpha provides if (enabled) 1f else 0.6f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(bankAccount.icon),
                contentDescription = null,
                modifier = Modifier
                    .width(38.dp)
                    .padding(horizontal = 6.dp),
                tint = MaterialTheme.linkColors.actionLabelLight
                    .copy(alpha = LocalContentAlpha.current)
            )
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = bankAccount.bankName,
                    color = MaterialTheme.colors.onPrimary
                        .copy(alpha = LocalContentAlpha.current),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.h6
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "•••• ",
                        color = MaterialTheme.colors.onSecondary
                            .copy(alpha = LocalContentAlpha.current),
                        style = MaterialTheme.typography.body2
                    )
                    Text(
                        text = bankAccount.last4,
                        color = MaterialTheme.colors.onSecondary
                            .copy(alpha = LocalContentAlpha.current),
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}
