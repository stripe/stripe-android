package com.stripe.android.paymentsheet.verticalmode

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.PaymentMethodIcon
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeColors

@Composable
internal fun PaymentMethodRowButton(
    isEnabled: Boolean,
    isSelected: Boolean,
    isClickable: Boolean = isEnabled,
    iconContent: @Composable RowScope.() -> Unit,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val contentPaddingValues = if (subtitle != null) {
        8.dp
    } else {
        12.dp
    }

    RowButtonFloatingOuterContent(
        isEnabled = isEnabled,
        isSelected = isSelected,
        contentPaddingValues = PaddingValues(horizontal = 12.dp, vertical = contentPaddingValues),
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(
                selected = isSelected,
                enabled = isClickable,
                onClick = onClick
            ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowButtonInnerContent(isEnabled, iconContent, title, subtitle, contentDescription)

            if (trailingContent != null) {
                Spacer(modifier = Modifier.weight(1f))
                trailingContent()
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

/**
 * Icon, title, and subtitle if provided. Common across all PaymentMethodRowButton configurations
 */
@Composable
private fun RowButtonInnerContent(
    isEnabled: Boolean,
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

@Composable
@Preview
internal fun ButtonPreview() {
    Row {
        PaymentMethodRowButton(
            isEnabled = true,
            isSelected = true,
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
            title = "Card",
            subtitle = "This is a card",
            onClick = {}
        )
    }
}
