@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.paymentsheet.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.shouldUseDarkDynamicColor

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val SAVED_PAYMENT_METHOD_CARD_TEST_TAG = "SAVED_PAYMENT_METHOD_CARD_TEST_TAG"

internal const val TEST_TAG_MODIFY_BADGE = "modify_badge"

private const val EDIT_ICON_SCALE = 0.9f
private val editIconColorLight = Color(0x99000000)
private val editIconColorDark = Color.White
private val editIconBackgroundColorLight = Color(0xFFE5E5EA)
private val editIconBackgroundColorDark = Color(0xFF525252)

// We use internal content padding to make sure that we have space to display
// the remove badge on the payment method card.
internal val SavedPaymentMethodsTopContentPadding = 12.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SavedPaymentMethodTab(
    modifier: Modifier = Modifier,
    viewWidth: Dp,
    isSelected: Boolean,
    shouldShowModifyBadge: Boolean,
    shouldShowDefaultBadge: Boolean,
    isEnabled: Boolean,
    isClickable: Boolean = isEnabled,
    iconRes: Int,
    iconTint: Color? = null,
    @DrawableRes labelIcon: Int? = null,
    labelText: String = "",
    description: String,
    onModifyListener: (() -> Unit)? = null,
    onModifyAccessibilityDescription: String = "",
    onItemSelectedListener: () -> Unit,
) {
    BadgedBox(
        badge = {
            SavedPaymentMethodBadge(
                isSelected = isSelected,
                shouldShowModifyBadge = shouldShowModifyBadge,
                onModifyListener = onModifyListener,
                onModifyAccessibilityDescription = onModifyAccessibilityDescription
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .testTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_$labelText")
                    .selectable(
                        selected = isSelected,
                        enabled = isClickable,
                        onClick = onItemSelectedListener,
                    )
                    .semantics {
                        if (!isClickable) {
                            // This shouldn't be visible for accessibility purposes
                            // due to it not being clickable, the user should be
                            // interacting with the badge instead
                            invisibleToUser()
                        }
                    },
            ) {
                SavedPaymentMethodCard(
                    isSelected = isSelected,
                    iconRes = iconRes,
                    iconTint = iconTint,
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

                if (shouldShowDefaultBadge) {
                    DefaultPaymentMethodLabel(
                        modifier = Modifier
                            .padding(top = 4.dp, start = 6.dp, end = 6.dp)
                    )
                }
            }
        },
        modifier = modifier
            .padding(top = SavedPaymentMethodsTopContentPadding)
            .requiredWidth(viewWidth)
            .alpha(alpha = if (isEnabled) 1.0F else 0.6F)
    )
}

@Composable
private fun SavedPaymentMethodBadge(
    isSelected: Boolean,
    shouldShowModifyBadge: Boolean,
    onModifyListener: (() -> Unit)? = null,
    onModifyAccessibilityDescription: String = ""
) {
    if (shouldShowModifyBadge) {
        ModifyBadge(
            onModifyAccessibilityDescription = onModifyAccessibilityDescription,
            onPressed = { onModifyListener?.invoke() },
            modifier = Modifier.offset(x = (-14).dp, y = 1.dp).focusable(),
        )
    } else if (isSelected) {
        SelectedBadge(
            modifier = Modifier.offset(x = (-18).dp, y = 58.dp),
        )
    }
}

@Composable
private fun SavedPaymentMethodCard(
    isSelected: Boolean,
    iconRes: Int,
    iconTint: Color?,
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
@Preview(name = "Selected payment option", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SavedPaymentMethodTabUISelected() {
    StripeTheme {
        SavedPaymentMethodTab(
            viewWidth = 100.dp,
            isSelected = true,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = true,
            iconRes = R.drawable.stripe_ic_paymentsheet_card_visa_ref,
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
            shouldShowModifyBadge = true,
            shouldShowDefaultBadge = false,
            isEnabled = true,
            iconRes = R.drawable.stripe_ic_paymentsheet_card_visa_ref,
            labelText = "MasterCard",
            description = "MasterCard",
            onItemSelectedListener = {},
        )
    }
}

@Preview(name = "Default Payment option in modifiable mode")
@Composable
private fun DefaultSavedPaymentMethodTabUIModifiable() {
    StripeTheme {
        SavedPaymentMethodTab(
            viewWidth = 100.dp,
            isSelected = false,
            shouldShowModifyBadge = true,
            shouldShowDefaultBadge = true,
            isEnabled = true,
            iconRes = R.drawable.stripe_ic_paymentsheet_card_visa_ref,
            labelText = "MasterCard",
            description = "MasterCard",
            onItemSelectedListener = {},
        )
    }
}
