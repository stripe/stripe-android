package com.stripe.android.financialconnections.ui.components

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.R
import android.view.HapticFeedbackConstants.CONFIRM
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.financialconnections.features.common.LoadingSpinner
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type.Primary
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type.Secondary
import com.stripe.android.financialconnections.ui.theme.Brand400
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.Neutral0
import com.stripe.android.financialconnections.ui.theme.Neutral50
import com.stripe.android.financialconnections.ui.theme.Theme

private val DefaultSpinnerHeight = 24.dp

@Composable
internal fun FinancialConnectionsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: Type = Primary,
    size: FinancialConnectionsButton.Size = FinancialConnectionsButton.Size.Regular,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable (RowScope.() -> Unit)
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val colors = FinancialConnectionsTheme.colors

    val multipleEventsCutter = remember { MultipleEventsCutter.get() }
    var spinnerHeight by remember { mutableStateOf(DefaultSpinnerHeight) }

    val loadingIndicatorAlpha by animateFloatAsState(
        targetValue = if (loading) 1f else 0f,
        label = "LoadingIndicatorAlpha",
    )

    val spinnerBrush = remember {
        // We need to flip the direction of the gradient when rendering in a primary button
        // due to its background color. Otherwise, the spinner looks inverted.
        when (type) {
            Primary -> Brush.sweepGradient(listOf(colors.borderBrand, colors.contentOnBrand))
            Secondary -> Brush.sweepGradient(listOf(colors.buttonSecondary, colors.textDefault))
        }
    }

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
            contentPadding = PaddingValues(0.dp),
            colors = type.buttonColors(),
            content = {
                ProvideTextStyle(
                    value = typography.labelLargeEmphasized.copy(
                        // material button adds letter spacing internally, this removes it.
                        letterSpacing = 0.sp
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(
                            modifier = Modifier
                                .alpha(1f - loadingIndicatorAlpha)
                                .padding(size.paddingValues())
                                .onSizeChanged {
                                    // Set the spinner to the same height as the label,
                                    // so we avoid visual jitter.
                                    spinnerHeight = with(density) { it.height.toDp() }
                                },
                            content = content,
                        )

                        LoadingSpinner(
                            gradient = spinnerBrush,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(spinnerHeight)
                                .alpha(loadingIndicatorAlpha),
                        )
                    }
                }
            }
        )
    }
}

private fun Type.rippleTheme() = object : RippleTheme {
    @Composable
    override fun defaultColor() = when (this@rippleTheme) {
        Primary -> Neutral0
        Secondary -> colors.textDefault
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

        data object Primary : Type() {
            @Composable
            override fun buttonColors(): ButtonColors = buttonColors(
                backgroundColor = colors.buttonPrimary,
                contentColor = colors.contentOnBrand,
                disabledBackgroundColor = colors.buttonPrimary,
                disabledContentColor = colors.contentOnBrand.copy(alpha = 0.4f)
            )

            override fun rippleColor(): Color = Brand400

            @Composable
            override fun elevation(): ButtonElevation = ButtonDefaults.elevation()
        }

        data object Secondary : Type() {
            @Composable
            override fun buttonColors(): ButtonColors = buttonColors(
                backgroundColor = colors.buttonSecondary,
                contentColor = colors.textDefault,
                disabledBackgroundColor = colors.buttonSecondary,
                disabledContentColor = colors.textDefault.copy(alpha = 0.4f)
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

        data object Regular : Size() {
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

internal class ThemePreviewParameterProvider : CollectionPreviewParameterProvider<Theme>(Theme.entries)

@Preview(group = "Components", name = "Button - primary - idle")
@Composable
internal fun FinancialConnectionsButtonPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme,
) {
    FinancialConnectionsPreview(theme) {
        Column(
            modifier = Modifier
                .background(colors.backgroundSurface)
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
                type = Secondary,
                loading = false
            ) {
                Text(text = "Secondary")
            }
            FinancialConnectionsButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                type = Secondary,
                enabled = false,
                loading = false
            ) {
                Text(text = "Secondary disabled")
            }
            FinancialConnectionsButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                type = Secondary,
                loading = true,
            ) {
                Text(text = "Secondary loading")
            }
        }
    }
}
