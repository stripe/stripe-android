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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.stripe.android.paymentsheet.ui.LpmSelectorText
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
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
    // An attempt was made to not use constraint layout here but it was unsuccessful in
    // precisely positioning the check and delete icons to match the mocks.
    ConstraintLayout(
        modifier = modifier
            .padding(top = 12.dp)
            .width(viewWidth)
            .alpha(alpha = if (isEnabled) 1.0F else 0.6F)
    ) {
        val (checkIcon, deleteIcon, label, card) = createRefs()
        SectionCard(
            isSelected = isSelected,
            modifier = Modifier
                .height(64.dp)
                .padding(horizontal = 6.dp)
                .fillMaxWidth()
                .constrainAs(card) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
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
        if (isSelected) {
            val iconColor = MaterialTheme.colors.primary
            val checkSymbolColor = if (iconColor.shouldUseDarkDynamicColor()) {
                Color.Black
            } else {
                Color.White
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(24.dp)
                    .background(MaterialTheme.colors.primary)
                    .constrainAs(checkIcon) {
                        top.linkTo(card.bottom, (-18).dp)
                        end.linkTo(card.end)
                    }
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = checkSymbolColor,
                    modifier = Modifier
                        .size(12.dp)
                )
            }
        }
        if (isEditing && onRemoveListener != null) {
            val openDialog = remember { mutableStateOf(false) }

            SimpleDialogElementUI(
                openDialog = openDialog,
                titleText = removePmDialogTitle,
                messageText = description,
                confirmText = stringResource(R.string.remove),
                dismissText = stringResource(R.string.cancel),
                onConfirmListener = onRemoveListener
            )

            // tint the delete symbol so it contrasts well with the error color around it.
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
                modifier = Modifier
                    .constrainAs(deleteIcon) {
                        top.linkTo(card.top, margin = (-9).dp)
                        end.linkTo(card.end)
                    }
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color = iconColor)
                    .clickable(
                        onClick = {
                            openDialog.value = true
                        }
                    )
            )
        }

        LpmSelectorText(
            icon = labelIcon,
            text = labelText,
            textColor = MaterialTheme.colors.onSurface,
            isEnabled = isEnabled,
            modifier = Modifier
                .constrainAs(label) {
                    top.linkTo(card.bottom)
                    start.linkTo(card.start)
                }
                .padding(
                    top = 4.dp,
                    start = 6.dp,
                    end = 6.dp
                )
                .semantics {
                    // This makes the screen reader read out numbers digit by digit
                    // one one one one vs one thousand one hundred eleven
                    this.contentDescription =
                        description.replace("\\d".toRegex(), "$0 ")
                }
        )
    }
}
