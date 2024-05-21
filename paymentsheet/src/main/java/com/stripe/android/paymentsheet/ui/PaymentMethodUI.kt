package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors

private object PaymentMethodUISpacing {
    val cardPadding = 12.dp
    val iconSize = 16.dp
}

@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_ICON_FROM_RES = "PaymentMethodsUIIconFomRes"

@Composable
internal fun PaymentMethodUI(
    minViewWidth: Dp,
    iconRes: Int,
    iconUrl: String?,
    imageLoader: StripeImageLoader,
    title: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    iconRequiresTinting: Boolean,
    modifier: Modifier = Modifier,
    onItemSelectedListener: () -> Unit
) {
    RowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        onClick = onItemSelectedListener,
        contentPaddingValues = PaddingValues(
            start = PaymentMethodUISpacing.cardPadding,
            end = PaymentMethodUISpacing.cardPadding,
            top = PaymentMethodUISpacing.cardPadding,
        ),
        modifier = modifier
            .height(60.dp)
            .widthIn(min = minViewWidth),
    ) {
        PaymentMethodIconUi(
            iconRes = iconRes,
            iconUrl = iconUrl,
            imageLoader = imageLoader,
            iconRequiresTinting = iconRequiresTinting,
            modifier = Modifier
                .height(PaymentMethodUISpacing.iconSize),
        )

        LpmSelectorText(
            text = title,
            isEnabled = isEnabled,
            textColor = MaterialTheme.stripeColors.onComponent,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun PaymentMethodIconUi(
    iconRes: Int,
    iconUrl: String?,
    imageLoader: StripeImageLoader,
    iconRequiresTinting: Boolean,
    modifier: Modifier,
) {
    val color = MaterialTheme.stripeColors.onComponent
    val colorFilter = remember(iconRequiresTinting) {
        if (iconRequiresTinting) {
            ColorFilter.tint(color)
        } else {
            null
        }
    }

    Box(
        modifier = modifier,
    ) {
        if (iconUrl != null) {
            StripeImage(
                url = iconUrl,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                errorContent = {
                    PaymentMethodIconFromResource(
                        iconRes = iconRes,
                        colorFilter = colorFilter
                    )
                },
            )
        } else {
            PaymentMethodIconFromResource(iconRes = iconRes, colorFilter = colorFilter)
        }
    }
}

@Composable
private fun PaymentMethodIconFromResource(
    iconRes: Int,
    colorFilter: ColorFilter?,
) {
    if (iconRes != 0) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            colorFilter = colorFilter,
            modifier = Modifier.testTag(TEST_TAG_ICON_FROM_RES)
        )
    }
}

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
        subTitle = null,
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
    subTitle: String?,
    iconRequiresTinting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaymentMethodRowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        iconContent = {
            PaymentMethodIconUi(
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

                if (subTitle != null) {
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.subtitle1,
                        color = if (isEnabled) textColor else textColor.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        onClick = onClick,
        modifier = modifier,
    )
}
