package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.RowButton
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

    RowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        isClickable = isClickable,
        onClick = onClick,
        contentPaddingValues = PaddingValues(horizontal = 12.dp, vertical = contentPaddingValues),
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
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

            if (trailingContent != null) {
                Spacer(modifier = Modifier.weight(1f))
                trailingContent()
            }
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
