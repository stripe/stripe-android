@file:Suppress("ktlint:filename")

package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
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
    type: FinancialConnectionsButtonType = FinancialConnectionsButtonType.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable (RowScope.() -> Unit)
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 16.dp
        ),
        colors = type.buttonColors(),
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

internal sealed class FinancialConnectionsButtonType {

    @Composable
    abstract fun buttonColors(): ButtonColors

    object Primary : FinancialConnectionsButtonType() {
        @Composable
        override fun buttonColors(): ButtonColors {
            return buttonColors(
                backgroundColor = colors.textBrand,
                contentColor = colors.textWhite,
                disabledBackgroundColor = colors.textBrand.copy(alpha = 0.12f),
                disabledContentColor = colors.textWhite,
            )
        }
    }

    object Secondary : FinancialConnectionsButtonType() {
        @Composable
        override fun buttonColors(): ButtonColors {
            return buttonColors(
                backgroundColor = colors.textWhite,
                contentColor = colors.textPrimary,
                disabledBackgroundColor = colors.textWhite,
                disabledContentColor = colors.textPrimary.copy(alpha = 0.12f),
            )
        }
    }
}

@Composable
@Preview(group = "Components", name = "Button - primary - idle")
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

@Composable
@Preview(group = "Components", name = "Button - primary - loading")
internal fun FinancialConnectionsButtonLoadingPreview() {
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
@Preview(group = "Components", name = "Button - secondary - idle")
internal fun FinancialConnectionsButtonSecondaryPreview() {
    FinancialConnectionsTheme {
        FinancialConnectionsButton(
            type = FinancialConnectionsButtonType.Secondary,
            loading = false,
            onClick = { }
        ) {
            Text(text = "Sample text")
        }
    }
}

@Composable
@Preview(group = "Components", name = "Button - secondary - disabled")
internal fun FinancialConnectionsButtonSecondaryDisabledPreview() {
    FinancialConnectionsTheme {
        FinancialConnectionsButton(
            type = FinancialConnectionsButtonType.Secondary,
            loading = false,
            enabled = false,
            onClick = { }
        ) {
            Text(text = "Sample text")
        }
    }
}
