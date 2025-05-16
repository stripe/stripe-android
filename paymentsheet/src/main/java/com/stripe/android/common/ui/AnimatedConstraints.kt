package com.stripe.android.common.ui

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalAnimatableApi::class)
@Composable
internal fun AnimatedConstraints(
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<IntSize> = DefaultAnimateConstraintsAnimation,
    content: @Composable () -> Unit,
) {
    val sizeAnimation = remember { DeferredTargetAnimation(IntSize.VectorConverter) }
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = modifier.animateConstraints(sizeAnimation, coroutineScope, animationSpec)) {
        content()
    }
}

// Copied from the `approachLayout` docs:
// https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier
//
// Creates a custom modifier that animates the constraints and measures child with the
// animated constraints. This modifier is built on top of `Modifier.approachLayout` to approach
// th destination size determined by the lookahead pass. A resize animation will be kicked off
// whenever the lookahead size changes, to animate children from current size to destination
// size. Fixed constraints created based on the animation value will be used to measure
// child, so the child layout gradually changes its animated constraints until the approach
// completes.
@OptIn(ExperimentalAnimatableApi::class)
internal fun Modifier.animateConstraints(
    sizeAnimation: DeferredTargetAnimation<IntSize, AnimationVector2D>,
    coroutineScope: CoroutineScope,
    animationSpec: FiniteAnimationSpec<IntSize> = DefaultAnimateConstraintsAnimation,
) = this.approachLayout(
    isMeasurementApproachInProgress = { lookaheadSize ->
        // Update the target of the size animation.
        sizeAnimation.updateTarget(lookaheadSize, coroutineScope, animationSpec)
        // Return true if the size animation has pending target change or hasn't finished
        // running.
        !sizeAnimation.isIdle
    }
) { measurable, _ ->
    // In the measurement approach, the goal is to gradually reach the destination size
    // (i.e. lookahead size). To achieve that, we use an animation to track the current
    // size, and animate to the destination size whenever it changes. Once the animation
    // finishes, the approach is complete.

    // First, update the target of the animation, and read the current animated size.
    val (width, height) = sizeAnimation.updateTarget(lookaheadSize, coroutineScope, animationSpec)
    // Then create fixed size constraints using the animated size
    val animatedConstraints = Constraints.fixed(width, height)
    // Measure child with animated constraints.
    val placeable = measurable.measure(animatedConstraints)
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

internal val DefaultAnimateConstraintsAnimation get() = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntSize.VisibilityThreshold
)
