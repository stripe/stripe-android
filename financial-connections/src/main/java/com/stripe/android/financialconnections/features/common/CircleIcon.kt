package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.theme.Brand50
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.image.StripeImage

private val iconSize = 20.dp

/**
 * A circular icon with a branded background color.
 *
 * @param painter the [Painter] to use for the icon
 */
@Composable
internal fun CircleIcon(painter: Painter) {
    CircleBox {
        LocalIcon(painter)
    }
}

/**
 * A circular icon with a branded background color.
 *
 * @param url the URL to use for the icon
 * @param errorPainter the [Painter] to use for the icon if the URL fails to load. If null,
 *        no icon will be rendered inside the circle.
 */
@Composable
internal fun CircleIcon(
    url: String,
    errorPainter: Painter? = null
) {
    CircleBox {
        StripeImage(
            modifier = Modifier.size(iconSize),
            url = url,
            imageLoader = LocalImageLoader.current,
            debugPainter = painterResource(id = R.drawable.stripe_ic_person),
            contentDescription = "Web Icon",
            errorContent = { errorPainter?.let { LocalIcon(errorPainter) } },
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun LocalIcon(painter: Painter) {
    Icon(
        painter = painter,
        tint = FinancialConnectionsTheme.v3Colors.iconBrand,
        contentDescription = "Web Icon",
        modifier = Modifier.size(iconSize),
    )
}

@Composable
private fun CircleBox(
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .background(color = Brand50, shape = CircleShape)
    ) {
        content()
    }
}
