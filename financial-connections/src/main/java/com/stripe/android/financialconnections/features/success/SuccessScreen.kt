package com.stripe.android.financialconnections.features.success

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.V3LoadingSpinner
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import kotlinx.coroutines.delay

@Composable
internal fun SuccessScreen() {
    val viewModel: SuccessViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    state.value.payload()?.let { payload ->
        SuccessContent(
            loading = state.value.completeSession is Loading,
            skipSuccessPane = payload.skipSuccessPane,
            merchantName = payload.businessName,
            onDoneClick = viewModel::onDoneClick,
        ) { parentViewModel.onCloseNoConfirmationClick(Pane.SUCCESS) }
    }
}

@Composable
private fun SuccessContent(
    loading: Boolean,
    skipSuccessPane: Boolean,
    merchantName: String?,
    onDoneClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = false,
                onCloseClick = onCloseClick,
                elevation = scrollState.elevation
            )
        }
    ) {
        SuccessLoaded(
            skipSuccessPane = skipSuccessPane,
            loading = loading,
            merchantName = merchantName,
            onDoneClick = onDoneClick
        )
    }
}

@Composable
fun SuccessLoaded(
    loading: Boolean,
    skipSuccessPane: Boolean,
    merchantName: String?,
    onDoneClick: () -> Unit
) {
    var showSpinner by remember { mutableStateOf(true) }

    if (skipSuccessPane.not()) {
        // Show the spinner for a bit before hiding it (or keep it showing if skipping success pane)
        LaunchedEffect(true) {
            delay(1500)
            showSpinner = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SuccessItem(
                showSpinner = showSpinner
            )
        }
        SuccessLoadedFooter(
            modifier = Modifier.alpha(if (showSpinner) 0f else 1f),
            merchantName = merchantName,
            loading = loading,
            onDoneClick = onDoneClick
        )
    }
}

@Composable
fun SuccessItem(
    showSpinner: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Animate the entire column fading in
        if (showSpinner) {
            V3LoadingSpinner(
                modifier = Modifier.size(56.dp)
            )
        }
        AnimatedVisibility(
            visible = !showSpinner,
            enter = fadeIn(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val offsetY by animateDpAsState(
                    targetValue = if (!showSpinner) 0.dp else 56.dp,
                    animationSpec = tween(
                        durationMillis = 5000,
                        easing = LinearOutSlowInEasing
                    ), label = "bottom_up_animation"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .offset(y = offsetY) // apply the animated offset here
                        .background(v3Colors.iconBrand, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color.White // Replace with your desired icon color
                    )
                }
                Text(
                    "Success",
                    style = v3Typography.headingXLarge
                )
                Text(
                    "Your account is connected.",
                    style = v3Typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SuccessLoadedFooter(
    modifier: Modifier = Modifier,
    loading: Boolean,
    merchantName: String?,
    onDoneClick: () -> Unit
) {
    Box(
        modifier.padding(
            bottom = 24.dp,
            start = 24.dp,
            end = 24.dp
        )
    ) {
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
    name = "Default"
)
@Suppress("LongMethod")
@Composable
internal fun SuccessScreenPreview() {
    FinancialConnectionsPreview {
        SuccessContent(
            loading = false,
            skipSuccessPane = false,
            merchantName = "Test Merchant",
            onDoneClick = {},
        ) {}
    }
}

