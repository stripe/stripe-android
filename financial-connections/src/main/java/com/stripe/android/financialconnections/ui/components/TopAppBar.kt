@file:Suppress("ktlint:filename")

package com.stripe.android.financialconnections.ui.components

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalNavHostController
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun FinancialConnectionsTopAppBar(
    title: @Composable () -> Unit = {
        Icon(
            painter = painterResource(id = R.drawable.stripe_logo),
            contentDescription = null // decorative element
        )
    },
    showBack: Boolean = true,
    onCloseClick: () -> Unit
) {
    val localBackPressed = LocalOnBackPressedDispatcherOwner.current
        ?.onBackPressedDispatcher
    val navController = LocalNavHostController.current
    TopAppBar(
        title = title,
        navigationIcon = if (navController.previousBackStackEntry != null && showBack) {
            {
                IconButton(onClick = { localBackPressed?.onBackPressed() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back icon",
                        tint = FinancialConnectionsTheme.colors.textSecondary
                    )
                }
            }
        } else null,
        actions = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close icon",
                    tint = FinancialConnectionsTheme.colors.textSecondary
                )
            }
        },
        backgroundColor = FinancialConnectionsTheme.colors.textWhite,
        contentColor = FinancialConnectionsTheme.colors.textBrand
    )
}

@Composable
@Preview(group = "Components", name = "TopAppBar - idle")
internal fun FinancialConnectionsTopAppBarPreview() {
    FinancialConnectionsPreview {
        FinancialConnectionsTopAppBar(
            title = {}
        ) {}
    }
}
