package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.uicore.image.StripeImage

/**
 * A circular icon with a branded background color.
 *
 * @param painter the [Painter] to use for the icon
 */
@Composable
internal fun ShapedIcon(
    painter: Painter,
    modifier: Modifier = Modifier,
    iconSize: IconSize = IconSize.Medium,
    backgroundShape: Shape = CircleShape,
    contentDescription: String?,
) {
    IconWrapperBox(
        modifier = modifier,
        iconSize = iconSize,
        backgroundShape = backgroundShape,
    ) {
        LocalIcon(
            painter = painter,
            iconSize = iconSize,
            contentDescription = contentDescription
        )
    }
}

/**
 * A circular icon with a branded background color.
 *
 * @param url the URL to use for the icon
 * @param modifier to apply to the icon wrapper
 * @param iconSize the size of the icon
 * @param backgroundShape the shape of the icon wrapper
 * @param contentDescription the content description for the icon
 * @param flushed whether the icon should be flushed to the edge of the wrapper
 * @param errorPainter the [Painter] to use for the icon if the URL fails to load. If null,
 *        no icon will be rendered inside the circle.
 */
@Composable
internal fun ShapedIcon(
    url: String,
    modifier: Modifier = Modifier,
    iconSize: IconSize = IconSize.Medium,
    backgroundShape: Shape = CircleShape,
    contentDescription: String?,
    errorPainter: Painter? = null,
    flushed: Boolean = false
) {
    IconWrapperBox(
        modifier = modifier,
        backgroundShape = backgroundShape,
        iconSize = iconSize
    ) {
        StripeImage(
            modifier = Modifier.size(
                if (flushed) iconSize.size else iconSize.paddedSize
            ),
            url = url,
            imageLoader = LocalImageLoader.current,
            debugPainter = painterResource(id = R.drawable.stripe_ic_person),
            contentDescription = contentDescription,
            errorContent = {
                errorPainter?.let {
                    LocalIcon(
                        iconSize = iconSize,
                        painter = errorPainter,
                        contentDescription = contentDescription
                    )
                }
            },
            loadingContent = {
                Image(
                    modifier = modifier,
                    contentDescription = contentDescription,
                    painter = ColorPainter(color = colors.background),
                )
            },
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun LocalIcon(
    iconSize: IconSize,
    painter: Painter,
    contentDescription: String?
) {
    Icon(
        painter = painter,
        tint = colors.iconTint,
        contentDescription = contentDescription,
        modifier = Modifier.size(iconSize.paddedSize),
    )
}

@Composable
private fun IconWrapperBox(
    modifier: Modifier = Modifier,
    iconSize: IconSize,
    backgroundShape: Shape,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(iconSize.size)
            .background(color = colors.iconBackground, shape = backgroundShape)
            .clip(backgroundShape)
    ) {
        content()
    }
}

internal enum class IconSize(val size: Dp, val paddedSize: Dp) {
    Large(64.dp, 32.dp),
    Medium(56.dp, 20.dp),
    Small(24.dp, 12.dp)
}
