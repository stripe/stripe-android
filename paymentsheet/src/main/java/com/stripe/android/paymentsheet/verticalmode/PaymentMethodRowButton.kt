package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.ButtonType
import com.stripe.android.paymentsheet.ui.RowButton
import com.stripe.android.uicore.StripeTheme
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
    buttonType: ButtonType = ButtonType.FLOATING,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val verticalPadding = if (subtitle != null) {
        8.dp
    } else {
        12.dp
    }

    val startPadding = if (buttonType == ButtonType.FLOATING) {
        12.dp
    } else {
        0.dp
    }

    Row {
        if (buttonType == ButtonType.RADIO) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
        RowButton(
            isEnabled = isEnabled,
            isSelected = isSelected,
            isClickable = isClickable,
            onClick = onClick,
            contentPaddingValues = PaddingValues(
                start = startPadding,
                end = 12.dp,
                top = verticalPadding,
                bottom = verticalPadding
            ),
            verticalArrangement = Arrangement.Center,
            buttonType = buttonType,
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
                    contentDescription = contentDescription,
                    buttonType = buttonType,
                    trailingContent = trailingContent
                )

                if (trailingContent != null && buttonType != ButtonType.CHECKMARK) {
                    Spacer(modifier = Modifier.weight(1f))
                    trailingContent()
                }

                if (buttonType == ButtonType.CHECKMARK && isSelected) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        // TODO(tjclawson): Replace with embedded appearance API values once merged
                        tint = StripeTheme.getColors(isSystemInDarkTheme()).materialColors.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleContent(
    title: String,
    subtitle: String?,
    isEnabled: Boolean,
    contentDescription: String?,
    buttonType: ButtonType,
    trailingContent: @Composable (RowScope.() -> Unit)?
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
        if (trailingContent != null && buttonType == ButtonType.CHECKMARK) {
            Row {
                trailingContent()
            }
        }
    }
}
