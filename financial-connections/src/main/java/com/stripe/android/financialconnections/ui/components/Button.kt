@file:Suppress("ktlint:filename")

package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type
import com.stripe.android.financialconnections.ui.theme.Brand400
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.Neutral50
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun FinancialConnectionsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: Type = Type.Primary,
    size: FinancialConnectionsButton.Size = FinancialConnectionsButton.Size.Regular,
    enabled: Boolean = true,
    loading: Boolean = false,
    debounceTime: Long = 500,
    content: @Composable (RowScope.() -> Unit)
) {

    // Remember a mutable state to store a debounce flag
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    CompositionLocalProvider(LocalRippleTheme provides type.rippleTheme()) {
        Button(
            onClick = {
                if (loading.not()) {
                    if (debounceJob == null || debounceJob?.isCompleted == true) {
                        debounceJob = coroutineScope.launch {
                            delay(debounceTime)
                            onClick()
                        }
                    }
                }
            },
            modifier = modifier,
            elevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp,
            ),
            enabled = enabled,
            shape = RoundedCornerShape(size = size.radius),
            contentPadding = size.paddingValues(),
            colors = type.buttonColors(),
            content = {
                ProvideTextStyle(
                    value = FinancialConnectionsTheme.typography.bodyEmphasized.copy(
                        // material button adds letter spacing internally, this removes it.
                        letterSpacing = 0.sp
                    )
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
}

private fun Type.rippleTheme() = object : RippleTheme {
    @Composable
    override fun defaultColor() = when (this@rippleTheme) {
        Type.Primary -> Color.White
        Type.Secondary -> colors.textSecondary
        Type.Critical -> Color.White
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

        object Primary : Type() {
            @Composable
            override fun buttonColors(): ButtonColors {
                return buttonColors(
                    backgroundColor = colors.textBrand,
                    contentColor = colors.textWhite,
                    disabledBackgroundColor = colors.textBrand,
                    disabledContentColor = colors.textWhite.copy(alpha = 0.3f)
                )
            }

            override fun rippleColor(): Color = Brand400
        }

        object Secondary : Type() {
            @Composable
            override fun buttonColors(): ButtonColors {
                return buttonColors(
                    backgroundColor = colors.backgroundContainer,
                    contentColor = colors.textPrimary,
                    disabledBackgroundColor = colors.backgroundContainer,
                    disabledContentColor = colors.textPrimary.copy(alpha = 0.12f)
                )
            }

            override fun rippleColor(): Color = Neutral50
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

            override fun rippleColor(): Color = Neutral50
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

@Preview(group = "Components", name = "Button - primary - idle")
@Composable
internal fun FinancialConnectionsButtonPreview() {
    FinancialConnectionsPreview {
        Column(
            modifier = Modifier
                .background(colors.backgroundSurface)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            FinancialConnectionsButton(
                modifier = Modifier.fillMaxWidth(),
                loading = false,
                onClick = { }
            ) {
                Text(text = "Primary")
            }
            FinancialConnectionsButton(
                modifier = Modifier.fillMaxWidth(),
                loading = true,
                onClick = { }
            ) {
                Text(text = "Primary - loading")
            }
            FinancialConnectionsButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                onClick = { }
            ) {
                Text(text = "Primary - disabled")
            }
            FinancialConnectionsButton(
                modifier = Modifier.fillMaxWidth(),
                type = Type.Secondary,
                loading = false,
                onClick = { }
            ) {
                Text(text = "Secondary")
            }
            FinancialConnectionsButton(
                modifier = Modifier.fillMaxWidth(),
                type = Type.Secondary,
                loading = false,
                enabled = false,
                onClick = { }
            ) {
                Text(text = "Secondary disabled")
            }
        }
    }
}

internal interface MultipleEventsCutter {
    fun processEvent(event: () -> Unit)

    companion object
}

internal fun MultipleEventsCutter.Companion.get(): MultipleEventsCutter =
    MultipleEventsCutterImpl()

private class MultipleEventsCutterImpl : MultipleEventsCutter {
    private val now: Long
        get() = System.currentTimeMillis()

    private var lastEventTimeMs: Long = 0

    override fun processEvent(event: () -> Unit) {
        if (now - lastEventTimeMs >= 500L) {
            event.invoke()
        }
        lastEventTimeMs = now
    }
}

fun Modifier.clickableSingle(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit

) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "clickable"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    },

    factory = {
        val multipleEventsCutter = remember { MultipleEventsCutter.get() }
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = { multipleEventsCutter.processEvent { onClick() } },
            role = role,
        )
    }
)

fun Modifier.clickableSingle(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit

) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "clickable"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    },

    factory = {
        val multipleEventsCutter = remember { MultipleEventsCutter.get() }
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = { multipleEventsCutter.processEvent { onClick() } },
            role = role,
        )
    }
)
