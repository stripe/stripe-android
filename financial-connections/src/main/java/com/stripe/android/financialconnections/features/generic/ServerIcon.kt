package com.stripe.android.financialconnections.features.generic

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.CircleBox
import com.stripe.android.financialconnections.features.common.LocalIcon
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.uicore.image.StripeImage

@Composable
internal fun ServerIcon(
    iconSize: IconSize = IconSize.Medium,
    squarcle: Boolean = false,
    contentDescription: String? = null,
    errorPainter: Painter? = null,
    iconUrl: String,
) {
    if (squarcle) {
        CircleBox(
            backgroundShape = CircleShape,
            size = iconSize.size
        ) {
            Icon(
                iconSize = iconSize.paddedSize,
                iconUrl = iconUrl,
                contentDescription = contentDescription,
                errorPainter = errorPainter
            )
        }
    } else {
        Icon(
            iconSize = iconSize.size,
            iconUrl = iconUrl,
            contentDescription = contentDescription,
            errorPainter = errorPainter
        )
    }

}

@Composable
private fun Icon(
    iconSize: Dp,
    iconUrl: String,
    contentDescription: String?,
    errorPainter: Painter?
) {
    StripeImage(
        modifier = Modifier.size(iconSize),
        url = iconUrl,
        imageLoader = LocalImageLoader.current,
        debugPainter = painterResource(id = R.drawable.stripe_ic_person),
        contentDescription = contentDescription,
        errorContent = { errorPainter?.let { LocalIcon(errorPainter, contentDescription) } },
        contentScale = ContentScale.Crop
    )
}

internal enum class IconSize(val size: Dp, val paddedSize: Dp) {
    Large(64.dp, 32.dp),
    Medium(56.dp, 20.dp),
    Small(24.dp, 12.dp)
}