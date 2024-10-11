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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.shouldUseDarkDynamicColor
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors

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

class PaymentOptionTab(
    val label: Label,
    val icon: Icon,
    val state: State,
) {
    data class Label(
        val text: ResolvableString,
        val description: ResolvableString,
        val icon: Icon?,
    )

    data class Icon(
        @DrawableRes val onLight: Int,
        @DrawableRes val onDark: Int,
    )

    sealed interface State {
        class Removable(
            val displayName: ResolvableString,
            val confirmationMessage: ResolvableString,
            val accessibilityDescription: ResolvableString,
            val onRemove: () -> Unit
        ) : State

        class Modifiable(
            val accessibilityDescription: ResolvableString,
            val onModify: () -> Unit
        ) : State

        class Selectable(val onSelect: () -> Unit) : State

        class Selected(val onClick: () -> Unit) : State

        data object Disabled : State
    }
}

@Composable
internal fun SavedPaymentMethodTab(
    tab: PaymentOptionTab,
    viewWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val openRemoveDialog = rememberSaveable { mutableStateOf(false) }

    val state = tab.state
    val enabled = state !is PaymentOptionTab.State.Disabled

    BadgedBox(
        badge = {
            SavedPaymentMethodBadge(
                state = state,
                openRemoveDialog = openRemoveDialog,
            )
        },
        content = {
            Column {
                SavedPaymentMethodCard(tab)

                val label = tab.label
                val description = label.description.resolve()
                val iconResource = label.icon?.let { icon ->
                    if (MaterialTheme.colors.surface.shouldUseDarkDynamicColor()) {
                        icon.onDark
                    } else {
                        icon.onLight
                    }
                }

                LpmSelectorText(
                    icon = iconResource,
                    text = label.text.resolve(),
                    textColor = MaterialTheme.colors.onSurface,
                    isEnabled = enabled,
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
            .alpha(alpha = if (enabled) 1.0F else 0.6F)
    )

    if (state is PaymentOptionTab.State.Removable && openRemoveDialog.value) {
        RemovePaymentMethodDialogUI(
            displayName = state.displayName,
            confirmationMessage = state.confirmationMessage,
            onConfirm = {
                openRemoveDialog.value = false
                state.onRemove()
            },
            onDismiss = { openRemoveDialog.value = false }
        )
    }
}

@Composable
private fun SavedPaymentMethodBadge(
    state: PaymentOptionTab.State,
    openRemoveDialog: MutableState<Boolean>,
) {
    when (state) {
        is PaymentOptionTab.State.Modifiable -> ModifyBadge(
            onModifyAccessibilityDescription = state.accessibilityDescription
                .resolve()
                .readNumbersAsIndividualDigits(),
            onPressed = { state.onModify() },
            modifier = Modifier.offset(x = (-14).dp, y = 1.dp),
        )
        is PaymentOptionTab.State.Removable -> RemoveBadge(
            onRemoveAccessibilityDescription = state.accessibilityDescription
                .resolve()
                .readNumbersAsIndividualDigits(),
            onPressed = { openRemoveDialog.value = true },
            modifier = Modifier.offset(x = (-14).dp, y = 1.dp),
        )
        is PaymentOptionTab.State.Selected -> SelectedBadge(
            modifier = Modifier.offset(x = (-18).dp, y = 58.dp),
        )
        is PaymentOptionTab.State.Selectable -> Unit
        is PaymentOptionTab.State.Disabled -> Unit
    }
}

@Composable
private fun SavedPaymentMethodCard(
    tab: PaymentOptionTab,
) {
    val state = tab.state

    SectionCard(
        isSelected = tab.state is PaymentOptionTab.State.Selected,
        modifier = Modifier
            .height(64.dp)
            .padding(horizontal = 6.dp)
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .testTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_${tab.label.text.resolve(LocalContext.current)}")
                .selectable(
                    selected = state is PaymentOptionTab.State.Selected,
                    enabled = state is PaymentOptionTab.State.Selectable,
                    onClick = {
                        when (state) {
                            is PaymentOptionTab.State.Selectable -> state.onSelect()
                            is PaymentOptionTab.State.Selected -> state.onClick()
                            else -> Unit
                        }
                    },
                ),
        ) {
            val iconResource = if (MaterialTheme.stripeColors.component.shouldUseDarkDynamicColor()) {
                tab.icon.onDark
            } else {
                tab.icon.onLight
            }

            Image(
                painter = painterResource(iconResource),
                contentDescription = null,
                modifier = Modifier
                    .height(40.dp)
                    .width(56.dp)
            )
        }
    }
}

@Composable
private fun RemovePaymentMethodDialogUI(
    displayName: ResolvableString,
    confirmationMessage: ResolvableString,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val removeTitle = stringResource(
        R.string.stripe_paymentsheet_remove_pm,
        displayName.resolve(),
    )

    SimpleDialogElementUI(
        titleText = removeTitle,
        messageText = confirmationMessage.resolve(),
        confirmText = stringResource(com.stripe.android.R.string.stripe_remove),
        dismissText = stringResource(com.stripe.android.R.string.stripe_cancel),
        destructive = true,
        onConfirmListener = onConfirm,
        onDismissListener = onDismiss,
    )
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
            tab = PaymentOptionTab(
                label = PaymentOptionTab.Label(
                    text = resolvableString("MasterCard"),
                    description = resolvableString("MasterCard"),
                    icon = null,
                ),
                icon = PaymentOptionTab.Icon(
                    onDark = R.drawable.stripe_ic_paymentsheet_card_visa,
                    onLight = R.drawable.stripe_ic_paymentsheet_card_visa,
                ),
                state = PaymentOptionTab.State.Selected(
                    onClick = {},
                ),
            ),
            viewWidth = 100.dp,
        )
    }
}

@Preview(name = "Payment option in removable mode")
@Composable
private fun SavedPaymentMethodTabUIRemovable() {
    StripeTheme {
        SavedPaymentMethodTab(
            tab = PaymentOptionTab(
                label = PaymentOptionTab.Label(
                    text = resolvableString("MasterCard"),
                    description = resolvableString("MasterCard"),
                    icon = null,
                ),
                icon = PaymentOptionTab.Icon(
                    onDark = R.drawable.stripe_ic_paymentsheet_card_visa,
                    onLight = R.drawable.stripe_ic_paymentsheet_card_visa,
                ),
                state = PaymentOptionTab.State.Removable(
                    displayName = resolvableString("Card"),
                    accessibilityDescription = resolvableString("Remove this card!"),
                    confirmationMessage = resolvableString("Remove this card!"),
                    onRemove = {},
                ),
            ),
            viewWidth = 100.dp,
        )
    }
}

@Preview(name = "Payment option in modifiable mode")
@Composable
private fun SavedPaymentMethodTabUIModifiable() {
    StripeTheme {
        SavedPaymentMethodTab(
            tab = PaymentOptionTab(
                label = PaymentOptionTab.Label(
                    text = resolvableString("MasterCard"),
                    description = resolvableString("MasterCard"),
                    icon = null,
                ),
                icon = PaymentOptionTab.Icon(
                    onDark = R.drawable.stripe_ic_paymentsheet_card_mastercard,
                    onLight = R.drawable.stripe_ic_paymentsheet_card_mastercard,
                ),
                state = PaymentOptionTab.State.Modifiable(
                    accessibilityDescription = resolvableString("Remove this card!"),
                    onModify = {},
                ),
            ),
            viewWidth = 100.dp,
        )
    }
}
