package com.stripe.android.financialconnections.features.success

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.V3LoadingSpinner
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import kotlinx.coroutines.delay

private const val ENTER_TRANSITION_DURATION_MS = 1000
private const val CHECK_ALPHA_DURATION_MS = 250
private const val SLIDE_IN_ANIMATION_FRACTION = 4

@Composable
internal fun SuccessContent(
    completeSessionAsync: Async<FinancialConnectionsSession>,
    payload: SuccessState.Payload,
    onDoneClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    SuccessContentInternal(
        // Just enabled on Compose Previews: allows to preview the post-animation state.
        overrideAnimationForPreview = false,
        payload = payload,
        onCloseClick = onCloseClick,
        completeSessionAsync = completeSessionAsync,
        onDoneClick = onDoneClick
    )
}

@Composable
private fun SuccessContentInternal(
    overrideAnimationForPreview: Boolean,
    payload: SuccessState.Payload,
    onCloseClick: () -> Unit,
    completeSessionAsync: Async<FinancialConnectionsSession>,
    onDoneClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showSpinner by remember { mutableStateOf(overrideAnimationForPreview.not()) }

    if (payload.skipSuccessPane.not()) {
        LaunchedEffect(true) {
            delay(ENTER_TRANSITION_DURATION_MS.toLong())
            showSpinner = false
        }
    }

    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = false,
                onCloseClick = onCloseClick,
                elevation = scrollState.elevation
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SpinnerToSuccessAnimation(
                    customSuccessMessage = payload.customSuccessMessage,
                    accountsCount = payload.accountsCount,
                    showSpinner = showSpinner
                )
            }
            SuccessFooter(
                modifier = Modifier.alpha(if (showSpinner) 0f else 1f),
                merchantName = payload.businessName,
                loading = completeSessionAsync is Loading,
                onDoneClick = onDoneClick
            )
        }
    }
}

@Composable
private fun SpinnerToSuccessAnimation(
    showSpinner: Boolean,
    accountsCount: Int,
    customSuccessMessage: TextResource?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Define the animation specs
        val enterTransition = fadeIn(animationSpec = tween(ENTER_TRANSITION_DURATION_MS))
        val exitTransition = fadeOut(animationSpec = tween(ENTER_TRANSITION_DURATION_MS))

        // Delay the appearance of the check icon
        val checkAlpha: Float by animateFloatAsState(
            targetValue = if (showSpinner) 0f else 1f,
            animationSpec = tween(
                delayMillis = CHECK_ALPHA_DURATION_MS,
                durationMillis = CHECK_ALPHA_DURATION_MS,
                easing = LinearEasing,
            ),
            label = "check_icon_alpha"
        )

        // Fade out loading spinner
        AnimatedVisibility(
            visible = showSpinner,
            enter = enterTransition,
            exit = exitTransition,
        ) {
            V3LoadingSpinner(
                modifier = Modifier.size(56.dp)
            )
        }

        // Fade in + slide success content.
        AnimatedVisibility(
            visible = !showSpinner,
            enter = enterTransition + slideInVertically(initialOffsetY = { it / SLIDE_IN_ANIMATION_FRACTION }),
            exit = exitTransition
        ) {
            SuccessCompletedContent(
                checkAlpha = checkAlpha,
                customSuccessMessage = customSuccessMessage,
                accountsCount = accountsCount
            )
        }
    }
}

@Composable
private fun SuccessCompletedContent(
    checkAlpha: Float,
    customSuccessMessage: TextResource?,
    accountsCount: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(FinancialConnectionsTheme.v3Colors.iconBrand, CircleShape)
        ) {
            Icon(
                modifier = Modifier.graphicsLayer { alpha = checkAlpha },
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(id = R.string.stripe_success_pane_title),
                tint = Color.White
            )
        }
        Text(
            stringResource(id = R.string.stripe_success_pane_title),
            style = FinancialConnectionsTheme.v3Typography.headingXLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = customSuccessMessage
                ?.toText()?.toString()
                ?: pluralStringResource(
                    id = R.plurals.stripe_success_pane_desc,
                    count = accountsCount
                ),
            style = FinancialConnectionsTheme.v3Typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessFooter(
    modifier: Modifier = Modifier,
    loading: Boolean,
    merchantName: String?,
    onDoneClick: () -> Unit
) {
    Box(modifier) {
        FinancialConnectionsButton(
            loading = loading,
            onClick = onDoneClick,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = when (merchantName) {
                    null -> stringResource(id = R.string.stripe_success_pane_done)
                    else -> stringResource(id = R.string.stripe_success_pane_done_with_merchant, merchantName)
                }
            )
        }
    }
}

@Preview(
    group = "Success",
    name = "Loading"
)
@Suppress("LongMethod")
@Composable
internal fun SuccessScreenPreview(
    @PreviewParameter(SuccessPreviewParameterProvider::class) state: SuccessState
) {
    FinancialConnectionsPreview {
        SuccessContentInternal(
            overrideAnimationForPreview = false,
            completeSessionAsync = state.completeSession,
            payload = state.payload()!!,
            onDoneClick = {},
            onCloseClick = {}
        )
    }
}

@Preview(
    group = "Success",
    name = "Animation completed"
)
@Suppress("LongMethod")
@Composable
internal fun SuccessScreenAnimationCompletedPreview(
    @PreviewParameter(SuccessPreviewParameterProvider::class) state: SuccessState
) {
    FinancialConnectionsPreview {
        SuccessContentInternal(
            overrideAnimationForPreview = true,
            completeSessionAsync = state.completeSession,
            payload = state.payload()!!,
            onDoneClick = {},
            onCloseClick = {}
        )
    }
}
