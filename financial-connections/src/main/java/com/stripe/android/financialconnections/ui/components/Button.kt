@file:Suppress("ktlint:filename")

package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors

@Composable
internal fun FinancialConnectionsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    content: @Composable (RowScope.() -> Unit)
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = loading.not(),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 16.dp
        ),
        colors = buttonColors(
            backgroundColor = colors.textBrand,
            contentColor = colors.textWhite,
            disabledBackgroundColor = colors.textBrand.copy(alpha = 0.12f),
            disabledContentColor = colors.textWhite,
        ),
        content = {
            ProvideTextStyle(
                value = FinancialConnectionsTheme.typography.bodyEmphasized
            ) {
                Row {
                    if (loading) {
                        CircularProgressIndicator(
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(21.dp),
                            color = colors.textWhite
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    content()
                }
            }
        }
    )
}

@Composable
@Preview(group = "Components", name = "Button - loading")
internal fun FinancialConnectionsButtonDisabledPreview() {
    FinancialConnectionsTheme {
        FinancialConnectionsButton(
            loading = true,
            onClick = { }
        ) {
            Text(text = "Sample text")
        }
    }
}

@Composable
@Preview(group = "Components", name = "Button - idle")
internal fun FinancialConnectionsButtonPreview() {
    FinancialConnectionsTheme {
        FinancialConnectionsButton(
            loading = false,
            onClick = { }
        ) {
            Text(text = "Sample text")
        }
    }
}
