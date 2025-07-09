package com.stripe.android.paymentsheet.example.playground

import android.util.Patterns
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkController
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.ui.core.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun LinkControllerUi(
    viewModel: PaymentSheetPlaygroundViewModel,
    linkController: LinkController,
    playgroundState: PlaygroundState.Payment,
) {
    LaunchedEffect(playgroundState) {
        val configuration = playgroundState.paymentSheetConfiguration(viewModel.settings)
        linkController.setConfiguration(configuration)
    }

    val linkControllerState by viewModel.linkControllerState.collectAsState()

    LinkControllerUi(
        linkControllerState = linkControllerState,
        onEmailChange = { email ->
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                linkController.lookupConsumer(email)
            }
        },
        onPaymentMethodButtonClick = { email ->
            linkController.presentPaymentMethods(email = email.takeIf { it.isNotBlank() })
        },
        onCreatePaymentMethodClick = linkController::createPaymentMethod,
    )
}

@Composable
internal fun LinkControllerUi(
    linkControllerState: LinkControllerState,
    onEmailChange: (email: String) -> Unit,
    onPaymentMethodButtonClick: (email: String) -> Unit,
    onCreatePaymentMethodClick: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    val selectedPaymentMethodStateError = linkControllerState.selectedPaymentMethodState?.error
    val lookupConsumerError =
        (linkControllerState.lookupConsumerResult as? LinkController.LookupConsumerResult.Failed)
            ?.error
    val createPaymentMethodError =
        (linkControllerState.createPaymentMethodResult as? LinkController.CreatePaymentMethodResult.Failed)
            ?.error
    val errorToPresent = selectedPaymentMethodStateError ?: lookupConsumerError ?: createPaymentMethodError

    val scope = rememberCoroutineScope()
    DisposableEffect(email) {
        val job = scope.launch {
            delay(1_000L)
            onEmailChange(email)
        }
        onDispose { job.cancel() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        errorToPresent?.let { error ->
            Text(
                text = error.message ?: "An error occurred",
                color = MaterialTheme.colors.error,
            )
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = email,
            label = { Text(text = "Customer email (optional)") },
            onValueChange = { email = it }
        )
        Divider(Modifier.padding(vertical = 20.dp))

        when (linkControllerState.lookupConsumerResult) {
            is LinkController.LookupConsumerResult.Success -> {
                val exists = if (linkControllerState.lookupConsumerResult.isConsumer) "exists" else "does not exist"
                Text(
                    text = "${linkControllerState.lookupConsumerResult.email} $exists",
                    style = MaterialTheme.typography.body1,
                )
            }
            is LinkController.LookupConsumerResult.Failed, null -> {
                // No-op.
            }
        }

        PaymentMethodButton(
            preview = linkControllerState.paymentMethodPreview,
            onClick = { onPaymentMethodButtonClick(email) },
        )
        Spacer(Modifier.height(16.dp))
        ConfirmButton(
            onClick = onCreatePaymentMethodClick,
            enabled = linkControllerState.paymentMethodPreview != null,
        )

        val createPaymentMethodResultText = when (linkControllerState.createPaymentMethodResult) {
            is LinkController.CreatePaymentMethodResult.Success ->
                linkControllerState.createPaymentMethodResult.paymentMethod.id ?: "Payment method created (no id)"
            is LinkController.CreatePaymentMethodResult.Failed ->
                "Failed"
            null ->
                ""
        }
        Text(
            text = createPaymentMethodResultText,
            style = MaterialTheme.typography.body1,
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun LinkControllerUiPreview() {
    PaymentSheetExampleTheme {
        LinkControllerUi(
            linkControllerState = LinkControllerState(),
            onEmailChange = {},
            onPaymentMethodButtonClick = {},
            onCreatePaymentMethodClick = {}
        )
    }
}

@Composable
private fun ConfirmButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick, enabled = enabled)
            .background(color = Color.Black)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth(),
        style = MaterialTheme.typography.h6,
        color = Color.White,
        textAlign = TextAlign.Center,
        text = "Confirm",
    )
}

@Composable
private fun PaymentMethodButton(
    preview: LinkController.PaymentMethodPreview?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(color = Color.Black.copy(alpha = 0.1f))
            .heightIn(min = 80.dp)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconSize = 24.dp
        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = preview,
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { preview ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (preview != null) {
                    Image(
                        modifier = Modifier.size(iconSize),
                        painter = painterResource(preview.iconRes),
                        contentDescription = null,
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f)
                    ) {
                        Text(
                            modifier = Modifier,
                            text = preview.label,
                            style = MaterialTheme.typography.h6,
                        )
                        preview.sublabel?.let { sublabel ->
                            Text(
                                modifier = Modifier.padding(top = 2.dp),
                                text = sublabel,
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(
                                    alpha = 0.6f
                                ),
                            )
                        }
                    }
                } else {
                    Icon(
                        modifier = Modifier.size(iconSize),
                        painter = painterResource(R.drawable.stripe_ic_paymentsheet_pm_card),
                        contentDescription = null,
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f)
                    ) {
                        Text(
                            modifier = Modifier,
                            text = "Choose payment method",
                            style = MaterialTheme.typography.h6,
                        )
                    }
                }
            }
        }
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(com.stripe.android.paymentsheet.R.drawable.stripe_ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentMethodButtonPreview() {
    PaymentSheetExampleTheme {
        Column {
            PaymentMethodButton(
                modifier = Modifier.padding(16.dp),
                preview = null,
                onClick = {},
            )
            PaymentMethodButton(
                modifier = Modifier.padding(16.dp),
                preview = LinkController.PaymentMethodPreview(
                    label = "Link",
                    sublabel = "Visa (Personal) •••• 4242",
                    iconRes = com.stripe.android.paymentsheet.R.drawable.stripe_ic_paymentsheet_link_arrow,
                ),
                onClick = {},
            )
        }
    }
}
