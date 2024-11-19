package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.link.R
import com.stripe.android.link.model.icon
import com.stripe.android.link.theme.MinimumTouchTargetSize
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails.Card
import com.stripe.android.R as StripeR

@Composable
internal fun PaymentDetailsListItem(
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    enabled: Boolean,
    isSelected: Boolean,
    isUpdating: Boolean,
    onClick: () -> Unit,
    onMenuButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(enabled = enabled, onClick = onClick),
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
                PaymentDetails(paymentDetails = paymentDetails)

                if (paymentDetails.isDefault) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colors.secondary,
                                shape = MaterialTheme.linkShapes.extraSmall
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.stripe_wallet_default),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.linkColors.disabledText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                val showWarning = (paymentDetails as? Card)?.isExpired ?: false
                if (showWarning && !isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.stripe_link_error),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.linkColors.errorText
                    )
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(MinimumTouchTargetSize)
                .padding(end = 12.dp)
        ) {
            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = onMenuButtonClick,
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(StripeR.string.stripe_edit),
                        tint = MaterialTheme.linkColors.actionLabelLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
    TabRowDefaults.Divider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.linkColors.componentDivider,
        thickness = 1.dp
    )
}

@Composable
private fun RowScope.PaymentDetails(
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
) {
    when (paymentDetails) {
        is Card -> {
            CardInfo(
                last4 = paymentDetails.last4,
                icon = paymentDetails.brand.icon,
                contentDescription = paymentDetails.brand.displayName
            )
        }
        is ConsumerPaymentDetails.BankAccount -> {
            BankAccountInfo(bankAccount = paymentDetails)
        }
        is ConsumerPaymentDetails.Passthrough -> {
            CardInfo(
                last4 = paymentDetails.last4,
                icon = R.drawable.stripe_link_bank,
                contentDescription = "Passthrough"
            )
        }
    }
}

@Composable
private fun RowScope.CardInfo(
    last4: String,
    icon: Int,
    contentDescription: String? = null
) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            modifier = Modifier
                .width(38.dp)
                .padding(horizontal = 6.dp),
            alignment = Alignment.Center,
            alpha = LocalContentAlpha.current
        )
        Text(
            text = "•••• ",
            color = MaterialTheme.colors.onPrimary
                .copy(alpha = LocalContentAlpha.current)
        )
        Text(
            text = last4,
            color = MaterialTheme.colors.onPrimary
                .copy(alpha = LocalContentAlpha.current),
            style = MaterialTheme.typography.h6
        )
    }
}

@Composable
private fun RowScope.BankAccountInfo(
    bankAccount: ConsumerPaymentDetails.BankAccount,
) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(bankAccount.icon),
            contentDescription = null,
            modifier = Modifier
                .width(38.dp)
                .padding(horizontal = 6.dp),
            alignment = Alignment.Center,
            alpha = LocalContentAlpha.current,
            colorFilter = ColorFilter.tint(MaterialTheme.linkColors.actionLabelLight)
        )
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = bankAccount.bankName ?: "Bank",
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
