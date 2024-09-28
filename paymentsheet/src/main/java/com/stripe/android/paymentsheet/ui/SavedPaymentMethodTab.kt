@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.paymentsheet.ui

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.shouldUseDarkDynamicColor

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val SAVED_PAYMENT_METHOD_CARD_TEST_TAG = "SAVED_PAYMENT_METHOD_CARD_TEST_TAG"

internal const val TEST_TAG_REMOVE_BADGE = "remove_badge"
internal const val TEST_TAG_MODIFY_BADGE = "modify_badge"

private const val EDIT_ICON_SCALE = 0.9f
private val editIconColorLight = Color(0x99000000)
private val editIconColorDark = Color.White
private val editIconBackgroundColorLight = Color(0xFFE5E5EA)
private val editIconBackgroundColorDark = Color(0xFF525252)

// We use internal content padding to make sure that we have space to display
// the remove badge on the payment method card.
internal val SavedPaymentMethodsTopContentPadding = 12.dp

@Composable
internal fun SavedPaymentMethodTab(
    viewWidth: Dp,
    isSelected: Boolean,
    editState: PaymentOptionEditState,
    isEnabled: Boolean,
    isClickable: Boolean = isEnabled,
    iconRes: Int,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
    @DrawableRes labelIcon: Int? = null,
    labelText: String = "",
    paymentMethod: DisplayableSavedPaymentMethod? = null,
    description: String,
    shouldOpenRemoveDialog: Boolean = false,
    onRemoveListener: (() -> Unit)? = null,
    onModifyListener: (() -> Unit)? = null,
    onRemoveAccessibilityDescription: String = "",
    onModifyAccessibilityDescription: String = "",
    onItemSelectedListener: (() -> Unit),
) {
    val openRemoveDialog = rememberSaveable { mutableStateOf(shouldOpenRemoveDialog) }

    BadgedBox(
        badge = {
            SavedPaymentMethodBadge(
                isSelected = isSelected,
                editState = editState,
                openRemoveDialog = openRemoveDialog,
                onModifyListener = onModifyListener,
                onRemoveAccessibilityDescription = onRemoveAccessibilityDescription,
                onModifyAccessibilityDescription = onModifyAccessibilityDescription
            )
        },
        content = {
            Column {
                SavedPaymentMethodCard(
                    isSelected = isSelected,
                    isClickable = isClickable,
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
                            contentDescription = description.readNumbersAsIndividualDigits()
                        }
                )
            }
        },
        modifier = modifier
            .padding(top = SavedPaymentMethodsTopContentPadding)
            .requiredWidth(viewWidth)
            .alpha(alpha = if (isEnabled) 1.0F else 0.6F)
    )

    val displayRemoveDialog = openRemoveDialog.value && editState == PaymentOptionEditState.Removable
    if (displayRemoveDialog && onRemoveListener != null && paymentMethod != null) {
        RemovePaymentMethodDialogUI(
            paymentMethod = paymentMethod,
            onConfirmListener = {
                openRemoveDialog.value = false
                onRemoveListener()
            },
            onDismissListener = { openRemoveDialog.value = false }
        )
    }
}

@Composable
private fun SavedPaymentMethodBadge(
    isSelected: Boolean,
    editState: PaymentOptionEditState,
    openRemoveDialog: MutableState<Boolean>,
    onModifyListener: (() -> Unit)? = null,
    onRemoveAccessibilityDescription: String = "",
    onModifyAccessibilityDescription: String = ""
) {
    when (editState) {
        PaymentOptionEditState.Modifiable -> ModifyBadge(
            onModifyAccessibilityDescription = onModifyAccessibilityDescription,
            onPressed = { onModifyListener?.invoke() },
            modifier = Modifier.offset(x = (-14).dp, y = 1.dp),
        )
        PaymentOptionEditState.Removable -> RemoveBadge(
            onRemoveAccessibilityDescription = onRemoveAccessibilityDescription,
            onPressed = { openRemoveDialog.value = true },
            modifier = Modifier.offset(x = (-14).dp, y = 1.dp),
        )
        PaymentOptionEditState.None -> Unit
    }

    if (isSelected) {
        SelectedBadge(
            modifier = Modifier.offset(x = (-18).dp, y = 58.dp),
        )
    }
}

@Composable
private fun SavedPaymentMethodCard(
    isSelected: Boolean,
    isClickable: Boolean,
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
                .testTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_$labelText")
                .selectable(
                    selected = isSelected,
                    enabled = isClickable,
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
            .clickable(onClick = onPressed)
            .testTag(TEST_TAG_REMOVE_BADGE),
    )
}

@Composable
private fun ModifyBadge(
    onModifyAccessibilityDescription: String,
    onPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shouldUseDarkColor = MaterialTheme.colors.background.shouldUseDarkDynamicColor()

    val backgroundColor = if (shouldUseDarkColor) {
        editIconBackgroundColorLight
    } else {
        editIconBackgroundColorDark
    }

    val iconColor = if (shouldUseDarkColor) {
        editIconColorLight
    } else {
        editIconColorDark
    }

    Image(
        painter = painterResource(R.drawable.stripe_ic_edit_symbol),
        contentDescription = onModifyAccessibilityDescription,
        colorFilter = ColorFilter.tint(iconColor),
        contentScale = FixedScale(EDIT_ICON_SCALE),
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color = backgroundColor)
            .clickable(onClick = onPressed)
            .testTag(TEST_TAG_MODIFY_BADGE),
    )
}

@Preview(name = "Selected payment option")
@Composable
private fun SavedPaymentMethodTabUISelected() {
    StripeTheme {
        SavedPaymentMethodTab(
            viewWidth = 100.dp,
            isSelected = true,
            editState = PaymentOptionEditState.None,
            isEnabled = true,
            iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
            labelText = "MasterCard",
            description = "MasterCard",
            onItemSelectedListener = {},
        )
    }
}

@Preview(name = "Payment option in removable mode")
@Composable
private fun SavedPaymentMethodTabUIRemovable() {
    StripeTheme {
        SavedPaymentMethodTab(
            viewWidth = 100.dp,
            isSelected = false,
            editState = PaymentOptionEditState.Removable,
            isEnabled = true,
            iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
            labelText = "MasterCard",
            description = "MasterCard",
            onItemSelectedListener = {},
        )
    }
}

@Preview(name = "Payment option in modifiable mode")
@Composable
private fun SavedPaymentMethodTabUIModifiable() {
    StripeTheme {
        SavedPaymentMethodTab(
            viewWidth = 100.dp,
            isSelected = false,
            editState = PaymentOptionEditState.Modifiable,
            isEnabled = true,
            iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
            labelText = "MasterCard",
            description = "MasterCard",
            onItemSelectedListener = {},
        )
    }
}
