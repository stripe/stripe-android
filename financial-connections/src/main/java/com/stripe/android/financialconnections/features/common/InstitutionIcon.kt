package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.isLink
import com.stripe.android.uicore.image.StripeImage

@Composable
internal fun InstitutionIcon(
    institutionIcon: String?,
    modifier: Modifier = Modifier,
    disablePlaceholder: Boolean = false,
    size: Dp = 56.dp,
) {
    val previewMode = LocalInspectionMode.current
    val isLink = FinancialConnectionsTheme.theme.isLink
    val shape = RoundedCornerShape(if (isLink && size >= 64.dp) 18.dp else 12.dp)
    val iconModifier = modifier
        .size(size)
        .then(
            if (isLink) {
                Modifier
                    .border(0.5.dp, colors.borderOnCard, shape)
                    .clip(shape)
            } else {
                Modifier.shadow(1.dp, shape, clip = true)
            }
        )

    when {
        institutionIcon == null && disablePlaceholder -> {
            Box(modifier = iconModifier.background(colors.backgroundSecondary))
        }
        previewMode || institutionIcon == null -> {
            InstitutionPlaceholder(iconModifier)
        }
        else -> {
            StripeImage(
                url = institutionIcon,
                imageLoader = LocalImageLoader.current,
                contentDescription = null,
                modifier = iconModifier,
                contentScale = ContentScale.Crop,
                loadingContent = { Box(modifier = iconModifier.background(colors.backgroundSecondary)) },
                errorContent = { InstitutionPlaceholder(iconModifier) }
            )
        }
    }
}

@Composable
private fun InstitutionPlaceholder(modifier: Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
        contentDescription = "Bank icon placeholder",
        contentScale = ContentScale.Crop
    )
}
