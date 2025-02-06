package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.ImageResource
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.sdui.BulletUI
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.uicore.image.StripeImage

@Composable
internal fun ListItem(
    bullet: BulletUI,
    onClickableTextClick: (String) -> Unit,
) {
    val firstText = bullet.title ?: bullet.content ?: TextResource.Text("")
    val secondText = remember(firstText) { bullet.content?.takeIf { bullet.title != null } }
    val titleStyle = if (secondText != null) typography.bodyMediumEmphasized else typography.bodyMedium
    Row {
        ListItemIcon(icon = bullet.imageResource)
        Spacer(modifier = Modifier.size(16.dp))
        Column {
            AnnotatedText(
                text = firstText,
                defaultStyle = titleStyle.copy(color = FinancialConnectionsTheme.colors.textDefault),
                onClickableTextClick = onClickableTextClick
            )
            secondText?.let {
                AnnotatedText(
                    text = requireNotNull(bullet.content),
                    defaultStyle = typography.bodySmall.copy(color = FinancialConnectionsTheme.colors.textSubdued),
                    onClickableTextClick = onClickableTextClick
                )
            }
        }
    }
}

@Composable
private fun ListItemIcon(icon: ImageResource?) {
    val bulletColor = FinancialConnectionsTheme.colors.icon
    val iconSize = 20.dp
    val modifier = Modifier
        .size(iconSize)
        .offset(y = 1.dp)
    when (icon) {
        // Render a bullet if no icon is provided
        null -> Canvas(
            modifier = modifier.padding((iconSize - 8.dp) / 2),
            onDraw = { drawCircle(color = bulletColor) }
        )

        // Render the icon if it's a local resource
        is ImageResource.Local -> Image(
            modifier = modifier,
            painter = painterResource(id = icon.resId),
            contentDescription = null,
        )

        // Render the icon if it's a network resource, or fallback to a bullet if it fails to load
        is ImageResource.Network -> StripeImage(
            url = icon.url,
            debugPainter = painterResource(id = R.drawable.stripe_ic_check_circle),
            errorContent = {
                Canvas(
                    modifier = modifier.padding((iconSize - 8.dp) / 2),
                    onDraw = { drawCircle(color = bulletColor) }
                )
            },
            loadingContent = {
                LoadingShimmerEffect { shimmer ->
                    Spacer(
                        modifier = modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                }
            },
            imageLoader = LocalImageLoader.current,
            colorFilter = ColorFilter.tint(bulletColor),
            contentDescription = null,
            modifier = modifier
        )
    }
}
