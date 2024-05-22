package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors

@Composable
internal fun NewPaymentMethodRowButton(
    isEnabled: Boolean,
    isSelected: Boolean,
    supportedPaymentMethod: SupportedPaymentMethod,
    imageLoader: StripeImageLoader,
    onClick: (SupportedPaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconUrl = if (isSystemInDarkTheme() && supportedPaymentMethod.darkThemeIconUrl != null) {
        supportedPaymentMethod.darkThemeIconUrl
    } else {
        supportedPaymentMethod.lightThemeIconUrl
    }
    NewPaymentMethodRowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        iconRes = supportedPaymentMethod.iconResource,
        iconUrl = iconUrl,
        imageLoader = imageLoader,
        title = supportedPaymentMethod.displayName.resolve(),
        subtitle = supportedPaymentMethod.subtitle?.resolve(),
        iconRequiresTinting = supportedPaymentMethod.iconRequiresTinting,
        onClick = {
            onClick(supportedPaymentMethod)
        },
        modifier = modifier,
    )
}

@Composable
internal fun NewPaymentMethodRowButton(
    isEnabled: Boolean,
    isSelected: Boolean,
    iconRes: Int,
    iconUrl: String?,
    imageLoader: StripeImageLoader,
    title: String,
    subtitle: String?,
    iconRequiresTinting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaymentMethodRowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        iconContent = {
            PaymentMethodIcon(
                iconRes = iconRes,
                iconUrl = iconUrl,
                imageLoader = imageLoader,
                iconRequiresTinting = iconRequiresTinting,
                modifier = Modifier.size(20.dp),
            )
        },
        textContent = {
            val textColor = MaterialTheme.stripeColors.onComponent
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.caption,
                    color = if (isEnabled) textColor else textColor.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle != null) {
                    val subtitleTextColor = MaterialTheme.stripeColors.subtitle
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.subtitle1.copy(fontSize = 10.sp),
                        color = if (isEnabled) subtitleTextColor else subtitleTextColor.copy(alpha = 0.6f),
                    )
                }
            }
        },
        onClick = onClick,
        modifier = modifier,
    )
}
