package com.stripe.android.identity.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.stripe.android.identity.R
import com.stripe.android.identity.ui.IdentityTopBarState.CLOSE
import com.stripe.android.identity.ui.IdentityTopBarState.GO_BACK

@Composable
internal fun IdentityTopAppBar(
    topBarState: IdentityTopBarState,
    onTopBarNavigationClick: () -> Unit
) {
    val context = LocalContext.current
    val style = LocalTextStyle.current
    TopAppBar(
        title = {
            Text(
                text = context.applicationInfo.loadLabel(context.packageManager).toString(),
                style = style
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                onTopBarNavigationClick()
            }) {
                Icon(
                    painter = when (topBarState) {
                        GO_BACK -> painterResource(id = R.drawable.stripe_arrow_back)
                        CLOSE -> painterResource(id = R.drawable.stripe_close)
                    },
                    contentDescription = when (topBarState) {
                        GO_BACK -> stringResource(id = R.string.stripe_description_go_back)
                        CLOSE -> stringResource(id = R.string.stripe_description_close)
                    }
                )
            }
        },
        windowInsets = WindowInsets.statusBars
    )
}

/**
 * [GO_BACK]: Clicking home button goes to previous screen.
 * [CLOSE]: Clicking home button closes the verification session.
 */
internal enum class IdentityTopBarState {
    GO_BACK, CLOSE
}
