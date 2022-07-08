@file:Suppress("ktlint:filename")

package com.stripe.android.financialconnections.ui.components

import androidx.compose.material.Icon
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun FinancialConnectionsTopAppBar(
    title: @Composable () -> Unit = {
        Icon(
            painter = painterResource(id = R.drawable.stripe_logo),
            contentDescription = null // decorative element
        )
    },
    navigationIcon: @Composable (() -> Unit)? = null
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        backgroundColor = FinancialConnectionsTheme.colors.textWhite,
        contentColor = FinancialConnectionsTheme.colors.textBrand,
        elevation = 0.dp
    )
}

@Composable
@Preview
private fun FinancialConnectionsTopAppBarPreview() {
    FinancialConnectionsTheme {
        FinancialConnectionsTopAppBar()
    }
}
