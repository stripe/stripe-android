package com.stripe.android.paymentsheet.verticalmode

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle
import com.stripe.android.paymentsheet.ui.DefaultPaymentMethodLabel
import com.stripe.android.paymentsheet.ui.PaymentMethodIcon
import com.stripe.android.paymentsheet.ui.PromoBadge
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconWidth
import com.stripe.android.uicore.DefaultStripeTheme
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeColors

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
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
    style: RowStyle = RowStyle.FloatingButton.default,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val contentPaddingValues = if (subtitle != null) {
        8.dp
    } else {
        12.dp
    }

    RowButtonOuterContent(
        style = style,
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowButtonInnerContent(isEnabled, shouldShowDefaultBadge, iconContent, title, subtitle, contentDescription)

            if (style !is RowStyle.FlatWithCheckmark) {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (promoText != null) {
                PromoBadge(promoText)
            }

            if (trailingContent != null && displayTrailingContent) {
                trailingContent()
            }
        }
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun RowButtonOuterContent(
    style: RowStyle,
    isEnabled: Boolean,
    isSelected: Boolean,
    contentPaddingValues: Dp,
    modifier: Modifier,
    trailingContent: @Composable (RowScope.() -> Unit)?,
    onClick: () -> Unit,
    rowContent: @Composable (displayTrailingContent: Boolean) -> Unit
) {
    when (style) {
        is RowStyle.FloatingButton -> {
            RowButtonFloatingOuterContent(
                isEnabled = isEnabled,
                isSelected = isSelected,
                contentPaddingValues = PaddingValues(
                    horizontal = 12.dp,
                    vertical = contentPaddingValues + style.additionalInsetsDp.dp
                ),
                verticalArrangement = Arrangement.Center,
                modifier = modifier,
            ) {
                rowContent(true)
            }
        }
        is RowStyle.FlatWithCheckmark -> {
            RowButtonCheckmarkOuterContent(
                isSelected = isSelected,
                contentPaddingValues = PaddingValues(
                    horizontal = 0.dp,
                    vertical = contentPaddingValues + style.additionalVerticalInsetsDp.dp
                ),
                verticalArrangement = Arrangement.Center,
                trailingContent = trailingContent,
                style = style,
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
                    horizontal = 0.dp,
                    vertical = contentPaddingValues + style.additionalVerticalInsetsDp.dp
                ),
                verticalArrangement = Arrangement.Center,
                onClick = onClick,
                style = style,
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
    verticalArrangement: Arrangement.Vertical,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .alpha(alpha = if (isEnabled) 1.0F else 0.6F),
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.stripeColors.component,
        border = MaterialTheme.getBorderStroke(isSelected),
        elevation = if (isSelected) 1.5.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPaddingValues),
            verticalArrangement = verticalArrangement,
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun RowButtonRadioOuterContent(
    isEnabled: Boolean,
    isSelected: Boolean,
    contentPaddingValues: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    onClick: () -> Unit,
    modifier: Modifier,
    style: RowStyle.FlatWithRadio,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.stripeColors.component)
            .padding(horizontal = style.additionalHorizontalInsetsDp.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            enabled = isEnabled,
            modifier = Modifier.align(Alignment.CenterVertically).size(20.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(style.selectedColor),
                unselectedColor = Color(style.unselectedColor)
            )
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .padding(contentPaddingValues)
                .align(Alignment.CenterVertically),
            verticalArrangement = verticalArrangement,
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun RowButtonCheckmarkOuterContent(
    isSelected: Boolean,
    contentPaddingValues: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    trailingContent: (@Composable RowScope.() -> Unit)?,
    modifier: Modifier,
    style: RowStyle.FlatWithCheckmark,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.stripeColors.component)
            .padding(horizontal = style.additionalHorizontalInsetsDp.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.padding(contentPaddingValues),
            verticalArrangement = verticalArrangement,
        ) {
            content()
            Row {
                if (trailingContent != null) {
                    Spacer(Modifier.width(iconWidth + 16.dp))
                    trailingContent()
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterVertically).padding(end = style.checkmarkInsetDp.dp),
                tint = Color(style.checkmarkColor)
            )
        }
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
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        iconContent()

        TitleContent(
            title = title,
            subtitle = subtitle,
            isEnabled = isEnabled,
            contentDescription = contentDescription
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
private fun TitleContent(title: String, subtitle: String?, isEnabled: Boolean, contentDescription: String?,) {
    val textColor = MaterialTheme.stripeColors.onComponent

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Medium),
            color = if (isEnabled) textColor else textColor.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
        )

        if (subtitle != null) {
            val subtitleTextColor = MaterialTheme.stripeColors.subtitle
            Text(
                text = subtitle,
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Normal),
                color = if (isEnabled) subtitleTextColor else subtitleTextColor.copy(alpha = 0.6f),
            )
        }
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
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
                        modifier = Modifier.height(22.dp).width(22.dp),
                        contentAlignment = Alignment.Center,
                    )
                },
                title = "•••• 4242",
                subtitle = null,
                promoText = null,
                onClick = {},
                style = RowStyle.FloatingButton.default,
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
                        modifier = Modifier.height(22.dp).width(22.dp),
                        contentAlignment = Alignment.Center,
                    )
                },
                title = "•••• 4242",
                subtitle = null,
                promoText = null,
                onClick = {},
                style = RowStyle.FloatingButton.default,
                trailingContent = {
                    Text("Edit")
                }
            )
        }
    }
}
