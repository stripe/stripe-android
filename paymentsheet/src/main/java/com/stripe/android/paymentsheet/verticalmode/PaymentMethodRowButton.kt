package com.stripe.android.paymentsheet.verticalmode

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.PaymentSheet.Appearance
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.DefaultPaymentMethodLabel
import com.stripe.android.paymentsheet.ui.PaymentMethodIcon
import com.stripe.android.paymentsheet.ui.PromoBadge
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconWidth
import com.stripe.android.uicore.DefaultStripeTheme
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeColors

@Composable
internal fun PaymentMethodRowButton(
    isEnabled: Boolean,
    isSelected: Boolean,
    isClickable: Boolean = isEnabled,
    shouldShowDefaultBadge: Boolean,
    iconContent: @Composable RowScope.() -> Unit,
    title: String,
    subtitle: String?,
    promoText: String?,
    onClick: () -> Unit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    appearance: Appearance.Embedded = Appearance.Embedded(RowStyle.FloatingButton.default),
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val contentPaddingValues = if (subtitle != null) {
        8.dp
    } else {
        12.dp
    }

    RowButtonOuterContent(
        appearance = appearance,
        isEnabled = isEnabled,
        isSelected = isSelected,
        contentPaddingValues = contentPaddingValues,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(
                selected = isSelected,
                enabled = isClickable,
                onClick = onClick
            ),
        trailingContent = trailingContent,
        onClick = onClick
    ) { displayTrailingContent ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(ROW_CONTENT_HORIZONTAL_SPACING.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowButtonInnerContent(
                isEnabled = isEnabled,
                shouldShowDefaultBadge = shouldShowDefaultBadge,
                iconContent = iconContent,
                title = title,
                subtitle = subtitle,
                contentDescription = contentDescription,
                appearance = appearance,
                modifier = if (appearance.style.shouldAddModifierWeight()) {
                    Modifier.weight(1f, fill = true)
                } else {
                    Modifier
                }
            )

            if (promoText != null) {
                PromoBadge(promoText)
            }

            if (trailingContent != null && displayTrailingContent) {
                trailingContent()
            }
        }
    }
}

@Composable
private fun RowButtonOuterContent(
    appearance: Appearance.Embedded,
    isEnabled: Boolean,
    isSelected: Boolean,
    contentPaddingValues: Dp,
    modifier: Modifier,
    trailingContent: @Composable (RowScope.() -> Unit)?,
    onClick: () -> Unit,
    rowContent: @Composable (displayTrailingContent: Boolean) -> Unit
) {
    when (appearance.style) {
        is RowStyle.FloatingButton -> {
            RowButtonFloatingOuterContent(
                isEnabled = isEnabled,
                isSelected = isSelected,
                contentPaddingValues = PaddingValues(
                    horizontal = ROW_CONTENT_HORIZONTAL_SPACING.dp,
                    vertical = contentPaddingValues + appearance.style.additionalInsetsDp.dp
                ),
                modifier = modifier,
            ) {
                rowContent(true)
            }
        }
        is RowStyle.FlatWithCheckmark -> {
            RowButtonCheckmarkOuterContent(
                isSelected = isSelected,
                contentPaddingValues = PaddingValues(
                    horizontal = appearance.style.horizontalInsetsDp.dp,
                    vertical = contentPaddingValues + appearance.style.additionalVerticalInsetsDp.dp
                ),
                trailingContent = trailingContent,
                style = appearance.style,
                modifier = modifier
            ) {
                rowContent(false)
            }
        }
        is RowStyle.FlatWithChevron -> {
            RowButtonChevronOuterContent(
                contentPaddingValues = PaddingValues(
                    horizontal = appearance.style.horizontalInsetsDp.dp,
                    vertical = contentPaddingValues + appearance.style.additionalVerticalInsetsDp.dp
                ),
                trailingContent = trailingContent,
                style = appearance.style,
                modifier = modifier
            ) {
                rowContent(false)
            }
        }
        is RowStyle.FlatWithRadio -> {
            RowButtonRadioOuterContent(
                isEnabled = isEnabled,
                isSelected = isSelected,
                contentPaddingValues = PaddingValues(
                    horizontal = appearance.style.horizontalInsetsDp.dp,
                    vertical = contentPaddingValues + appearance.style.additionalVerticalInsetsDp.dp
                ),
                onClick = onClick,
                style = appearance.style,
                modifier = modifier
            ) {
                rowContent(true)
            }
        }
    }
}

@Composable
private fun RowButtonFloatingOuterContent(
    isEnabled: Boolean,
    isSelected: Boolean,
    contentPaddingValues: PaddingValues,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .then(
                modifier.alpha(alpha = if (isEnabled) 1.0F else 0.6F)
            ),
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.stripeColors.component,
        border = MaterialTheme.getBorderStroke(isSelected),
        elevation = if (isSelected) 1.5.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPaddingValues),
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun RowButtonRadioOuterContent(
    isEnabled: Boolean,
    isSelected: Boolean,
    contentPaddingValues: PaddingValues,
    onClick: () -> Unit,
    modifier: Modifier,
    style: RowStyle.FlatWithRadio,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = style.getColors(isSystemInDarkTheme())
    Row(
        modifier = modifier.padding(contentPaddingValues)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            enabled = isEnabled,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(20.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(colors.selectedColor),
                unselectedColor = Color(colors.unselectedColor)
            )
        )
        Spacer(Modifier.width(ROW_CONTENT_HORIZONTAL_SPACING.dp))
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun RowButtonCheckmarkOuterContent(
    isSelected: Boolean,
    contentPaddingValues: PaddingValues,
    trailingContent: (@Composable RowScope.() -> Unit)?,
    modifier: Modifier,
    style: RowStyle.FlatWithCheckmark,
    content: @Composable ColumnScope.() -> Unit,
) {
    RowButtonWithEndIconOuterContent(
        contentPaddingValues = contentPaddingValues,
        trailingContent = trailingContent,
        modifier = modifier,
        iconContent = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = style.checkmarkInsetDp.dp)
                        .offset(3.dp),
                    tint = Color(style.getColors(isSystemInDarkTheme()).checkmarkColor)
                )
            }
        },
        content = content
    )
}

@Composable
private fun RowButtonChevronOuterContent(
    contentPaddingValues: PaddingValues,
    trailingContent: (@Composable RowScope.() -> Unit)?,
    modifier: Modifier,
    style: RowStyle.FlatWithChevron,
    content: @Composable ColumnScope.() -> Unit,
) {
    RowButtonWithEndIconOuterContent(
        contentPaddingValues = contentPaddingValues,
        trailingContent = trailingContent,
        modifier = modifier,
        iconContent = {
            Icon(
                painter = painterResource(R.drawable.stripe_ic_chevron_right),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                tint = Color(style.getColors(isSystemInDarkTheme()).chevronColor)
            )
        },
        content = content
    )
}

@Composable
private fun RowButtonWithEndIconOuterContent(
    contentPaddingValues: PaddingValues,
    trailingContent: (@Composable RowScope.() -> Unit)?,
    modifier: Modifier,
    iconContent: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier.padding(contentPaddingValues),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            content()
            Row {
                if (trailingContent != null) {
                    Spacer(Modifier.width(iconWidth + ROW_CONTENT_HORIZONTAL_SPACING.dp))
                    trailingContent()
                }
            }
        }
        Spacer(Modifier.weight(1f))
        iconContent()
    }
}

/**
 * Icon, title, and subtitle if provided. Common across all PaymentMethodRowButton configurations
 */
@Composable
private fun RowButtonInnerContent(
    isEnabled: Boolean,
    shouldShowDefaultBadge: Boolean,
    iconContent: @Composable RowScope.() -> Unit,
    title: String,
    subtitle: String?,
    contentDescription: String? = null,
    appearance: Appearance.Embedded,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ROW_CONTENT_HORIZONTAL_SPACING.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        iconContent()

        TitleContent(
            title = title,
            subtitle = subtitle,
            isEnabled = isEnabled,
            contentDescription = contentDescription,
            style = appearance.style
        )

        if (shouldShowDefaultBadge) {
            DefaultPaymentMethodLabel(
                modifier = Modifier
                    .padding(top = 4.dp, end = 6.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun TitleContent(
    title: String,
    subtitle: String?,
    isEnabled: Boolean,
    contentDescription: String?,
    style: RowStyle
) {
    val titleColor = style.getTitleTextColor()
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Medium),
            color = if (isEnabled) titleColor else titleColor.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
        )

        if (subtitle != null) {
            val subtitleTextColor = style.getSubtitleTextColor()
            Text(
                text = subtitle,
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Normal),
                color = if (isEnabled) subtitleTextColor else subtitleTextColor.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
private fun ButtonPreview() {
    DefaultStripeTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PaymentMethodRowButton(
                isEnabled = true,
                isSelected = true,
                shouldShowDefaultBadge = true,
                iconContent = {
                    PaymentMethodIcon(
                        iconRes = com.stripe.android.ui.core.R.drawable.stripe_ic_paymentsheet_pm_card,
                        iconUrl = null,
                        imageLoader = StripeImageLoader(LocalContext.current.applicationContext),
                        iconRequiresTinting = true,
                        modifier = Modifier
                            .height(22.dp)
                            .width(22.dp),
                        contentAlignment = Alignment.Center,
                    )
                },
                title = "•••• 4242",
                subtitle = null,
                promoText = null,
                onClick = {},
                appearance = Appearance.Embedded.default,
                trailingContent = {
                    Text("Edit")
                }
            )
            PaymentMethodRowButton(
                isEnabled = false,
                isSelected = false,
                shouldShowDefaultBadge = false,
                iconContent = {
                    PaymentMethodIcon(
                        iconRes = com.stripe.android.ui.core.R.drawable.stripe_ic_paymentsheet_pm_card,
                        iconUrl = null,
                        imageLoader = StripeImageLoader(LocalContext.current.applicationContext),
                        iconRequiresTinting = true,
                        modifier = Modifier
                            .height(22.dp)
                            .width(22.dp),
                        contentAlignment = Alignment.Center,
                    )
                },
                title = "•••• 4242",
                subtitle = null,
                promoText = null,
                onClick = {},
                appearance = Appearance.Embedded.default,
                trailingContent = {
                    Text("Edit")
                }
            )
        }
    }
}

@Composable
private fun RowStyle.getTitleTextColor() = when (this) {
    is RowStyle.FloatingButton -> MaterialTheme.stripeColors.onComponent
    else -> MaterialTheme.colors.onSurface
}

@Composable
private fun RowStyle.getSubtitleTextColor() = when (this) {
    is RowStyle.FloatingButton -> MaterialTheme.stripeColors.placeholderText
    else -> MaterialTheme.stripeColors.subtitle
}

private fun RowStyle.shouldAddModifierWeight(): Boolean {
    return when (this) {
        is RowStyle.FlatWithCheckmark,
        is RowStyle.FlatWithChevron -> false
        else -> true
    }
}

private const val ROW_CONTENT_HORIZONTAL_SPACING = 12
