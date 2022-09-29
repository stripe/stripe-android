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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors

@Composable
internal fun FinancialConnectionsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: FinancialConnectionsButton.Type = FinancialConnectionsButton.Type.Primary,
    size: FinancialConnectionsButton.Size = FinancialConnectionsButton.Size.Regular,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable (RowScope.() -> Unit)
) {
    Button(
        onClick =  { if (!loading) onClick() },
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(size = size.radius),
        contentPadding = size.paddingValues(),
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

internal object FinancialConnectionsButton {

    internal sealed class Type {

        @Composable
        abstract fun buttonColors(): ButtonColors

        object Primary : Type() {
            @Composable
            override fun buttonColors(): ButtonColors {
                return buttonColors(
                    backgroundColor = colors.textBrand,
                    contentColor = colors.textWhite,
                    disabledBackgroundColor = colors.textBrand.copy(alpha = 0.12f),
                    disabledContentColor = colors.textWhite
                )
            }
        }

        object Secondary : Type() {
            @Composable
            override fun buttonColors(): ButtonColors {
                return buttonColors(
                    backgroundColor = colors.textWhite,
                    contentColor = colors.textPrimary,
                    disabledBackgroundColor = colors.textWhite,
                    disabledContentColor = colors.textPrimary.copy(alpha = 0.12f)
                )
            }
        }

        object Critical : Type() {
            @Composable
            override fun buttonColors(): ButtonColors {
                return buttonColors(
                    backgroundColor = colors.textCritical,
                    contentColor = colors.textWhite,
                    disabledBackgroundColor = colors.textCritical.copy(alpha = 0.12f),
                    disabledContentColor = colors.textPrimary.copy(alpha = 0.12f)
                )
            }
        }
    }

    sealed class Size {

        @Composable
        abstract fun paddingValues(): PaddingValues
        abstract val radius: Dp

        object Pill : Size() {
            override val radius: Dp = 4.dp

            @Composable
            override fun paddingValues(): PaddingValues = PaddingValues(
                start = 8.dp,
                top = 4.dp,
                end = 8.dp,
                bottom = 4.dp
            )
        }

        object Regular : Size() {
            override val radius: Dp = 12.dp

            @Composable
            override fun paddingValues(): PaddingValues = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp
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
            type = FinancialConnectionsButton.Type.Secondary,
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
            type = FinancialConnectionsButton.Type.Secondary,
            loading = false,
            enabled = false,
            onClick = { }
        ) {
            Text(text = "Sample text")
        }
    }
}

@Composable
@Preview(group = "Components", name = "Pill button - critical - disabled")
internal fun FinancialConnectionsButtonCriticalPreview() {
    FinancialConnectionsTheme {
        FinancialConnectionsButton(
            type = FinancialConnectionsButton.Type.Critical,
            size = FinancialConnectionsButton.Size.Pill,
            loading = false,
            enabled = true,
            onClick = { }
        ) {
            Text(text = "Sample text")
        }
    }
}
