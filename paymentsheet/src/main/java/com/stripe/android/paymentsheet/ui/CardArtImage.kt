package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
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
                modifier = Modifier
                    .width(maxWidth)
                    .height(maxHeight),
                imageContent = {
                    Image(
                        modifier = Modifier
                            .widthIn(max = maxWidth)
                            .heightIn(max = maxHeight)
                            .clip(RoundedCornerShape(3.dp))
                            .border(
                                width = 1.dp,
                                color = Color.Black.copy(alpha = .2f),
                                shape = RoundedCornerShape(3.dp),
                            ),
                        contentScale = ContentScale.Fit,
                        contentDescription = null,
                        painter = it
                    )
                },
                errorContent = { fallbackContent() },
                loadingContent = {
                    ShimmerEffect(
                        modifier = Modifier
                            .width(maxWidth)
                            .height(maxHeight)
                            .clip(RoundedCornerShape(3.dp)),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.stripe_ic_cbc),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            )
        } else {
            fallbackContent()
        }
    }
}

private val DebugCardArtPainter = ColorPainter(Color(0xFF635BFF))
