package com.stripe.android.common.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import java.util.UUID
import kotlin.math.roundToInt

@Composable
internal fun AnimatedContentHeight(
    animationSpec: AnimationSpec<Float> = spring(),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val parentState = LocalAnimatedContentSizeState.current
    val state = remember { AnimatedContentSizeState(parentState) }

    var didFirstLayout by remember { mutableStateOf(false) }
    var targetHeight by remember { mutableFloatStateOf(-1f) }
    val heightAnimation = remember { Animatable(0f) }
    var activeAnimationId by remember { mutableStateOf<AnimatedContentSizeStateId?>(null) }

    LaunchedEffect(targetHeight) {
        if (targetHeight < 0f) {
            return@LaunchedEffect
        }
        if (!didFirstLayout || !state.isEnabled) {
            didFirstLayout = true
            heightAnimation.snapTo(targetHeight)
        } else {
            activeAnimationId?.let { parentState?.unregisterAnimation(it) }
            activeAnimationId = parentState?.registerAnimation()
            heightAnimation.animateTo(targetHeight, animationSpec = animationSpec) {
                if (value == targetValue) {
                    activeAnimationId?.let { parentState?.unregisterAnimation(it) }
                    activeAnimationId = null
                }
            }
        }
    }

    DisposableEffect(activeAnimationId) {
        val animationId = activeAnimationId
        onDispose {
            animationId?.let { parentState?.unregisterAnimation(it) }
        }
    }
    CompositionLocalProvider(LocalAnimatedContentSizeState provides state) {
        Box(
            modifier = modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    if (!constraints.hasBoundedHeight) {
                        targetHeight = placeable.height.toFloat()
                    }

                    val height =
                        if (state.isEnabled && !constraints.hasBoundedHeight) {
                            heightAnimation.value.roundToInt()
                                .takeIf { it > 0 }
                                ?: placeable.height
                        } else {
                            placeable.height
                        }
                    layout(placeable.width, height) {
                        placeable.place(0, 0)
                    }
                },
            content = { content() }
        )
    }
}

internal val LocalAnimatedContentSizeState = compositionLocalOf<AnimatedContentSizeState?> { null }

internal class AnimatedContentSizeState(private val parent: AnimatedContentSizeState?) {
    val isEnabled: Boolean get() = childAnimations.isEmpty()

    private var childAnimations by mutableStateOf(emptySet<AnimatedContentSizeStateId>())

    fun registerAnimation(): AnimatedContentSizeStateId {
        return AnimatedContentSizeStateId().also {
            childAnimations += it
            parent?.registerAnimation(it)
        }
    }

    fun unregisterAnimation(id: AnimatedContentSizeStateId) {
        childAnimations -= id
    }

    private fun registerAnimation(id: AnimatedContentSizeStateId) {
        parent?.registerAnimation(id)
    }
}

@JvmInline
internal value class AnimatedContentSizeStateId private constructor(val value: String) {
    constructor() : this(UUID.randomUUID().toString())
}
