package com.stripe.android.link.ui.oauth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.LinkTheme

@Composable
internal fun OAuthConsentScreen() {
    Text(
        modifier = Modifier
            .height(100.dp)
            .fillMaxWidth(),
        text = "TODO: OAuth Consent",
        color = LinkTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
    )
}
