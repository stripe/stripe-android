package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.image.LocalStripeImageLoader
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.rememberOptimizedImage

@Composable
internal fun CardArtImage(
    url: String?,
    modifier: Modifier = Modifier,
    fallbackContent: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier) {
        val optimizedUrl = rememberOptimizedImage(url = url, width = maxWidth)
        if (optimizedUrl != null) {
            StripeImage(
                url = optimizedUrl,
                imageLoader = LocalStripeImageLoader.current,
                debugPainter = DebugCardArtPainter,
                contentDescription = null,
                modifier = Modifier
                    .width(maxWidth)
                    .height(maxHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .border(
                        width = 1.dp,
                        color = Color.Black.copy(alpha = .2f),
                        shape = RoundedCornerShape(3.dp),
                    )
                    .align(Alignment.Center),
                contentScale = ContentScale.FillWidth,
                errorContent = { fallbackContent() },
                loadingContent = {}
            )
        } else {
            fallbackContent()
        }
    }
}

private val DebugCardArtPainter = ColorPainter(Color(0xFF635BFF))
