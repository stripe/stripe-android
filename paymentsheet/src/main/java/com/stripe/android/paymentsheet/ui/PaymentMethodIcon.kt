package com.stripe.android.paymentsheet.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeColors

@VisibleForTesting
internal const val TEST_TAG_ICON_FROM_RES = "PaymentMethodIconFomRes"

@Composable
internal fun PaymentMethodIcon(
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
internal fun PaymentMethodIconFromResource(
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
