package com.stripe.android.link.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import com.stripe.android.link.R
import com.stripe.android.link.theme.AppBarHeight
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.MinimumTouchTargetSize
import com.stripe.android.link.theme.linkColors

@Composable
internal fun LinkAppBar(
    email: String?,
    isRootScreen: Boolean,
    hideLinkHeader: Boolean,
    onButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppBarHeight),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        IconButton(
            onClick = onButtonClick,
            modifier = Modifier
                .padding(4.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isRootScreen) {
                        R.drawable.ic_link_close
                    } else {
                        R.drawable.ic_link_back
                    }
                ),
                contentDescription = stringResource(id = R.string.back),
                tint = MaterialTheme.linkColors.closeButton
            )
        }

        val contentAlpha by animateFloatAsState(targetValue = if (hideLinkHeader) 0f else 1f)

        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(contentAlpha)
                .padding(top = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_link_logo),
                contentDescription = stringResource(R.string.link),
                tint = MaterialTheme.linkColors.linkLogo
            )

            val showEmail = isRootScreen && !email.isNullOrEmpty()
            AnimatedVisibility(visible = showEmail) {
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

        Spacer(modifier = Modifier.width(MinimumTouchTargetSize))
    }
}

@Preview
@Composable
private fun LinkAppBar() {
    DefaultLinkTheme {
        Surface {
            LinkAppBar(
                email = "email@example.com",
                isRootScreen = true,
                hideLinkHeader = false,
                onButtonClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun LinkAppBar_NoEmail() {
    DefaultLinkTheme {
        Surface {
            LinkAppBar(
                email = null,
                isRootScreen = true,
                hideLinkHeader = false,
                onButtonClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun LinkAppBar_ChildScreen() {
    DefaultLinkTheme {
        Surface {
            LinkAppBar(
                email = "email@example.com",
                isRootScreen = false,
                hideLinkHeader = true,
                onButtonClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun LinkAppBar_ChildScreen_NoEmail() {
    DefaultLinkTheme {
        Surface {
            LinkAppBar(
                email = null,
                isRootScreen = false,
                hideLinkHeader = true,
                onButtonClick = {}
            )
        }
    }
}
