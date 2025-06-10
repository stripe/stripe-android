package com.stripe.android.link.utils

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

internal const val LINK_SCREEN_SIZE_ANIMATION_DURATION_MILLIS: Int =
    AnimationConstants.DefaultDurationMillis

// Approximate duration in milliseconds (plus some buffer) of a soft keyboard (IME) toggle animation.
// We *could* get the exact value using `ViewCompat.setWindowInsetsAnimationCallback()` however
// we can't do so without permanently overriding the existing callback (there is no associated
// getter).
private const val LINK_IME_ANIMATION_DURATION_MILLIS: Int =
    AnimationConstants.DefaultDurationMillis + 50

/**
 * Delay in milliseconds to allow animations to complete before performing
 * additional animations on a screen. Without delaying, clashing animations may
 * run concurrently, causing stuttering/jank.
 */
internal const val LINK_DEFAULT_ANIMATION_DELAY_MILLIS: Long =
    (LINK_SCREEN_SIZE_ANIMATION_DURATION_MILLIS + LINK_IME_ANIMATION_DURATION_MILLIS).toLong()

internal val LinkScreenTransition: ContentTransform =
    ContentTransform(
        targetContentEnter = fadeIn(tween(durationMillis = 300)),
        initialContentExit = fadeOut(tween(durationMillis = 300)),
        sizeTransform = SizeTransform(
            sizeAnimationSpec = { _, _ ->
                tween(
                    durationMillis = LINK_SCREEN_SIZE_ANIMATION_DURATION_MILLIS,
                    easing = FastOutSlowInEasing
                )
            }
        )
    )
