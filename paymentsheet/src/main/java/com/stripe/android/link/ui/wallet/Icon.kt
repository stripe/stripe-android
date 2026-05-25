package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.stripe.android.uicore.image.LocalStripeImageLoader
import com.stripe.android.uicore.image.StripeImage

@Composable
fun Icon(
    iconUrl: String?,
    modifier: Modifier = Modifier,
    errorContent: @Composable (BoxWithConstraintsScope.() -> Unit) = {},
) {
    Box(modifier = modifier) {
        iconUrl?.let {
                StripeImage(
                    url = iconUrl,
                    imageLoader = LocalStripeImageLoader.current,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    errorContent = errorContent
                )
        } ?: errorContent
    }
}
