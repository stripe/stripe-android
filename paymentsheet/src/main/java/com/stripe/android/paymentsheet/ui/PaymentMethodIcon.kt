package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeColors

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_ICON_FROM_RES = "PaymentMethodIconFomRes"
private const val MIN_LUMINANCE_FOR_BLACK_TINT = 0.5

@Composable
internal fun PaymentMethodIcon(
    iconRes: Int,
    iconUrl: String?,
    imageLoader: StripeImageLoader,
    iconRequiresTinting: Boolean,
    modifier: Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
) {
    val color = MaterialTheme.stripeColors.component
    val colorFilter = remember(iconRequiresTinting) {
        if (iconRequiresTinting) {
            val tintColor = if (color.luminance() < MIN_LUMINANCE_FOR_BLACK_TINT) Color.White else Color.Black
            ColorFilter.tint(tintColor)
        } else {
            null
        }
    }
    val iconModifier = Modifier.fillMaxSize()
    val iconFromResource = @Composable {
        PaymentMethodIconFromResource(
            iconRes = iconRes,
            colorFilter = colorFilter,
            modifier = iconModifier,
            alignment = contentAlignment,
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = contentAlignment,
    ) {
        if (iconUrl != null) {
            StripeImage(
                url = iconUrl,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                loadingContent = { iconFromResource() },
                errorContent = { iconFromResource() },
                disableAnimations = true,
                alignment = contentAlignment,
                modifier = iconModifier,
            )
        } else {
            iconFromResource()
        }
    }
}

@Composable
internal fun PaymentMethodIconFromResource(
    iconRes: Int,
    colorFilter: ColorFilter?,
    alignment: Alignment,
    modifier: Modifier,
) {
    if (iconRes != 0) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            colorFilter = colorFilter,
            alignment = alignment,
            modifier = modifier.testTag(TEST_TAG_ICON_FROM_RES)
        )
    }
}
