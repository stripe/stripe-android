package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.image.LocalStripeImageLoader
import com.stripe.android.uicore.image.StripeImage

@Composable
fun Icon(
    iconUrl: String?,
    modifier: Modifier = Modifier,
    errorContent: @Composable BoxWithConstraintsScope.() -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.clip(shape = RoundedCornerShape(3.dp))) {
        iconUrl?.let {
                StripeImage(
                    url = iconUrl,
                    imageLoader = LocalStripeImageLoader.current,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    errorContent = errorContent
                )
        } ?: errorContent()
    }
}
