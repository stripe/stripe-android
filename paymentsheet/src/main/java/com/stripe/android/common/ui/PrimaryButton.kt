package com.stripe.android.common.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
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
    canClickWhileDisabled: Boolean = false,
    isLoading: Boolean = false,
    displayLockIcon: Boolean = false,
    onDisabledButtonClick: () -> Unit = {},
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
        LocalContentColor provides if (isEnabled) onBackground else onBackground.copy(alpha = 0.38f)
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
                        minHeight = StripeTheme.primaryButtonStyle.shape.height.dp
                    ),
                enabled = isEnabled,
                shape = shape,
                border = borderStroke,
                colors = ButtonDefaults.buttonColors(
                    containerColor = background,
                    disabledContainerColor = background
                )
            ) {
                PrimaryButtonContent(
                    text = label,
                    color = LocalContentColor.current,
                    style = textStyle,
                    isEnabled = isEnabled,
                    isLoading = isLoading,
                    displayLockIcon = displayLockIcon,
                )
            }

            DisabledButton(
                enabled = isEnabled,
                allowedDisabledClicks = canClickWhileDisabled,
                onDisabledButtonClick = onDisabledButtonClick,
            )
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
                    color = MaterialTheme.colorScheme.onPrimary,
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
                            LocalContentColor.current.alpha
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

@Composable
private fun BoxScope.DisabledButton(
    enabled: Boolean,
    allowedDisabledClicks: Boolean,
    onDisabledButtonClick: () -> Unit,
) {
    if (allowedDisabledClicks && !enabled) {
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { onDisabledButtonClick.invoke() }
                }
        )
    }
}
