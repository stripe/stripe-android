package com.stripe.android.common.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBackgroundColor
import com.stripe.android.uicore.getBorderStrokeColor
import com.stripe.android.uicore.getComposeTextStyle
import com.stripe.android.uicore.getOnBackgroundColor
import com.stripe.android.ui.core.R as UiCoreR

@Composable
internal fun PrimaryButton(
    label: String,
    isEnabled: Boolean,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    displayLockIcon: Boolean = false,
) {
    // We need to use PaymentsTheme.primaryButtonStyle instead of MaterialTheme
    // because of the rules API for primary button.
    val context = LocalContext.current
    val background = Color(StripeTheme.primaryButtonStyle.getBackgroundColor(context))
    val onBackground = Color(StripeTheme.primaryButtonStyle.getOnBackgroundColor(context))
    val borderStroke = BorderStroke(
        StripeTheme.primaryButtonStyle.shape.borderStrokeWidth.dp,
        Color(StripeTheme.primaryButtonStyle.getBorderStrokeColor(context))
    )
    val shape = RoundedCornerShape(
        StripeTheme.primaryButtonStyle.shape.cornerRadius.dp
    )
    val textStyle = StripeTheme.primaryButtonStyle.getComposeTextStyle()

    CompositionLocalProvider(
        LocalContentAlpha provides if (isEnabled) ContentAlpha.high else ContentAlpha.disabled
    ) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(
                        minHeight = dimensionResource(id = R.dimen.stripe_paymentsheet_primary_button_height)
                    ),
                enabled = isEnabled,
                shape = shape,
                border = borderStroke,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = background,
                    disabledBackgroundColor = background
                )
            ) {
                PrimaryButtonContent(
                    text = label,
                    color = onBackground.copy(alpha = LocalContentAlpha.current),
                    style = textStyle,
                    isEnabled = isEnabled,
                    isLoading = isLoading,
                    displayLockIcon = displayLockIcon,
                )
            }
        }
    }
}

@Composable
private fun PrimaryButtonContent(
    text: String,
    color: Color,
    style: TextStyle,
    isEnabled: Boolean,
    isLoading: Boolean,
    displayLockIcon: Boolean,
) {
    val context = LocalContext.current
    val onBackground = Color(StripeTheme.primaryButtonStyle.getOnBackgroundColor(context))

    BoxWithConstraints(contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            color = color,
            style = style,
            modifier = Modifier.align(Alignment.Center),
        )
        if (isLoading) {
            Box(
                modifier = Modifier
                    .width(maxWidth)
                    .padding(end = 8.dp)
            ) {
                LoadingIndicator(
                    color = MaterialTheme.colors.onPrimary,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        } else if (displayLockIcon) {
            Box(
                modifier = Modifier
                    .width(maxWidth)
                    .padding(end = 8.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = UiCoreR.drawable.stripe_ic_lock
                    ),
                    tint = onBackground.copy(
                        alpha = if (isEnabled) {
                            LocalContentAlpha.current
                        } else {
                            0.5f
                        }
                    ),
                    contentDescription = "lock",
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}
