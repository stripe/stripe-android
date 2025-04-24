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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.link.theme.MinimumTouchTargetSize
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails.Card
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.getCardBrandIconForVerticalMode
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.R as StripeR
import com.stripe.android.ui.core.R as StripeUiCoreR

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
            val title = paymentDetails.displayName?.resolve() ?: "•••• ${paymentDetails.last4}"
            val subtitle = "•••• ${paymentDetails.last4}"

            CardInfo(
                modifier = modifier,
                title = title,
                subtitle = subtitle.takeIf { it != title },
                icon = paymentDetails.brand.getCardBrandIconForVerticalMode(),
                contentDescription = paymentDetails.brand.displayName
            )
        }
        is ConsumerPaymentDetails.BankAccount -> {
            BankAccountInfo(bankAccount = paymentDetails)
        }
        is ConsumerPaymentDetails.Passthrough -> {
            CardInfo(
                modifier = modifier,
                title = "•••• ${paymentDetails.last4}",
                subtitle = null,
                icon = CardBrand.Unknown.getCardBrandIconForVerticalMode(),
                contentDescription = stringResource(R.string.stripe_wallet_passthrough_description)
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
    contentDescription: String? = null,
) {
    PaymentMethodInfo(
        modifier = modifier,
        iconResource = icon,
        title = title,
        subtitle = subtitle,
        contentDescription = contentDescription,
    )
}

@Composable
private fun RowScope.BankAccountInfo(
    modifier: Modifier = Modifier,
    bankAccount: ConsumerPaymentDetails.BankAccount,
) {
    PaymentMethodInfo(
        modifier = modifier,
        iconResource = R.drawable.stripe_link_bank,
        iconColorFilter = ColorFilter.tint(MaterialTheme.linkColors.actionLabelLight),
        title = bankAccount.displayName?.resolve() ?: stringResource(StripeUiCoreR.string.stripe_payment_method_bank),
        subtitle = "•••• ${bankAccount.last4}",
    )
}

@Composable
private fun RowScope.PaymentMethodInfo(
    iconResource: Int,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    iconColorFilter: ColorFilter? = null,
    contentDescription: String? = null,
) {
    Row(
        modifier = modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(iconResource),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            alignment = Alignment.Center,
            colorFilter = iconColorFilter,
        )

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

internal const val WALLET_PAYMENT_DETAIL_ITEM_RADIO_BUTTON = "wallet_payment_detail_item_radio_button"
internal const val WALLET_PAYMENT_DETAIL_ITEM_MENU_BUTTON = "wallet_payment_detail_item_menu_button"
internal const val WALLET_PAYMENT_DETAIL_ITEM_LOADING_INDICATOR = "wallet_payment_detail_item_loading_indicator"
