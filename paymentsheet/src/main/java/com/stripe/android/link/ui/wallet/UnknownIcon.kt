package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.stripe.android.uicore.image.LocalStripeImageLoader
import com.stripe.android.uicore.image.StripeImage

@Composable
fun UnknownIcon(
    iconUrl: String?,
    modifier: Modifier = Modifier
) {
    iconUrl?.let {
        Box(modifier = modifier) {
            StripeImage(
                url = iconUrl,
                imageLoader = LocalStripeImageLoader.current,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                errorContent = { BankIcon(null) }
            )
        }
    } ?: BankIcon(null)
}
