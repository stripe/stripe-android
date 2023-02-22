package com.stripe.android.paymentsheet

import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BadgedBox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.LpmSelectorText
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.shouldUseDarkDynamicColor

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val PAYMENT_OPTION_CARD_TEST_TAG = "PAYMENT_OPTION_CARD_TEST_TAG"

@Composable
internal fun PaymentOptionUi(
    viewWidth: Dp,
    isSelected: Boolean,
    isEditing: Boolean,
    isEnabled: Boolean,
    iconRes: Int,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
    @DrawableRes labelIcon: Int? = null,
    labelText: String = "",
    removePmDialogTitle: String = "",
    description: String,
    onRemoveListener: (() -> Unit)? = null,
    onRemoveAccessibilityDescription: String = "",
    onItemSelectedListener: (() -> Unit),
) {
    val openRemoveDialog = rememberSaveable { mutableStateOf(false) }

    BadgedBox(
        badge = {
            if (isEditing) {
                RemoveBadge(
                    onRemoveAccessibilityDescription = onRemoveAccessibilityDescription,
                    onPressed = { openRemoveDialog.value = true },
                    modifier = Modifier.offset(x = (-14).dp, y = 1.dp),
                )
            }

            if (isSelected) {
                SelectedBadge(
                    modifier = Modifier.offset(x = (-18).dp, y = 58.dp),
                )
            }
        },
        content = {
            Column {
                PaymentOptionCard(
                    isSelected = isSelected,
                    isEnabled = isEnabled,
                    labelText = labelText,
                    iconRes = iconRes,
                    iconTint = iconTint,
                    onItemSelectedListener = onItemSelectedListener,
                )

                LpmSelectorText(
                    icon = labelIcon,
                    text = labelText,
                    textColor = MaterialTheme.colors.onSurface,
                    isEnabled = isEnabled,
                    modifier = Modifier
                        .padding(top = 4.dp, start = 6.dp, end = 6.dp)
                        .semantics {
                            // This makes the screen reader read out numbers digit by digit
                            // one one one one vs one thousand one hundred eleven
                            this.contentDescription =
                                description.replace("\\d".toRegex(), "$0 ")
                        }
                )
            }
        },
        modifier = modifier
            .padding(top = 12.dp)
            .requiredWidth(viewWidth)
            .alpha(alpha = if (isEnabled) 1.0F else 0.6F)
    )

    if (isEditing && onRemoveListener != null) {
        SimpleDialogElementUI(
            openDialog = openRemoveDialog,
            titleText = removePmDialogTitle,
            messageText = description,
            confirmText = stringResource(R.string.remove),
            dismissText = stringResource(R.string.cancel),
            onConfirmListener = onRemoveListener
        )
    }
}

@Composable
private fun PaymentOptionCard(
    isSelected: Boolean,
    isEnabled: Boolean,
    iconRes: Int,
    iconTint: Color?,
    labelText: String,
    onItemSelectedListener: (() -> Unit),
    modifier: Modifier = Modifier,
) {
    SectionCard(
        isSelected = isSelected,
        modifier = modifier
            .height(64.dp)
            .padding(horizontal = 6.dp)
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .testTag("${PAYMENT_OPTION_CARD_TEST_TAG}_$labelText")
                .selectable(
                    selected = isSelected,
                    enabled = isEnabled,
                    onClick = onItemSelectedListener,
                ),
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = iconTint?.let { ColorFilter.tint(it) },
                modifier = Modifier
                    .height(40.dp)
                    .width(56.dp)
            )
        }
    }
}

@Composable
private fun RemoveBadge(
    onRemoveAccessibilityDescription: String,
    onPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconColor = MaterialTheme.colors.error

    val deleteIconColor = if (iconColor.shouldUseDarkDynamicColor()) {
        Color.Black
    } else {
        Color.White
    }

    Image(
        painter = painterResource(R.drawable.stripe_ic_delete_symbol),
        contentDescription = onRemoveAccessibilityDescription,
        colorFilter = ColorFilter.tint(deleteIconColor),
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color = iconColor)
            .clickable(onClick = onPressed),
    )
}

@Composable
private fun SelectedBadge(
    modifier: Modifier = Modifier,
) {
    val iconColor = MaterialTheme.colors.primary
    val checkSymbolColor = if (iconColor.shouldUseDarkDynamicColor()) {
        Color.Black
    } else {
        Color.White
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .size(24.dp)
            .background(MaterialTheme.colors.primary)
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = checkSymbolColor,
            modifier = Modifier.size(12.dp),
        )
    }
}

@Preview(name = "Selected payment option")
@Composable
private fun PaymentOptionUi_Selected() {
    StripeTheme {
        PaymentOptionUi(
            viewWidth = 100.dp,
            isSelected = true,
            isEditing = false,
            isEnabled = true,
            iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
            labelText = "MasterCard",
            description = "MasterCard",
            onItemSelectedListener = {},
        )
    }
}

@Preview(name = "Payment option in editing mode")
@Composable
private fun PaymentOptionUi_Editing() {
    StripeTheme {
        PaymentOptionUi(
            viewWidth = 100.dp,
            isSelected = false,
            isEditing = true,
            isEnabled = true,
            iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
            labelText = "MasterCard",
            description = "MasterCard",
            onItemSelectedListener = {},
        )
    }
}
