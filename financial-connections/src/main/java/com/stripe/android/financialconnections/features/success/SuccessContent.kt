package com.stripe.android.financialconnections.features.success

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.LongPress
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.LoadingSpinner
import com.stripe.android.financialconnections.features.success.SuccessState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.uicore.text.MiddleEllipsisText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ENTER_TRANSITION_DURATION_MS = 1000
private const val SLIDE_IN_ANIMATION_FRACTION = 4

private const val ICON_SIZE = 56
private const val SUCCESS_BODY_OFFSET = ICON_SIZE + 32

private val FADE_IN_ANIMATION = fadeIn(
    animationSpec = tween(ENTER_TRANSITION_DURATION_MS),
)

private val SUCCESS_SLIDE_IN_ANIMATION = FADE_IN_ANIMATION + slideInVertically(
    initialOffsetY = { fullHeight -> fullHeight / SLIDE_IN_ANIMATION_FRACTION },
)

@Composable
internal fun SuccessContent(
    completeSessionAsync: Async<FinancialConnectionsSession>,
    payloadAsync: Async<Payload>,
    onDoneClick: () -> Unit,
) {
    SuccessContentInternal(
        payloadAsync = payloadAsync,
        completeSessionAsync = completeSessionAsync,
        onDoneClick = onDoneClick
    )
}

@Composable
private fun SuccessContentInternal(
    overrideAnimationForPreview: Boolean = false,
    overrideSuccessBodyHeightForPreview: Dp? = null,
    payloadAsync: Async<Payload>,
    completeSessionAsync: Async<FinancialConnectionsSession>,
    onDoneClick: () -> Unit
) {
    var showSpinner by rememberSaveable { mutableStateOf(overrideAnimationForPreview.not()) }
    val payload by remember(payloadAsync) { mutableStateOf(payloadAsync()) }

    payload?.let {
        if (it.skipSuccessPane.not()) {
            LaunchedEffect(true) {
                delay(ENTER_TRANSITION_DURATION_MS.toLong())
                showSpinner = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        SpinnerToSuccessAnimation(
            content = payload?.content,
            title = payload?.title,
            showSpinner = showSpinner || payload == null,
            initialSuccessBodyHeight = overrideSuccessBodyHeightForPreview,
        )

        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            AnimatedVisibility(
                visible = !showSpinner,
                enter = FADE_IN_ANIMATION,
            ) {
                SuccessFooter(
                    merchantName = payload?.businessName,
                    loading = completeSessionAsync is Loading,
                    enabled = !showSpinner,
                    onDoneClick = onDoneClick,
                )
            }
        }
    }
}

@Composable
private fun SpinnerToSuccessAnimation(
    showSpinner: Boolean,
    initialSuccessBodyHeight: Dp?,
    content: TextResource?,
    title: TextResource?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current

    val scope = rememberCoroutineScope()
    var didPerformHaptics = rememberSaveable { false }

    var targetCheckmarkScale by rememberSaveable {
        mutableFloatStateOf(if (showSpinner) 0f else 1f)
    }

    var successBodyHeight by remember { mutableStateOf(initialSuccessBodyHeight ?: 0.dp) }

    if (!showSpinner && !didPerformHaptics) {
        LaunchedEffect(Unit) {
            hapticFeedback.performHapticFeedback(LongPress)
            didPerformHaptics = true
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val finalOffset = remember(successBodyHeight) {
            density.calculateFinalSpinnerOffset(successBodyHeight)
        }

        val spinnerPosition by animateIntOffsetAsState(
            targetValue = if (showSpinner) {
                IntOffset.Zero
            } else {
                finalOffset
            },
            label = "SpinnerPositionOffset",
            finishedListener = {
                scope.launch {
                    targetCheckmarkScale = 1f
                }
            },
        )

        Crossfade(
            targetState = showSpinner,
            label = "SpinnerToCheckmarkCrossfade",
            modifier = Modifier
                .size(ICON_SIZE.dp)
                .offset { spinnerPosition },
        ) { shouldShowSpinner ->
            SpinnerToCheckmark(
                showSpinner = shouldShowSpinner,
                targetCheckmarkScale = { targetCheckmarkScale },
            )
        }

        AnimatedVisibility(
            visible = !showSpinner,
            enter = SUCCESS_SLIDE_IN_ANIMATION,
        ) {
            SuccessBody(
                content = content,
                title = title,
                modifier = Modifier.onGloballyPositioned {
                    successBodyHeight = with(density) { it.size.height.toDp() }
                },
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SuccessFooter(
    modifier: Modifier = Modifier,
    loading: Boolean,
    enabled: Boolean,
    merchantName: String?,
    onDoneClick: () -> Unit
) {
    FinancialConnectionsButton(
        loading = loading,
        enabled = enabled,
        onClick = onDoneClick,
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("done_button")
            .fillMaxWidth()
    ) {
        MiddleEllipsisText(
            text = when (merchantName) {
                null -> stringResource(id = R.string.stripe_success_pane_done)
                else -> stringResource(id = R.string.stripe_success_pane_done_with_merchant, merchantName)
            }
        )
    }
}

@Composable
private fun SpinnerToCheckmark(
    showSpinner: Boolean,
    targetCheckmarkScale: () -> Float,
    modifier: Modifier = Modifier,
) {
    val checkmarkScale by animateFloatAsState(
        targetValue = targetCheckmarkScale(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "CheckmarkScale",
    )

    if (showSpinner) {
        LoadingSpinner(
            modifier = modifier.fillMaxSize(),
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(ICON_SIZE.dp)
                .background(FinancialConnectionsTheme.colors.buttonPrimary, CircleShape)
        ) {
            Icon(
                modifier = Modifier.graphicsLayer {
                    scaleX = checkmarkScale
                    scaleY = checkmarkScale
                },
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(id = R.string.stripe_success_pane_title),
                tint = FinancialConnectionsTheme.colors.contentOnBrand,
            )
        }
    }
}

@Composable
private fun SuccessBody(
    content: TextResource?,
    title: TextResource?,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        title?.let {
            AnnotatedText(
                text = it,
                defaultStyle = typography.headingXLarge.copy(
                    textAlign = TextAlign.Center
                ),
                onClickableTextClick = {}
            )
        }

        content?.let {
            AnnotatedText(
                text = content,
                defaultStyle = typography.bodyMedium.copy(
                    textAlign = TextAlign.Center
                ),
                annotationStyles = mapOf(
                    StringAnnotation.BOLD to typography.bodyMediumEmphasized.copy(
                        textAlign = TextAlign.Center,
                    ).toSpanStyle()
                ),
                onClickableTextClick = {}
            )
        }
    }
}

@Preview(
    group = "Success",
    name = "Loading"
)
@Composable
internal fun SuccessScreenPreview(
    @PreviewParameter(SuccessPreviewParameterProvider::class) state: SuccessState
) {
    FinancialConnectionsPreview {
        SuccessContentInternal(
            overrideAnimationForPreview = false,
            completeSessionAsync = state.completeSession,
            payloadAsync = state.payload,
            onDoneClick = {},
        )
    }
}

@Preview(
    group = "Success",
    name = "Animation completed"
)
@Composable
internal fun SuccessScreenAnimationCompletedPreview(
    @PreviewParameter(SuccessPreviewParameterProvider::class) state: SuccessState
) {
    FinancialConnectionsPreview {
        val configuration = LocalConfiguration.current
        val successBodyHeight = calculateBodyHeightForPreview(configuration)

        SuccessContentInternal(
            overrideAnimationForPreview = true,
            overrideSuccessBodyHeightForPreview = successBodyHeight,
            completeSessionAsync = state.completeSession,
            payloadAsync = state.payload,
            onDoneClick = {},
        )
    }
}

private fun calculateBodyHeightForPreview(config: Configuration): Dp {
    // We need to manually calculate this for our screenshot tests, as we've been unable to
    // delay the capture until the offset animation finishes.
    val isPhone = config.orientation == Configuration.ORIENTATION_PORTRAIT
    return if (isPhone) {
        120.dp
    } else {
        72.dp
    }
}

private fun Density.calculateFinalSpinnerOffset(
    successBodyHeight: Dp,
): IntOffset {
    val offsetInDp = DpOffset(
        x = 0.dp,
        y = (SUCCESS_BODY_OFFSET.dp + successBodyHeight) / 2 * (-1),
    )
    return IntOffset(
        x = offsetInDp.x.roundToPx(),
        y = offsetInDp.y.roundToPx(),
    )
}
