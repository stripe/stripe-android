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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.LinkThemeConfig.radioButtonColors
import com.stripe.android.link.theme.MinimumTouchTargetSize
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.ErrorTextStyle
import com.stripe.android.link.ui.LinkSpinner
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails.BankAccount
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
    isClickable: Boolean,
    isMenuButtonClickable: Boolean,
    isAvailable: Boolean,
    isSelected: Boolean,
    isUpdating: Boolean,
    onClick: () -> Unit,
    onMenuButtonClick: () -> Unit
) {
    // Using a `Layout` in order to achieve the following:
    //  1. main content (radio button, description, menu/loader) centered vertically
    //  2. error below the main content
    //  3. error start-aligned with description
    // without knowing the dimensions of the radio button on the first layout pass.
    Layout(
        modifier = modifier
            .padding(top = 8.dp, bottom = 8.dp, start = 20.dp)
            .clickable(enabled = isClickable, onClick = onClick),
        content = {
            RadioButton(
                selected = isSelected,
                onClick = null,
                modifier = Modifier
                    .testTag(WALLET_PAYMENT_DETAIL_ITEM_RADIO_BUTTON)
                    .padding(end = 12.dp),
                colors = LinkTheme.colors.radioButtonColors
            )

            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaymentDetails(
                    modifier = Modifier.alpha(if (isAvailable) 1f else 0.5f),
                    paymentDetails = paymentDetails
                )

                AnimatedVisibility(paymentDetails.isDefault) {
                    DefaultTag()
                }

                val showWarning = (paymentDetails as? Card)?.isExpired == true
                if (showWarning) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.stripe_ic_sail_warning_circle),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = LinkTheme.colors.iconCritical
                    )
                }
            }

            MenuAndLoader(
                enabled = isMenuButtonClickable,
                isUpdating = isUpdating,
                onMenuButtonClick = onMenuButtonClick
            )

            if (!isAvailable) {
                ErrorText(
                    text = stringResource(R.string.stripe_wallet_unavailable),
                    style = ErrorTextStyle.Small
                )
            }
        }
    ) { measurable, constraints ->
        val componentConstraints = constraints.copy(minHeight = 0)

        // Measure each component.
        val radioButton = measurable[0].measure(componentConstraints)
        val menuAndLoader = measurable[2].measure(componentConstraints)
        val descriptionWidth = constraints.maxWidth - radioButton.width - menuAndLoader.width
        val description = measurable[1].measure(
            componentConstraints.copy(
                minWidth = descriptionWidth,
                maxWidth = descriptionWidth,
            )
        )

        @Suppress("MagicNumber")
        val errorText = measurable.getOrNull(3)?.measure(componentConstraints)

        // Calculate heights.
        val topRowHeight = maxOf(
            radioButton.height,
            description.height,
            menuAndLoader.height
        )
        val fullHeight = maxOf(
            constraints.minHeight,
            topRowHeight + (errorText?.height ?: 0)
        )

        layout(constraints.maxWidth, fullHeight) {
            // Place top row.
            var x = 0
            radioButton.placeRelative(x, (topRowHeight - radioButton.height) / 2)
            x += radioButton.width
            description.placeRelative(x, (topRowHeight - description.height) / 2)
            x += description.width
            menuAndLoader.placeRelative(x, (topRowHeight - menuAndLoader.height) / 2)

            // Place error text start-aligned and below the description.
            errorText?.placeRelative(radioButton.width, topRowHeight)
        }
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
    val bank = BankAccount(
        id = "bank_id",
        last4 = "1234",
        isDefault = false,
        bankIconCode = null,
        nickname = null,
        bankName = "Bank of America",
    )
    DefaultLinkTheme {
        Column {
            PaymentDetailsListItem(
                paymentDetails = card.copy(isDefault = true),
                isClickable = true,
                isMenuButtonClickable = true,
                isSelected = true,
                isAvailable = true,
                isUpdating = false,
                onClick = {},
                onMenuButtonClick = {}
            )
            PaymentDetailsListItem(
                paymentDetails = card.copy(isDefault = true, brand = CardBrand.MasterCard, expiryYear = 0),
                isClickable = true,
                isMenuButtonClickable = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = false,
                onClick = {},
                onMenuButtonClick = {}
            )
            PaymentDetailsListItem(
                paymentDetails = card,
                isClickable = false,
                isMenuButtonClickable = true,
                isSelected = false,
                isAvailable = false,
                isUpdating = true,
                onClick = {},
                onMenuButtonClick = {}
            )
            PaymentDetailsListItem(
                paymentDetails = bank,
                isClickable = false,
                isMenuButtonClickable = true,
                isSelected = false,
                isAvailable = false,
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
            LinkSpinner(
                modifier = Modifier
                    .testTag(WALLET_PAYMENT_DETAIL_ITEM_LOADING_INDICATOR)
                    .size(24.dp),
                strokeWidth = 4.dp,
                filledColor = LinkTheme.colors.iconPrimary,
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
                    tint = LinkTheme.colors.iconTertiary,
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
                color = LinkTheme.colors.surfaceTertiary,
                shape = LinkTheme.shapes.extraSmall
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.stripe_wallet_default),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            color = LinkTheme.colors.textTertiary,
            style = LinkTheme.typography.caption,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
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
        is BankAccount -> {
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
    bankAccount: BankAccount,
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
                color = LinkTheme.colors.textPrimary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = LinkTheme.typography.bodyEmphasized,
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = LinkTheme.colors.textTertiary,
                    style = LinkTheme.typography.detail,
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
                color = LinkTheme.colors.surfaceTertiary,
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
                ColorFilter.tint(LinkTheme.colors.textTertiary)
            } else {
                null
            },
        )
    }
}

internal const val WALLET_PAYMENT_DETAIL_ITEM_RADIO_BUTTON = "wallet_payment_detail_item_radio_button"
internal const val WALLET_PAYMENT_DETAIL_ITEM_MENU_BUTTON = "wallet_payment_detail_item_menu_button"
internal const val WALLET_PAYMENT_DETAIL_ITEM_LOADING_INDICATOR = "wallet_payment_detail_item_loading_indicator"
