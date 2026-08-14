package com.stripe.android.financialconnections.ui.components

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.R
import android.view.HapticFeedbackConstants.CONFIRM
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.RippleDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.financialconnections.features.common.LoadingSpinner
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type.Primary
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type.Secondary
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type.SecondaryOutlined
import com.stripe.android.financialconnections.ui.theme.Brand400
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.Neutral0
import com.stripe.android.financialconnections.ui.theme.Neutral50
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.financialconnections.ui.theme.isLinkDs3

private val DefaultSpinnerHeight = 24.dp

/**
 * Link DS 3.0 primary button shadow: `rgba(48, 49, 61, 0.12)`, 2dp offset, 2.5dp blur. Compose can't
 * express offset, blur and opacity independently, so this is approximated with an elevation. Note
 * [Modifier.shadow]'s ambient/spot colors are only honored on API 28+; below that the platform
 * falls back to a neutral shadow.
 */
private val LinkDs3ShadowColor = Color(0xFF30313D)
private val LinkDs3ShadowElevation = 2.dp

/** Cosmetic sheen drawn over the primary button background in Link DS 3.0. */
private val LinkDs3SheenBrush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
)

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
    val isLinkDs3 = FinancialConnectionsTheme.theme.isLinkDs3

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
            Primary -> Brush.sweepGradient(listOf(colors.primary, colors.primaryAccent))
            Secondary,
            SecondaryOutlined -> Brush.sweepGradient(listOf(colors.backgroundSecondary, colors.textDefault))
        }
    }

    val shape = type.shape()
    val showSheen = isLinkDs3 && type == Primary

    CompositionLocalProvider(LocalRippleConfiguration provides type.rippleConfiguration()) {
        Button(
            onClick = {
                multipleEventsCutter.processEvent {
                    if (loading.not()) {
                        if (SDK_INT >= R) view.performHapticFeedback(CONFIRM)
                        onClick()
                    }
                }
            },
            modifier = modifier.then(type.shadow(shape)),
            elevation = type.elevation(),
            enabled = enabled,
            shape = shape,
            border = type.border(),
            contentPadding = PaddingValues(0.dp),
            colors = type.buttonColors(),
            content = {
                ProvideTextStyle(
                    value = typography.labelLargeEmphasized.copy(
                        // material button adds letter spacing internally, this removes it.
                        letterSpacing = 0.sp
                    )
                ) {
                    Box(
                        // The sheen uses `matchParentSize`, so the box has to span the full button
                        // for the gradient to cover it.
                        modifier = if (showSheen) Modifier.fillMaxWidth() else Modifier,
                        contentAlignment = Alignment.Center,
                    ) {
                        if (showSheen) {
                            // Drawn first so it sits above the button background but below the
                            // label. Non-interactive: it doesn't affect the touch target.
                            Spacer(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(brush = LinkDs3SheenBrush, shape = shape)
                            )
                        }

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

@Composable
private fun Type.rippleConfiguration(): RippleConfiguration? {
    // Link DS 3.0 secondary buttons have no pressed highlight at all.
    if (FinancialConnectionsTheme.theme.isLinkDs3 && this != Primary) {
        return null
    }
    return RippleConfiguration(
        color = when (this) {
            Primary -> Neutral0
            Secondary,
            SecondaryOutlined -> colors.textDefault
        },
        rippleAlpha = RippleDefaults.rippleAlpha(
            contentColor = buttonColors().contentColor(enabled = true).value,
            lightTheme = true
        )
    )
}

internal object FinancialConnectionsButton {

    internal sealed class Type {

        @Composable
        abstract fun buttonColors(): ButtonColors
        abstract fun rippleColor(): Color

        @Composable
        abstract fun elevation(): ButtonElevation

        /** The corner shape. Varies by type, since Link DS 3.0 only pills some of them. */
        @Composable
        abstract fun shape(): Shape

        /** An optional outline. Only [SecondaryOutlined] has one. */
        @Composable
        open fun border(): BorderStroke? = null

        /**
         * An optional shadow drawn outside the button. Used instead of [elevation] where the shadow
         * color needs controlling.
         */
        @Composable
        open fun shadow(shape: Shape): Modifier = Modifier

        data object Primary : Type() {
            @Composable
            override fun buttonColors(): ButtonColors = buttonColors(
                backgroundColor = colors.primary,
                contentColor = colors.primaryAccent,
                disabledBackgroundColor = colors.primary,
                disabledContentColor = colors.primaryAccent.copy(alpha = 0.4f)
            )

            override fun rippleColor(): Color = Brand400

            @Composable
            override fun shape(): Shape = if (FinancialConnectionsTheme.theme.isLinkDs3) {
                // A percentage rather than `height / 2` keeps the pill correct if the label wraps
                // or the user scales text up.
                RoundedCornerShape(percent = 50)
            } else {
                RoundedCornerShape(size = 12.dp)
            }

            @Composable
            override fun elevation(): ButtonElevation =
                if (FinancialConnectionsTheme.theme.isLinkDs3) {
                    // DS 3.0 draws its own shadow in [shadow] so it can control the color.
                    ButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp,
                    )
                } else {
                    ButtonDefaults.elevation()
                }

            @Composable
            override fun shadow(shape: Shape): Modifier =
                if (FinancialConnectionsTheme.theme.isLinkDs3) {
                    Modifier.shadow(
                        elevation = LinkDs3ShadowElevation,
                        shape = shape,
                        ambientColor = LinkDs3ShadowColor,
                        spotColor = LinkDs3ShadowColor,
                    )
                } else {
                    Modifier
                }
        }

        data object Secondary : Type() {
            @Composable
            override fun buttonColors(): ButtonColors {
                // DS 3.0 secondary buttons have no fill at all.
                val background = if (FinancialConnectionsTheme.theme.isLinkDs3) {
                    Color.Transparent
                } else {
                    colors.backgroundSecondary
                }
                return buttonColors(
                    backgroundColor = background,
                    contentColor = colors.textDefault,
                    disabledBackgroundColor = background,
                    disabledContentColor = colors.textDefault.copy(alpha = 0.4f)
                )
            }

            override fun rippleColor(): Color = Neutral50

            // Unlike the primary button, this keeps a 12.dp radius in every theme.
            @Composable
            override fun shape(): Shape = RoundedCornerShape(size = 12.dp)

            @Composable
            override fun elevation(): ButtonElevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp,
            )
        }

        /**
         * A pill-shaped, transparent secondary button with a hairline outline. Used for the "Cancel"
         * action in the Link DS 3.0 warmup sheet's side-by-side footer.
         */
        data object SecondaryOutlined : Type() {
            @Composable
            override fun buttonColors(): ButtonColors = buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = colors.textDefault,
                disabledBackgroundColor = Color.Transparent,
                disabledContentColor = colors.textDefault.copy(alpha = 0.4f)
            )

            override fun rippleColor(): Color = Neutral50

            @Composable
            override fun shape(): Shape = RoundedCornerShape(percent = 50)

            @Composable
            override fun border(): BorderStroke = BorderStroke(
                width = 0.5.dp,
                color = colors.border,
            )

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

        data object Regular : Size() {

            @Composable
            override fun paddingValues(): PaddingValues =
                if (FinancialConnectionsTheme.theme.isLinkDs3) {
                    // 14 + 24sp line height + 14 ≈ 52.dp, versus 56.dp elsewhere. Expressed as
                    // padding rather than a fixed height so the button can still grow when the
                    // user scales text up.
                    PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                } else {
                    PaddingValues(all = 16.dp)
                }
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
                .background(colors.background)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            PreviewButton(label = "Primary", type = Primary)
            PreviewButton(label = "Primary - loading", type = Primary, loading = true)
            PreviewButton(label = "Primary - disabled", type = Primary, enabled = false)
            PreviewButton(label = "Secondary", type = Secondary)
            PreviewButton(label = "Secondary disabled", type = Secondary, enabled = false)
            PreviewButton(label = "Secondary loading", type = Secondary, loading = true)
            PreviewButton(label = "Secondary outlined", type = SecondaryOutlined)
        }
    }
}

@Composable
private fun PreviewButton(
    label: String,
    type: Type,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    FinancialConnectionsButton(
        onClick = { },
        modifier = Modifier.fillMaxWidth(),
        type = type,
        enabled = enabled,
        loading = loading,
    ) {
        Text(text = label)
    }
}
