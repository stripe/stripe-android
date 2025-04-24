package com.stripe.android.link.ui.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.MinimumTouchTargetSize
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails.Card
import com.stripe.android.model.CvcCheck
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.transformBankIconCodeToBankIcon
import com.stripe.android.paymentsheet.ui.getCardBrandIconForVerticalMode
import com.stripe.android.R as StripeR

@Composable
internal fun PaymentDetailsListItem(
    modifier: Modifier = Modifier,
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    enabled: Boolean,
    isSelected: Boolean,
    isUpdating: Boolean,
    onClick: () -> Unit,
    onMenuButtonClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            modifier = Modifier
                .testTag(WALLET_PAYMENT_DETAIL_ITEM_RADIO_BUTTON)
                .padding(start = 20.dp, end = 12.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.linkColors.actionLabelLight,
                unselectedColor = MaterialTheme.linkColors.disabledText
            )
        )

        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaymentDetails(paymentDetails = paymentDetails)

                AnimatedVisibility(paymentDetails.isDefault) {
                    DefaultTag()
                }

                val showWarning = (paymentDetails as? Card)?.isExpired ?: false
                if (showWarning) {
                    Icon(
                        painter = painterResource(R.drawable.stripe_link_error),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.linkColors.errorText
                    )
                }
            }
        }

        MenuAndLoader(
            enabled = enabled,
            isUpdating = isUpdating,
            onMenuButtonClick = onMenuButtonClick
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentDetailsListItemPreview() {
    val card = Card(
        id = "id",
        last4 = "4242",
        isDefault = false,
        expiryYear = 2100,
        expiryMonth = 12,
        brand = CardBrand.Visa,
        cvcCheck = CvcCheck.Pass,
        networks = emptyList(),
        funding = "CREDIT",
        nickname = null,
        billingAddress = null
    )
    DefaultLinkTheme {
        Column {
            PaymentDetailsListItem(
                paymentDetails = card,
                enabled = true,
                isSelected = true,
                isUpdating = false,
                onClick = {},
                onMenuButtonClick = {}
            )
            PaymentDetailsListItem(
                paymentDetails = card.copy(brand = CardBrand.MasterCard, expiryYear = 0),
                enabled = true,
                isSelected = false,
                isUpdating = false,
                onClick = {},
                onMenuButtonClick = {}
            )
            PaymentDetailsListItem(
                paymentDetails = card,
                enabled = false,
                isSelected = false,
                isUpdating = true,
                onClick = {},
                onMenuButtonClick = {}
            )
        }
    }
}

@Composable
private fun MenuAndLoader(
    enabled: Boolean,
    isUpdating: Boolean,
    onMenuButtonClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(MinimumTouchTargetSize)
            .padding(end = 12.dp)
    ) {
        if (isUpdating) {
            CircularProgressIndicator(
                modifier = Modifier
                    .testTag(WALLET_PAYMENT_DETAIL_ITEM_LOADING_INDICATOR)
                    .size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            IconButton(
                modifier = Modifier
                    .testTag(WALLET_PAYMENT_DETAIL_ITEM_MENU_BUTTON),
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

@Composable
private fun DefaultTag() {
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

@Composable
internal fun RowScope.PaymentDetails(
    modifier: Modifier = Modifier,
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
) {
    when (paymentDetails) {
        is Card -> {
            CardInfo(
                modifier = modifier,
                title = paymentDetails.displayName,
                subtitle = "•••• ${paymentDetails.last4}",
                icon = paymentDetails.brand.getCardBrandIconForVerticalMode(),
            )
        }
        is ConsumerPaymentDetails.BankAccount -> {
            BankAccountInfo(bankAccount = paymentDetails)
        }
        is ConsumerPaymentDetails.Passthrough -> {
            CardInfo(
                modifier = modifier,
                title = paymentDetails.displayName,
                subtitle = null,
                icon = CardBrand.Unknown.getCardBrandIconForVerticalMode(),
            )
        }
    }
}

@Composable
private fun RowScope.CardInfo(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    icon: Int,
) {
    PaymentMethodInfo(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        icon = {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    )
}

@Composable
private fun RowScope.BankAccountInfo(
    modifier: Modifier = Modifier,
    bankAccount: ConsumerPaymentDetails.BankAccount,
) {
    PaymentMethodInfo(
        modifier = modifier,
        title = bankAccount.displayName,
        subtitle = "•••• ${bankAccount.last4}",
        icon = {
            BankIcon(bankAccount.bankIconCode)
        }
    )
}

@Composable
private fun RowScope.PaymentMethodInfo(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(24.dp)) {
            icon()
        }

        Column {
            Text(
                text = title,
                color = MaterialTheme.colors.onPrimary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.h6
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colors.onSecondary,
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}

@Composable
private fun BankIcon(
    bankIconCode: String?,
    modifier: Modifier = Modifier
) {
    val icon = remember(bankIconCode) {
        transformBankIconCodeToBankIcon(
            iconCode = bankIconCode,
            fallbackIcon = R.drawable.stripe_link_bank_outlined,
        )
    }

    val isGenericIcon = icon == R.drawable.stripe_link_bank_outlined

    val containerModifier = if (isGenericIcon) {
        modifier
            .background(
                color = MaterialTheme.linkColors.componentBorder,
                shape = RoundedCornerShape(3.dp),
            )
            .padding(4.dp)
    } else {
        modifier
    }

    Box(modifier = containerModifier) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = if (isGenericIcon) {
                ColorFilter.tint(MaterialTheme.colors.onSecondary)
            } else {
                null
            },
        )
    }
}

internal const val WALLET_PAYMENT_DETAIL_ITEM_RADIO_BUTTON = "wallet_payment_detail_item_radio_button"
internal const val WALLET_PAYMENT_DETAIL_ITEM_MENU_BUTTON = "wallet_payment_detail_item_menu_button"
internal const val WALLET_PAYMENT_DETAIL_ITEM_LOADING_INDICATOR = "wallet_payment_detail_item_loading_indicator"
