package com.stripe.android.link.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
    onLogout: () -> Unit,
    showBottomSheetContent: (BottomSheetContent?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppBarHeight),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        IconButton(
            onClick = onBackPressed,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                painter = painterResource(state.navigationIcon),
                contentDescription = stringResource(id = StripeUiCoreR.string.stripe_back),
                tint = MaterialTheme.linkColors.closeButton
            )
        }

        LinkAppBarTitle(
            email = state.email,
            showHeader = state.showHeader
        )

        LinkAppBarAction(
            showOverflowMenu = state.showOverflowMenu,
            onLogout = onLogout,
            showBottomSheetContent = showBottomSheetContent
        )
    }
}

@Composable
private fun RowScope.LinkAppBarTitle(
    email: String?,
    showHeader: Boolean
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (showHeader) 1f else 0f,
        label = "titleAlpha"
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .alpha(contentAlpha)
            .padding(top = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.stripe_link_logo),
            contentDescription = stringResource(com.stripe.android.R.string.stripe_link),
        )

        AnimatedVisibility(visible = email != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = email.orEmpty(),
                    color = MaterialTheme.linkColors.disabledText,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LinkAppBarAction(
    showOverflowMenu: Boolean,
    onLogout: () -> Unit,
    showBottomSheetContent: (BottomSheetContent?) -> Unit
) {
    val overflowIconAlpha by animateFloatAsState(
        targetValue = if (showOverflowMenu) 1f else 0f,
        label = "overflowAlpha"
    )

    IconButton(
        onClick = {
            showBottomSheetContent {
                LinkLogoutSheet(
                    onLogoutClick = {
                        showBottomSheetContent(null)
                        onLogout()
                    },
                    onCancelClick = {
                        showBottomSheetContent(null)
                    }
                )
            }
        },
        enabled = showOverflowMenu,
        modifier = Modifier
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
                    email = "email@example.com",
                ),
                onBackPressed = {},
                onLogout = {},
                showBottomSheetContent = {}
            )
        }
    }
}

@Preview
@Composable
private fun LinkAppBarNoEmail() {
    DefaultLinkTheme {
        Surface {
            LinkAppBar(
                state = LinkAppBarState(
                    navigationIcon = R.drawable.stripe_link_close,
                    showHeader = true,
                    showOverflowMenu = true,
                    email = null,
                ),
                onBackPressed = {},
                onLogout = {},
                showBottomSheetContent = {}
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
                    email = "email@example.com",
                ),
                onBackPressed = {},
                onLogout = {},
                showBottomSheetContent = {}
            )
        }
    }
}

@Preview
@Composable
private fun LinkAppBarChildScreenNoEmail() {
    DefaultLinkTheme {
        Surface {
            LinkAppBar(
                state = LinkAppBarState(
                    navigationIcon = R.drawable.stripe_link_back,
                    showHeader = false,
                    showOverflowMenu = false,
                    email = null,
                ),
                onBackPressed = {},
                onLogout = {},
                showBottomSheetContent = {}
            )
        }
    }
}
