@file:Suppress("ktlint:filename")

package com.stripe.android.financialconnections.ui.components

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.R
import android.view.HapticFeedbackConstants.CONFIRM
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.ButtonElevation
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.financialconnections.features.common.V3LoadingSpinner
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type
import com.stripe.android.financialconnections.ui.theme.Brand400
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import com.stripe.android.financialconnections.ui.theme.Neutral0
import com.stripe.android.financialconnections.ui.theme.Neutral50

@Composable
internal fun FinancialConnectionsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: Type = Type.Primary,
    size: FinancialConnectionsButton.Size = FinancialConnectionsButton.Size.Regular,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable (RowScope.() -> Unit)
) {
    val view = LocalView.current
    val multipleEventsCutter = remember { MultipleEventsCutter.get() }
    CompositionLocalProvider(LocalRippleTheme provides type.rippleTheme()) {
        Button(
            onClick = {
                multipleEventsCutter.processEvent {
                    if (loading.not()) {
                        if (SDK_INT >= R) view.performHapticFeedback(CONFIRM)
                        onClick()
                    }
                }
            },
            modifier = modifier,
            elevation = type.elevation(),
            enabled = enabled,
            shape = RoundedCornerShape(size = size.radius),
            contentPadding = size.paddingValues(),
            colors = type.buttonColors(),
            content = {
                ProvideTextStyle(
                    value = v3Typography.labelLargeEmphasized.copy(
                        // material button adds letter spacing internally, this removes it.
                        letterSpacing = 0.sp
                    )
                ) {
                    Row {
                        if (loading) {
                            V3LoadingSpinner(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            content()
                        }
                    }
                }
            }
        )
    }
}

private fun Type.rippleTheme() = object : RippleTheme {
    @Composable
    override fun defaultColor() = when (this@rippleTheme) {
        Type.Primary -> Neutral0
        Type.Secondary -> v3Colors.textDefault
    }

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleTheme.defaultRippleAlpha(
        buttonColors().contentColor(enabled = true).value,
        lightTheme = true
    )
}

internal object FinancialConnectionsButton {

    internal sealed class Type {

        @Composable
        abstract fun buttonColors(): ButtonColors
        abstract fun rippleColor(): Color

        @Composable
        abstract fun elevation(): ButtonElevation

        object Primary : Type() {
            @Composable
            override fun buttonColors(): ButtonColors = buttonColors(
                backgroundColor = v3Colors.iconBrand,
                contentColor = v3Colors.textWhite,
                disabledBackgroundColor = v3Colors.iconBrand,
                disabledContentColor = v3Colors.textWhite.copy(alpha = 0.4f)
            )

            override fun rippleColor(): Color = Brand400

            @Composable
            override fun elevation(): ButtonElevation = ButtonDefaults.elevation()
        }

        object Secondary : Type() {
            @Composable
            override fun buttonColors(): ButtonColors = buttonColors(
                backgroundColor = Neutral50,
                contentColor = v3Colors.textDefault,
                disabledBackgroundColor = Neutral50,
                disabledContentColor = v3Colors.textDefault.copy(alpha = 0.4f)
            )

            override fun rippleColor(): Color = Neutral50

            @Composable
            override fun elevation(): ButtonElevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp,
            )
        }
    }

    sealed class Size {

        @Composable
        abstract fun paddingValues(): PaddingValues
        abstract val radius: Dp

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

@Preview(group = "Components", name = "Button - primary - idle")
@Composable
internal fun FinancialConnectionsButtonPreview() {
    FinancialConnectionsPreview {
        Column(
            modifier = Modifier
                .background(v3Colors.backgroundSurface)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            FinancialConnectionsButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                loading = false
            ) {
                Text(text = "Primary")
            }
            FinancialConnectionsButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                loading = true
            ) {
                Text(text = "Primary - loading")
            }
            FinancialConnectionsButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            ) {
                Text(text = "Primary - disabled")
            }
            FinancialConnectionsButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                type = Type.Secondary,
                loading = false
            ) {
                Text(text = "Secondary")
            }
            FinancialConnectionsButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                type = Type.Secondary,
                enabled = false,
                loading = false
            ) {
                Text(text = "Secondary disabled")
            }
        }
    }
}
