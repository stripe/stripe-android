@file:Suppress("ktlint:filename")

package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun FinancialConnectionsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (RowScope.() -> Unit)
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 16.dp
        ),
        colors = buttonColors(
            backgroundColor = FinancialConnectionsTheme.colors.textBrand,
            contentColor = FinancialConnectionsTheme.colors.textWhite,
            disabledBackgroundColor = FinancialConnectionsTheme.colors.textDisabled,
            disabledContentColor = FinancialConnectionsTheme.colors.textWhite
        ),
        content = {
            ProvideTextStyle(
                value = FinancialConnectionsTheme.typography.bodyEmphasized
            ) {
                content()
            }
        }
    )
}

@Composable
internal fun FinancialConnectionsTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (RowScope.() -> Unit)
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 16.dp
        ),
        colors = ButtonDefaults.textButtonColors(
            contentColor = FinancialConnectionsTheme.colors.textBrand,
            disabledContentColor = Color.Transparent
        ),
        content = {
            ProvideTextStyle(
                value = FinancialConnectionsTheme.typography.bodyEmphasized
            ) {
                content()
            }
        }
    )
}
