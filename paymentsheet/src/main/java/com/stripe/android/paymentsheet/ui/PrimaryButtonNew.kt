@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package com.stripe.android.paymentsheet.ui

import android.widget.Toast
import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.LoadingIndicator
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import kotlinx.coroutines.delay

internal const val PRIMARY_BUTTON_TEST_TAG = "PRIMARY_BUTTON_TEST_TAG"
private const val FADE_ANIMATION_DURATION = 100
private const val FADE_OUT_ANIMATION_DELAY = 90

private val fadeAnimation = fadeIn(animationSpec = tween(FADE_ANIMATION_DURATION)) togetherWith
    fadeOut(animationSpec = tween(FADE_ANIMATION_DURATION, FADE_OUT_ANIMATION_DELAY))

private const val PRE_SUCCESS_ANIMATION_DELAY = 250L
private const val POST_SUCCESS_ANIMATION_DELAY = 1500L

private const val RIGHT_ALIGNED = 1f
private const val CENTER_ALIGNED = 0f

internal sealed interface PrimaryButtonProcessingState {
    data class Idle(val error: ResolvableString?) : PrimaryButtonProcessingState

    object Processing : PrimaryButtonProcessingState

    object Completed : PrimaryButtonProcessingState
}

@Composable
internal fun PrimaryButton(
    label: String,
    locked: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    processingState: PrimaryButtonProcessingState = PrimaryButtonProcessingState.Idle(null),
    onProcessingCompleted: () -> Unit = {},
    onClick: () -> Unit,
) {
    val isProcessingCompleted = processingState is PrimaryButtonProcessingState.Completed

    val colors = PrimaryButtonTheme.colors
    val shape = PrimaryButtonTheme.shape

    val background = remember(isProcessingCompleted, colors) {
        if (isProcessingCompleted) {
            colors.successBackground
        } else {
            colors.background
        }
    }

    val animatedBackground = if (LocalInspectionMode.current) {
        background
    } else {
        animateColorAsState(
            targetValue = background,
            label = "BackgroundAnimation",
            animationSpec = tween(durationMillis = FADE_ANIMATION_DURATION, easing = LinearEasing)
        ).value
    }

    CompositionLocalProvider(
        LocalContentAlpha provides if (enabled) ContentAlpha.high else ContentAlpha.disabled,
    ) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(
                onClick = onClick,
                modifier = Modifier
                    .testTag(PRIMARY_BUTTON_TEST_TAG)
                    .fillMaxWidth()
                    .defaultMinSize(
                        minHeight = dimensionResource(id = R.dimen.stripe_paymentsheet_primary_button_height)
                    ),
                enabled = enabled,
                shape = RoundedCornerShape(shape.cornerRadius),
                border = BorderStroke(shape.borderStrokeWidth, colors.border),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = animatedBackground,
                    disabledBackgroundColor = animatedBackground,
                ),
            ) {
                Content(
                    label = label,
                    processingState = processingState,
                    locked = locked,
                    onProcessingCompleted = onProcessingCompleted,
                )
            }
        }
    }
}

@Composable
private fun Content(
    label: String,
    processingState: PrimaryButtonProcessingState,
    locked: Boolean,
    onProcessingCompleted: () -> Unit,
) {
    AnimatedContent(
        targetState = processingState is PrimaryButtonProcessingState.Completed,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        transitionSpec = { fadeAnimation },
        label = "ContentAnimation",
    ) { completed ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            if (completed) {
                AnimatedCompleteProcessing(onProcessingCompleted)
            } else {
                StaticIncompleteProcessing(
                    text = when (processingState) {
                        is PrimaryButtonProcessingState.Idle -> label
                        else -> stringResource(R.string.stripe_paymentsheet_primary_button_processing)
                    },
                    processing = processingState !is PrimaryButtonProcessingState.Idle,
                    locked = locked,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.StaticIncompleteProcessing(
    text: String,
    processing: Boolean,
    locked: Boolean,
) {
    val colors = PrimaryButtonTheme.colors
    val typography = PrimaryButtonTheme.typography

    val textStyle = TextStyle(
        fontFamily = typography.fontFamily ?: FontFamily.Default,
        fontSize = typography.fontSize,
        fontWeight = FontWeight.Medium,
    )

    val onBackground = colors.onBackground.copy(LocalContentAlpha.current)

    Text(
        text = text,
        color = onBackground,
        style = textStyle,
        modifier = Modifier.align(Alignment.Center),
    )

    if (processing) {
        LoadingIndicator(
            color = onBackground,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    } else if (locked) {
        Icon(
            painter = painterResource(
                id = R.drawable.stripe_ic_paymentsheet_googlepay_primary_button_lock
            ),
            tint = onBackground,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun BoxScope.AnimatedCompleteProcessing(
    onAnimationCompleted: () -> Unit
) {
    val inInspectionMode = LocalInspectionMode.current

    var animationCompleted by remember {
        mutableStateOf(false)
    }

    var alignment by remember {
        mutableFloatStateOf(
            if (inInspectionMode) {
                CENTER_ALIGNED
            } else {
                RIGHT_ALIGNED
            }
        )
    }

    val animatedAlignment by animateFloatAsState(
        targetValue = alignment,
        label = "CheckmarkAnimation",
        finishedListener = { alignmentValue ->
            if (alignmentValue == CENTER_ALIGNED) {
                animationCompleted = true
            }
        }
    )

    val currentOnAnimationCompleted by rememberUpdatedState(onAnimationCompleted)

    if (!inInspectionMode) {
        LaunchedEffect(Unit) {
            delay(PRE_SUCCESS_ANIMATION_DELAY)

            alignment = CENTER_ALIGNED
        }

        LaunchedEffect(animationCompleted, currentOnAnimationCompleted) {
            if (animationCompleted) {
                delay(POST_SUCCESS_ANIMATION_DELAY)
                currentOnAnimationCompleted()
            }
        }
    } else {
        LaunchedEffect(currentOnAnimationCompleted) {
            currentOnAnimationCompleted()
        }
    }

    Icon(
        painterResource(R.drawable.stripe_ic_paymentsheet_googlepay_primary_button_checkmark),
        modifier = Modifier.align(
            BiasAlignment(
                horizontalBias = animatedAlignment,
                verticalBias = CENTER_ALIGNED
            )
        ),
        tint = PrimaryButtonTheme.colors.onSuccessBackground,
        contentDescription = null
    )
}

@Composable
@Preview(showBackground = true)
private fun PrimaryButtonPreview() {
    var processingState by remember {
        mutableStateOf<PrimaryButtonProcessingState>(PrimaryButtonProcessingState.Idle(null))
    }

    StripeTheme {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = processingState is PrimaryButtonProcessingState.Idle,
                    onClick = {
                        processingState = PrimaryButtonProcessingState.Idle(null)
                    }
                )
                Text(text = "Idle")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = processingState is PrimaryButtonProcessingState.Processing,
                    onClick = {
                        processingState = PrimaryButtonProcessingState.Processing
                    }
                )
                Text(text = "Processing")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = processingState is PrimaryButtonProcessingState.Completed,
                    onClick = {
                        processingState = PrimaryButtonProcessingState.Completed
                    }
                )
                Text(text = "Completed")
            }

            val context = LocalContext.current

            PrimaryButton(
                label = "Pay $50.99",
                processingState = processingState,
                locked = false,
                enabled = true,
                onClick = {},
                onProcessingCompleted = {
                    Toast.makeText(context, "Completed", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
