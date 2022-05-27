package com.stripe.android.link.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.R
import com.stripe.android.link.theme.AppBarHeight
import com.stripe.android.link.theme.CloseIconWidth
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors

@Preview
@Composable
internal fun LinkAppBar() {
    DefaultLinkTheme {
        Surface {
            LinkAppBar(
                email = "email@example.com",
                onCloseButtonClick = {}
            )
        }
    }
}

@Composable
internal fun LinkAppBar(
    email: String?,
    onCloseButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .defaultMinSize(minHeight = AppBarHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(CloseIconWidth))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_link_logo),
                    contentDescription = stringResource(R.string.link),
                    tint = MaterialTheme.linkColors.linkLogo
                )
            }

            Box(
                modifier = Modifier
                    .width(CloseIconWidth)
                    .clickable(onClick = onCloseButtonClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_link_close),
                    contentDescription = stringResource(id = R.string.close),
                    tint = MaterialTheme.linkColors.closeButton
                )
            }
        }

        AnimatedVisibility(visible = !email.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = email.orEmpty(),
                    color = MaterialTheme.linkColors.disabledText
                )
            }
        }
    }
}
