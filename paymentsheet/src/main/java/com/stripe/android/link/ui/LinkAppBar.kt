package com.stripe.android.link.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.AppBarHeight
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.R as StripeUiCoreR

@Composable
internal fun LinkAppBar(
    state: LinkAppBarState,
    onBackPressed: () -> Unit,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    onLogoutClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppBarHeight),
    ) {
        BackIcon(
            icon = state.navigationIcon,
            onBackPressed = onBackPressed,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        LinkAppBarTitle(
            showHeader = state.showHeader,
            modifier = Modifier.align(Alignment.Center),
        )

        LinkAppBarAction(
            showOverflowMenu = state.showOverflowMenu,
            showBottomSheetContent = showBottomSheetContent,
            onLogoutClicked = onLogoutClicked,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun BackIcon(
    icon: Int,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
) {
    IconButton(
        onClick = onBackPressed,
        modifier = modifier.padding(4.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(id = StripeUiCoreR.string.stripe_back),
            tint = MaterialTheme.linkColors.closeButton
        )
    }
}

@Composable
private fun LinkAppBarTitle(
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

@Composable
private fun LinkAppBarAction(
    showOverflowMenu: Boolean,
    modifier: Modifier = Modifier,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    onLogoutClicked: () -> Unit,
) {
    val overflowIconAlpha by animateFloatAsState(
        targetValue = if (showOverflowMenu) 1f else 0f,
        label = "overflowAlpha"
    )

    IconButton(
        onClick = {
            showBottomSheetContent {
                LinkAppBarMenu(onLogoutClicked)
            }
        },
        enabled = showOverflowMenu,
        modifier = modifier
            .alpha(overflowIconAlpha)
            .padding(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.stripe_show_menu),
            tint = MaterialTheme.linkColors.closeButton
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
                    navigationIcon = R.drawable.stripe_link_close,
                    showHeader = true,
                    showOverflowMenu = true,
                ),
                onBackPressed = {},
                showBottomSheetContent = {},
                onLogoutClicked = {}
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
                    navigationIcon = R.drawable.stripe_link_back,
                    showHeader = false,
                    showOverflowMenu = false,
                ),
                onBackPressed = {},
                showBottomSheetContent = {},
                onLogoutClicked = {}
            )
        }
    }
}
