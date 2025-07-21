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
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet.Appearance
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle
import com.stripe.android.paymentsheet.toTextStyle
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
    val defaultPadding = if (subtitle != null) {
        8.dp
    } else {
        12.dp
    }

    val contentPaddingValues = appearance.getPaddingValues(defaultPadding)

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
    contentPaddingValues: PaddingValues,
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
                contentPaddingValues = contentPaddingValues,
                modifier = modifier,
            ) {
                rowContent(true)
            }
        }
        is RowStyle.FlatWithCheckmark -> {
            RowButtonCheckmarkOuterContent(
                isSelected = isSelected,
                contentPaddingValues = contentPaddingValues,
                trailingContent = trailingContent,
                style = appearance.style,
                modifier = modifier
            ) {
                rowContent(false)
            }
        }
        is RowStyle.FlatWithDisclosure -> {
            RowButtonDisclosureOuterContent(

                contentPaddingValues = contentPaddingValues,
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
                contentPaddingValues = contentPaddingValues,
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
private fun RowButtonDisclosureOuterContent(
    contentPaddingValues: PaddingValues,
    trailingContent: (@Composable RowScope.() -> Unit)?,
    modifier: Modifier,
    style: RowStyle.FlatWithDisclosure,
    content: @Composable ColumnScope.() -> Unit,
) {
    RowButtonWithEndIconOuterContent(
        contentPaddingValues = contentPaddingValues,
        trailingContent = trailingContent,
        modifier = modifier,
        iconContent = {
            Icon(
                painter = painterResource(style.disclosureIconRes),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                tint = Color(style.getColors(isSystemInDarkTheme()).disclosureColor)
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
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Spacer(Modifier.size(appearance.paymentMethodIconMargins?.startDp?.dp ?: 0.dp))
        iconContent()
        Spacer(Modifier.size(appearance.paymentMethodIconMargins?.endDp?.dp ?: ROW_CONTENT_HORIZONTAL_SPACING.dp))

        TitleContent(
            title = title,
            subtitle = subtitle,
            isEnabled = isEnabled,
            contentDescription = contentDescription,
            appearance = appearance
        )

        if (shouldShowDefaultBadge) {
            DefaultPaymentMethodLabel(
                modifier = Modifier
                    .padding(start = ROW_CONTENT_HORIZONTAL_SPACING.dp, top = 4.dp, end = 6.dp, bottom = 4.dp)
            )
        }
    }
}

@OptIn(AppearanceAPIAdditionsPreview::class)
@Composable
private fun TitleContent(
    title: String,
    subtitle: String?,
    isEnabled: Boolean,
    contentDescription: String?,
    appearance: Appearance.Embedded,
) {
    val titleColor = appearance.style.getTitleTextColor()
    val textStyle = if (appearance.titleFont != null) {
        appearance.titleFont.toTextStyle()
    } else {
        MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Medium)
    }
    Column {
        Text(
            text = title,
            style = textStyle,
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
            val subtitleTextColor = appearance.style.getSubtitleTextColor()
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
        is RowStyle.FlatWithDisclosure -> false
        else -> true
    }
}

private fun Appearance.Embedded.getPaddingValues(defaultPadding: Dp): PaddingValues {
    return PaddingValues(
        start = style.getHorizontalInsets(),
        top = style.getVerticalInsets() + defaultPadding + (paymentMethodIconMargins?.topDp?.dp ?: 0.dp),
        end = style.getHorizontalInsets(),
        bottom = style.getVerticalInsets() + defaultPadding + (paymentMethodIconMargins?.bottomDp?.dp ?: 0.dp)
    )
}

private fun RowStyle.getVerticalInsets(): Dp = when (this) {
    is RowStyle.FloatingButton -> additionalInsetsDp.dp
    is RowStyle.FlatWithCheckmark -> additionalVerticalInsetsDp.dp
    is RowStyle.FlatWithDisclosure -> additionalVerticalInsetsDp.dp
    is RowStyle.FlatWithRadio -> additionalVerticalInsetsDp.dp
}

private fun RowStyle.getHorizontalInsets(): Dp = when (this) {
    is RowStyle.FloatingButton -> ROW_CONTENT_HORIZONTAL_SPACING.dp
    is RowStyle.FlatWithCheckmark -> horizontalInsetsDp.dp
    is RowStyle.FlatWithDisclosure -> horizontalInsetsDp.dp
    is RowStyle.FlatWithRadio -> horizontalInsetsDp.dp
}

private const val ROW_CONTENT_HORIZONTAL_SPACING = 12
