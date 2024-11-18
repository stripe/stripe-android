package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.PaymentMethodIcon
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeColors

@Composable
internal fun PaymentMethodRowButtonFloating(
    isEnabled: Boolean,
    isSelected: Boolean,
    isClickable: Boolean = isEnabled,
    iconContent: @Composable RowScope.() -> Unit,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.selectable(
            selected = isSelected,
            enabled = isClickable,
            onClick = onClick
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                .alpha(alpha = if (isEnabled) 1.0F else 0.6F),
            shape = MaterialTheme.shapes.medium,
            backgroundColor = MaterialTheme.stripeColors.component,
            border = MaterialTheme.getBorderStroke(isSelected),
            elevation = if (isSelected) 1.5.dp else 0.dp
        ) {
            RowButtonContent(
                contentPaddingValues = PaddingValues(horizontal = 12.dp, vertical = subtitle.paddingValues()),
                verticalArrangement = Arrangement.Center,
            ) {
                PaymentMethodContentExtras(
                    title = title,
                    subtitle = subtitle,
                    isEnabled = isEnabled,
                    contentDescription = contentDescription,
                    isSelected = isSelected,
                    iconContent = iconContent,
                    trailingContent = trailingContent,
                    isCheckmarkRow = false
                )
            }
        }
    }
}

@Composable
internal fun PaymentMethodRowButtonRadio(
    isEnabled: Boolean,
    isSelected: Boolean,
    isClickable: Boolean = isEnabled,
    iconContent: @Composable RowScope.() -> Unit,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.selectable(
            selected = isSelected,
            enabled = isClickable,
            onClick = onClick
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        RowButtonContent(
            contentPaddingValues = PaddingValues(
                start = 0.dp,
                end = 12.dp,
                top = subtitle.paddingValues(),
                bottom = subtitle.paddingValues()
            ),
            verticalArrangement = Arrangement.Center,
        ) {
            PaymentMethodContentExtras(
                title = title,
                subtitle = subtitle,
                isEnabled = isEnabled,
                contentDescription = contentDescription,
                isSelected = isSelected,
                iconContent = iconContent,
                trailingContent = trailingContent,
                isCheckmarkRow = false
            )
        }
    }
}

@Composable
internal fun PaymentMethodRowButtonCheckmark(
    isEnabled: Boolean,
    isSelected: Boolean,
    isClickable: Boolean = isEnabled,
    iconContent: @Composable RowScope.() -> Unit,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    contentDescription: String? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.selectable(
            selected = isSelected,
            enabled = isClickable,
            onClick = onClick
        )
    ) {
        RowButtonContent(
            contentPaddingValues = PaddingValues(
                start = 0.dp,
                end = 12.dp,
                top = subtitle.paddingValues(),
                bottom = subtitle.paddingValues()
            ),
            verticalArrangement = Arrangement.Center,
        ) {
            PaymentMethodContentExtras(
                title = title,
                subtitle = subtitle,
                isEnabled = isEnabled,
                contentDescription = contentDescription,
                isSelected = isSelected,
                iconContent = iconContent,
                trailingContent = trailingContent,
                isCheckmarkRow = true
            )
        }
    }
}

@Composable
internal fun RowButtonContent(
    contentPaddingValues: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .padding(contentPaddingValues),
        verticalArrangement = verticalArrangement,
    ) {
        content()
    }
}

@Composable
private fun TitleContent(
    title: String,
    subtitle: String?,
    isEnabled: Boolean,
    contentDescription: String?,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
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
        if (trailingContent != null) {
            Row {
                trailingContent()
            }
        }
    }
}

@Composable
private fun PaymentMethodContentExtras(
    title: String,
    subtitle: String?,
    isEnabled: Boolean,
    contentDescription: String?,
    isSelected: Boolean,
    iconContent: @Composable RowScope.() -> Unit,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    isCheckmarkRow: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = if (isCheckmarkRow && trailingContent != null) {
            Alignment.Top
        } else {
            Alignment.CenterVertically
        },
    ) {
        iconContent()
        if (isCheckmarkRow) {
            TitleContent(
                title = title,
                subtitle = subtitle,
                isEnabled = isEnabled,
                contentDescription = contentDescription,
                trailingContent = trailingContent
            )

            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterVertically),
                    tint = StripeTheme.getColors(isSystemInDarkTheme()).materialColors.primary
                )
            }
        } else {
            TitleContent(
                title = title,
                subtitle = subtitle,
                isEnabled = isEnabled,
                contentDescription = contentDescription,
            )

            if (trailingContent != null) {
                Spacer(modifier = Modifier.weight(1f))
                trailingContent()
            }
        }
    }
}

private fun String?.paddingValues(): Dp = if (this != null) 8.dp else 12.dp

@Composable
@Preview
internal fun ButtonPreview() {
    Row {
        PaymentMethodRowButtonFloating(
            isEnabled = true,
            isSelected = true,
            iconContent = {
                PaymentMethodIcon(
                    iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                    iconUrl = null,
                    imageLoader = StripeImageLoader(LocalContext.current.applicationContext),
                    iconRequiresTinting = true,
                    modifier = Modifier.height(22.dp).width(22.dp),
                    contentAlignment = Alignment.Center,
                )
            },
            title = "Card",
            subtitle = "This is a card",
            onClick = {}
        )
    }
}
