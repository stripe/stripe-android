package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    BoxWithConstraints(modifier = modifier) {
        val optimizedUrl = rememberOptimizedImage(url = url, width = maxWidth)
        if (optimizedUrl != null) {
            StripeImage(
                url = optimizedUrl,
                imageLoader = LocalStripeImageLoader.current,
                debugPainter = painterResource(R.drawable.card_art_sample),
                contentDescription = null,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .border(
                        width = 1.dp,
                        color = Color.Black.copy(
                            alpha = .2f
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )
                    .width(maxWidth)
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
