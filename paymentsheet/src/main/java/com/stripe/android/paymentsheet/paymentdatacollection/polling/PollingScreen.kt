package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.PaymentsTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
        modifier = modifier.fillMaxHeight(fraction = 0.5f),
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
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = stringResource(R.string.upi_polling_header),
            style = MaterialTheme.typography.h4,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = rememberActivePollingMessage(remainingDuration),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.upi_polling_cancel))
        }
    }
}

@Composable
private fun FailedPolling(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        IconButton(onClick = onCancel) {
            Icon(
                painter = painterResource(R.drawable.stripe_ic_paymentsheet_back_enabled),
                contentDescription = stringResource(R.string.back),
            )
        }

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
                imageVector = Icons.Default.Warning,
                colorFilter = ColorFilter.tint(Color.Red),
                contentDescription = null,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = stringResource(R.string.upi_polling_payment_failed_title),
                style = MaterialTheme.typography.h4,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = stringResource(R.string.upi_polling_payment_failed_message),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun rememberActivePollingMessage(
    remainingDuration: Duration,
): AnnotatedString {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colors.primary

    return remember(remainingDuration) {
        val remainingTime = remainingDuration.toComponents { minutes, seconds, _ ->
            val paddedSeconds = seconds.toString().padStart(length = 2, padChar = '0')
            "$minutes:$paddedSeconds"
        }

        val message = context.getString(R.string.upi_polling_message, remainingTime)

        buildAnnotatedString {
            append(message.removeSuffix(remainingTime))
            append(AnnotatedString(remainingTime, SpanStyle(primaryColor)))
        }
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
    PaymentsTheme {
        Surface {
            PollingScreen(
                uiState = PollingUiState(
                    durationRemaining = 83.seconds,
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
    PaymentsTheme {
        Surface {
            PollingScreen(
                uiState = PollingUiState(
                    durationRemaining = 83.seconds,
                    pollingState = PollingState.Failed,
                ),
                onCancel = {},
            )
        }
    }
}
