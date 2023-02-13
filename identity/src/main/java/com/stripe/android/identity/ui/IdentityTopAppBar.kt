package com.stripe.android.identity.ui

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
                        IdentityTopBarState.CONSENT -> painterResource(id = R.drawable.ic_baseline_close_24)
                        IdentityTopBarState.CONFIRMATION -> painterResource(id = R.drawable.ic_baseline_close_24)
                        IdentityTopBarState.ERROR_SHOULD_FAIL -> painterResource(id = R.drawable.ic_baseline_close_24)
                        IdentityTopBarState.DEFAULT -> painterResource(id = R.drawable.ic_baseline_arrow_back_24)
                        IdentityTopBarState.INDIVIDUAL_STANDALONE ->
                            painterResource(id = R.drawable.ic_baseline_close_24)
                    },
                    contentDescription = when (topBarState) {
                        IdentityTopBarState.CONSENT -> stringResource(id = R.string.description_close)
                        IdentityTopBarState.CONFIRMATION -> stringResource(id = R.string.description_close)
                        IdentityTopBarState.ERROR_SHOULD_FAIL -> stringResource(id = R.string.description_close)
                        IdentityTopBarState.DEFAULT -> stringResource(id = R.string.description_go_back)
                        IdentityTopBarState.INDIVIDUAL_STANDALONE -> stringResource(id = R.string.description_close)
                    }
                )
            }
        }
    )
}

// TODO(ccen) - change this to just two state -> go back/close
internal enum class IdentityTopBarState {
    CONSENT, CONFIRMATION, ERROR_SHOULD_FAIL, DEFAULT, INDIVIDUAL_STANDALONE
}
