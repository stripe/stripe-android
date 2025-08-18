package com.stripe.android.link.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.AppBarHeight
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.appBarTitle
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.ui.core.R as StripeUiCoreR

@Composable
internal fun LinkAppBar(
    state: LinkAppBarState,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
) {
    val elevation = animateDpAsState(
        targetValue = if (state.isElevated) AppBarDefaults.TopAppBarElevation else 0.dp,
        label = "LinkAppBarElevation",
    )

    Surface(
        color = LinkTheme.colors.surfacePrimary,
        modifier = modifier
            .zIndex(1f) // Needed to make sure that the shadow is rendered correctly above the screen content.
            .graphicsLayer { shadowElevation = elevation.value.toPx() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppBarHeight)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.canNavigateBack) {
                AppBarIcon(
                    icon = R.drawable.stripe_link_back,
                    contentDescription = stringResource(id = StripeUiCoreR.string.stripe_back),
                    onPressed = onBackPressed,
                )
            } else {
                LinkAppBarLogo(
                    showHeader = state.showHeader,
                )
            }

            if (state.title != null) {
                Text(
                    text = state.title.resolve(),
                    color = LinkTheme.colors.textPrimary,
                    style = LinkTheme.typography.appBarTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (state.canShowCloseIcon) {
                AppBarIcon(
                    icon = R.drawable.stripe_link_close,
                    contentDescription = stringResource(id = com.stripe.android.R.string.stripe_close),
                    onPressed = onBackPressed,
                    modifier = Modifier.padding(start = 20.dp),
                )
            }
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
        Surface(
            color = LinkTheme.colors.surfacePrimary,
        ) {
            Column(
                modifier = Modifier.padding(bottom = 20.dp),
            ) {
                LinkAppBar(
                    state = LinkAppBarState(
                        showHeader = true,
                        canNavigateBack = false,
                        title = null,
                        isElevated = true,
                    ),
                    onBackPressed = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun LinkAppBarChildScreen() {
    DefaultLinkTheme {
        Surface(
            color = LinkTheme.colors.surfacePrimary,
        ) {
            LinkAppBar(
                state = LinkAppBarState(
                    showHeader = false,
                    canNavigateBack = true,
                    title = "Add a payment method".resolvableString,
                    isElevated = false,
                ),
                onBackPressed = {},
            )
        }
    }
}
