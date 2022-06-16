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
internal fun FinancialConnectionsTopAppBar() {
    TopAppBar(
        title = {
            Icon(
                painter = painterResource(id = R.drawable.logo__stripe),
                contentDescription = null // decorative element
            )
        },
        backgroundColor = FinancialConnectionsTheme.colors.textWhite,
        contentColor = FinancialConnectionsTheme.colors.textBrand,
        elevation = 12.dp
    )
}

@Composable
@Preview
private fun FinancialConnectionsTopAppBarPreview() {
    FinancialConnectionsTheme {
        FinancialConnectionsTopAppBar()
    }
}
