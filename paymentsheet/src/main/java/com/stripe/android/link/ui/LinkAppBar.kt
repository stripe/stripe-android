package com.stripe.android.link.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.AppBarHeight
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.R as StripeUiCoreR

@Composable
internal fun LinkAppBar(
    state: LinkAppBarState,
    onBackPressed: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppBarHeight)
            .padding(horizontal = 20.dp),
    ) {
        if (state.canNavigateBack) {
            AppBarIcon(
                icon = R.drawable.stripe_link_back,
                contentDescription = stringResource(id = StripeUiCoreR.string.stripe_back),
                onPressed = onBackPressed,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        } else {
            LinkAppBarLogo(
                showHeader = state.showHeader,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }

        if (state.canShowCloseIcon) {
            AppBarIcon(
                icon = R.drawable.stripe_link_close,
                contentDescription = stringResource(id = com.stripe.android.R.string.stripe_close),
                onPressed = onBackPressed,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
internal fun AppBarIcon(
    icon: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onPressed: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(30.dp)
            .background(
                color = LinkTheme.colors.surfaceSecondary,
                shape = CircleShape,
            )
            .clip(CircleShape)
            .clickable(onClick = onPressed),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = LinkTheme.colors.iconSecondary,
            modifier = Modifier.size(12.dp),
        )
    }
}

@Composable
private fun LinkAppBarLogo(
    showHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (showHeader) 1f else 0f,
        label = "titleAlpha"
    )
    Box(
        modifier = modifier.alpha(contentAlpha),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.stripe_link_logo),
            contentDescription = stringResource(com.stripe.android.R.string.stripe_link),
        )
    }
}

@Preview
@Composable
private fun LinkAppBarPreview() {
    DefaultLinkTheme {
        Surface {
            LinkAppBar(
                state = LinkAppBarState(
                    showHeader = true,
                    showOverflowMenu = true,
                    canNavigateBack = false,
                ),
                onBackPressed = {},
            )
        }
    }
}

@Preview
@Composable
private fun LinkAppBarChildScreen() {
    DefaultLinkTheme {
        Surface {
            LinkAppBar(
                state = LinkAppBarState(
                    showHeader = false,
                    showOverflowMenu = false,
                    canNavigateBack = true,
                ),
                onBackPressed = {},
            )
        }
    }
}
