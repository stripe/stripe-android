package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stripe.android.common.ui.LoadingIndicator
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import com.stripe.android.ui.core.R as StripeUiCoreR

private object Spacing {
    val extended = 12.dp
    val normal = 8.dp
    const val lineHeightMultiplier = 1.3f
}

@Composable
internal fun PollingScreen(
    viewModel: PollingViewModel,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = PollingLifecycleObserver(viewModel)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    PollingScreen(
        uiState = uiState,
        onCancel = viewModel::handleCancel,
        modifier = modifier.fillMaxHeight(fraction = 0.67f),
    )
}

@Composable
private fun PollingScreen(
    uiState: PollingUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState.pollingState) {
        PollingState.Active,
        PollingState.Success,
        PollingState.Canceled -> {
            ActivePolling(
                remainingDuration = uiState.durationRemaining,
                onCancel = onCancel,
                modifier = modifier,
                ctaText = uiState.ctaText,
            )
        }
        PollingState.Failed -> {
            FailedPolling(
                onCancel = onCancel,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ActivePolling(
    remainingDuration: Duration,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes ctaText: Int,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(
                vertical = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_top),
                horizontal = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal),
            ),
    ) {
        LoadingIndicator(
            modifier = Modifier.padding(bottom = Spacing.extended),
            color = MaterialTheme.stripeColors.appBarIcon,
        )

        Text(
            text = stringResource(R.string.stripe_upi_polling_header),
            style = MaterialTheme.typography.h4,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Spacing.normal),
        )

        Text(
            text = rememberActivePollingMessage(remainingDuration, ctaText),
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.body1.fontSize * Spacing.lineHeightMultiplier,
            modifier = Modifier.padding(bottom = Spacing.normal),
        )

        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.stripe_upi_polling_cancel))
        }
    }
}

@Composable
private fun FailedPolling(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                elevation = 0.dp,
                backgroundColor = MaterialTheme.colors.surface,
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painter = painterResource(R.drawable.stripe_ic_paymentsheet_back),
                            contentDescription = stringResource(StripeUiCoreR.string.stripe_back),
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_top),
                        horizontal = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal),
                    ),
            ) {
                Image(
                    painter = painterResource(R.drawable.stripe_ic_paymentsheet_polling_failure),
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = Spacing.extended),
                )

                Text(
                    text = stringResource(R.string.stripe_upi_polling_payment_failed_title),
                    style = MaterialTheme.typography.h4,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = Spacing.normal),
                )

                Text(
                    text = stringResource(R.string.stripe_upi_polling_payment_failed_message),
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.body1.fontSize * Spacing.lineHeightMultiplier,
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun rememberActivePollingMessage(
    remainingDuration: Duration,
    @StringRes ctaText: Int
): String {
    val context = LocalContext.current

    return remember(remainingDuration) {
        val remainingTime = remainingDuration.toComponents { minutes, seconds, _ ->
            val paddedSeconds = seconds.toString().padStart(length = 2, padChar = '0')
            "$minutes:$paddedSeconds"
        }
        context.getString(ctaText, remainingTime)
    }
}

private class PollingLifecycleObserver(
    private val viewModel: PollingViewModel,
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        viewModel.resumePolling()
    }

    override fun onStop(owner: LifecycleOwner) {
        viewModel.pausePolling()
        super.onStop(owner)
    }
}

@Preview(heightDp = 400)
@Composable
private fun ActivePollingScreenPreview() {
    StripeTheme {
        Surface {
            PollingScreen(
                uiState = PollingUiState(
                    durationRemaining = 83.seconds,
                    ctaText = R.string.stripe_upi_polling_message,
                    pollingState = PollingState.Active,
                ),
                onCancel = {},
            )
        }
    }
}

@Preview(heightDp = 400)
@Composable
private fun FailedPollingScreenPreview() {
    StripeTheme {
        Surface {
            PollingScreen(
                uiState = PollingUiState(
                    durationRemaining = 83.seconds,
                    ctaText = R.string.stripe_upi_polling_message,
                    pollingState = PollingState.Failed,
                ),
                onCancel = {},
            )
        }
    }
}
