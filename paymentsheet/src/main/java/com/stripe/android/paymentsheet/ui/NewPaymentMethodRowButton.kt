package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve

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
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
    )
}
