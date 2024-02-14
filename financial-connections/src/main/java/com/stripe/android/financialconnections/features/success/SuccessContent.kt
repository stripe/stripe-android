package com.stripe.android.financialconnections.features.success

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val EnterTransitionDelay = 1.seconds
private val CheckmarkScaleDelay = 250.milliseconds

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

    val footerAlpha by animateFloatAsState(
        targetValue = if (showSpinner) 0f else 1f,
        label = "SuccessFooterAlpha",
    )

    if (payload.skipSuccessPane.not()) {
        LaunchedEffect(true) {
            delay(EnterTransitionDelay)
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
            SpinnerToSuccessAnimation(
                customSuccessMessage = payload.customSuccessMessage,
                accountsCount = payload.accountsCount,
                showSpinner = showSpinner,
                modifier = Modifier.weight(1f),
            )

            SuccessFooter(
                modifier = Modifier.graphicsLayer { alpha = footerAlpha },
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
    customSuccessMessage: TextResource?,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var checkmarkScaleTarget by rememberSaveable { mutableFloatStateOf(0f) }

    val successContentAlpha by animateFloatAsState(
        targetValue = if (showSpinner) 0f else 1f,
        label = "SuccessContentAlpha",
        finishedListener = {
            scope.launch {
                delay(CheckmarkScaleDelay)
                checkmarkScaleTarget = 1f
            }
        },
    )

    val checkmarkScale by animateFloatAsState(
        targetValue = checkmarkScaleTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "CheckmarkSize",
    )

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize(),
    ) {
        SpinnerToCheckmark(
            successContentAlpha = successContentAlpha,
            checkmarkScale = checkmarkScale,
        )

        AnimatedVisibility(visible = !showSpinner) {
            SuccessText(
                accountsCount = accountsCount,
                customSuccessMessage = customSuccessMessage,
                modifier = Modifier.graphicsLayer {
                    alpha = successContentAlpha
                },
            )
        }
    }
}

@Composable
private fun SpinnerToCheckmark(
    successContentAlpha: Float,
    checkmarkScale: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(56.dp)
            .background(
                color = FinancialConnectionsTheme.v3Colors.iconBrand.copy(
                    alpha = successContentAlpha,
                ),
                shape = CircleShape,
            ),
    ) {
        V3LoadingSpinner(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - successContentAlpha }
        )

        Icon(
            modifier = Modifier.scale(checkmarkScale),
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(id = R.string.stripe_success_pane_title),
            tint = Color.White
        )
    }
}

@Composable
private fun SuccessText(
    accountsCount: Int,
    customSuccessMessage: TextResource?,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            stringResource(id = R.string.stripe_success_pane_title),
            style = v3Typography.headingXLarge,
            textAlign = TextAlign.Center
        )

        AnnotatedText(
            text = customSuccessMessage ?: TextResource.PluralId(
                value = R.plurals.stripe_success_pane_desc,
                count = accountsCount,
                args = emptyList()
            ),
            defaultStyle = v3Typography.bodyMedium.copy(
                textAlign = TextAlign.Center
            ),
            annotationStyles = mapOf(
                StringAnnotation.BOLD to v3Typography.bodyMediumEmphasized.copy(
                    textAlign = TextAlign.Center,
                ).toSpanStyle()
            ),
            onClickableTextClick = {}
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
