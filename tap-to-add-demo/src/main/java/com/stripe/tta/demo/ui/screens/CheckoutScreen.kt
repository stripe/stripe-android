package com.stripe.tta.demo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.tta.demo.R
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.tta.demo.CheckoutLoadState
import com.stripe.tta.demo.CheckoutViewModel
import com.stripe.tta.demo.IntegrationMode
import com.stripe.tta.demo.catalog.CartLine
import com.stripe.tta.demo.catalog.MockCatalog

private const val SkeletonProductRowCount = 3
private const val SkeletonShortLineFraction = 0.4f

@Composable
internal fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    paymentSheet: PaymentSheet,
    flowController: PaymentSheet.FlowController,
    embeddedPaymentElement: EmbeddedPaymentElement,
    onNavigateToCatalog: () -> Unit,
) {
    val loadState by viewModel.loadState.collectAsState()
    val integrationMode by viewModel.integrationMode.collectAsState()
    val cartQuantities by viewModel.cartQuantities.collectAsState()
    val cartSubtotalCents = remember(cartQuantities) {
        MockCatalog.subtotalCents(cartQuantities)
    }
    val cartLines = remember(cartQuantities) {
        MockCatalog.cartLines(cartQuantities)
    }

    LaunchedEffect(cartSubtotalCents) {
        if (cartSubtotalCents > 0) {
            viewModel.loadCheckout()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (cartSubtotalCents <= 0) {
            EmptyCartBody(onBrowseCatalog = onNavigateToCatalog)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 20.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    CheckoutHeader(onNavigateToCatalog = onNavigateToCatalog)
                }
                Spacer(Modifier.size(16.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    IntegrationModeSelector(
                        selected = integrationMode,
                        onSelect = viewModel::setIntegrationMode,
                    )
                    when (val state = loadState) {
                        CheckoutLoadState.Loading -> CheckoutSkeleton()
                        is CheckoutLoadState.Failed -> CheckoutLoadError(state) {
                            viewModel.restart()
                        }
                        is CheckoutLoadState.Ready -> CheckoutReadyBody(
                            viewModel = viewModel,
                            paymentSheet = paymentSheet,
                            flowController = flowController,
                            embeddedPaymentElement = embeddedPaymentElement,
                            checkout = state,
                            integrationMode = integrationMode,
                            cartLines = cartLines,
                            cartSubtotalCents = cartSubtotalCents,
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Clear customer information")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCartBody(
    onBrowseCatalog: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Your cart is empty",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Add items in the shop before checking out.",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
        )
        Button(
            onClick = onBrowseCatalog,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Back to shop")
        }
    }
}

@Composable
private fun CheckoutHeader(
    onNavigateToCatalog: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            onClick = onNavigateToCatalog,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back_24),
                contentDescription = "Back",
                tint = MaterialTheme.colors.onSurface,
            )
        }
        Text(
            text = "Checkout",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun IntegrationModeSelector(
    selected: IntegrationMode,
    onSelect: (IntegrationMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "SDK integration",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Medium,
        )
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IntegrationMode.entries.forEach { mode ->
                val label = when (mode) {
                    IntegrationMode.PaymentSheet -> "PaymentSheet"
                    IntegrationMode.FlowController -> "FlowController"
                    IntegrationMode.Embedded -> "Embedded"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected == mode,
                            onClick = { onSelect(mode) },
                            role = Role.RadioButton,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected == mode,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colors.primary,
                        ),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.body2,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutLoadError(
    state: CheckoutLoadState.Failed,
    onTryAgain: () -> Unit,
) {
    Text(
        text = "Could not start checkout",
        style = MaterialTheme.typography.subtitle1,
        fontWeight = FontWeight.Medium,
    )
    Text(
        text = state.message,
        style = MaterialTheme.typography.body1,
    )
    Button(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) {
        Text("Try again")
    }
}

@Composable
private fun CheckoutReadyBody(
    viewModel: CheckoutViewModel,
    paymentSheet: PaymentSheet,
    flowController: PaymentSheet.FlowController,
    embeddedPaymentElement: EmbeddedPaymentElement,
    checkout: CheckoutLoadState.Ready,
    integrationMode: IntegrationMode,
    cartLines: List<CartLine>,
    cartSubtotalCents: Long,
) {
    val flowControllerReady by viewModel.flowControllerReady.collectAsState()
    val flowControllerPaymentOption by viewModel.flowControllerPaymentOption.collectAsState()
    val flowControllerConfirmInProgress by viewModel.flowControllerConfirmInProgress.collectAsState()
    val embeddedReady by viewModel.embeddedReady.collectAsState()
    val embeddedConfirmInProgress by viewModel.embeddedConfirmInProgress.collectAsState()
    val paymentError by viewModel.paymentErrorMessage.collectAsState()
    var configureRetryKey by remember { mutableStateOf(0) }
    var embeddedConfigureKey by remember { mutableStateOf(0) }

    LaunchedEffect(checkout.checkout, integrationMode, configureRetryKey) {
        if (integrationMode != IntegrationMode.FlowController) return@LaunchedEffect
        flowController.configureWithIntentConfiguration(
            intentConfiguration = viewModel.intentConfiguration(checkout.checkout),
            configuration = viewModel.paymentConfiguration(checkout.checkout),
        ) { success, error ->
            viewModel.onFlowControllerConfigureResult(success, error)
        }
    }

    LaunchedEffect(checkout.checkout, integrationMode, embeddedConfigureKey) {
        if (integrationMode != IntegrationMode.Embedded) return@LaunchedEffect
        viewModel.onEmbeddedConfigurationStarted()
        val result = embeddedPaymentElement.configure(
            intentConfiguration = viewModel.intentConfiguration(checkout.checkout),
            configuration = viewModel.embeddedConfiguration(checkout.checkout),
        )
        viewModel.onEmbeddedConfigureResult(result)
    }

    CartLineList(lines = cartLines)
    Divider(modifier = Modifier.padding(vertical = 8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Subtotal",
            style = MaterialTheme.typography.subtitle1,
        )
        Text(
            text = MockCatalog.formatUsd(cartSubtotalCents),
            style = MaterialTheme.typography.h6,
        )
    }
    when (integrationMode) {
        IntegrationMode.PaymentSheet -> {
            if (paymentError != null) {
                PaymentFailedInlineBanner(
                    message = paymentError.orEmpty(),
                    onDismiss = { viewModel.clearPaymentError() },
                )
            }
            Button(
                onClick = {
                    paymentSheet.presentWithIntentConfiguration(
                        intentConfiguration = viewModel.intentConfiguration(checkout.checkout),
                        configuration = viewModel.paymentConfiguration(checkout.checkout),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pay")
            }
        }

        IntegrationMode.FlowController -> {
            if (paymentError != null && !flowControllerReady) {
                FlowControllerConfigureErrorBanner(
                    message = paymentError.orEmpty(),
                    onRetry = {
                        viewModel.clearPaymentError()
                        configureRetryKey = configureRetryKey + 1
                    },
                )
            } else if (!flowControllerReady) {
                Text(
                    text = "Preparing payment…",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                )
            }
            if (paymentError != null && flowControllerReady) {
                PaymentFailedInlineBanner(
                    message = paymentError.orEmpty(),
                    onDismiss = { viewModel.clearPaymentError() },
                )
            }
            FlowControllerPaymentOptionSummary(
                paymentOption = flowControllerPaymentOption,
                flowControllerReady = flowControllerReady,
            )
            OutlinedButton(
                onClick = flowController::presentPaymentOptions,
                enabled = flowControllerReady,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Select payment method")
            }
            Button(
                onClick = {
                    viewModel.onFlowControllerConfirmStarted()
                    flowController.confirm()
                },
                enabled = flowControllerReady &&
                    flowControllerPaymentOption != null &&
                    !flowControllerConfirmInProgress,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (flowControllerConfirmInProgress) {
                    LoadingButtonContent()
                } else {
                    Text("Pay")
                }
            }
        }

        IntegrationMode.Embedded -> {
            if (paymentError != null && !embeddedReady) {
                FlowControllerConfigureErrorBanner(
                    message = paymentError.orEmpty(),
                    onRetry = {
                        viewModel.clearPaymentError()
                        embeddedConfigureKey = embeddedConfigureKey + 1
                    },
                )
            } else if (!embeddedReady) {
                Text(
                    text = "Preparing payment…",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                )
            }
            if (paymentError != null && embeddedReady) {
                PaymentFailedInlineBanner(
                    message = paymentError.orEmpty(),
                    onDismiss = { viewModel.clearPaymentError() },
                )
            }
            if (embeddedReady) {
                embeddedPaymentElement.Content()
            }
            Button(
                onClick = {
                    viewModel.onEmbeddedConfirmStarted()
                    embeddedPaymentElement.confirm()
                },
                enabled = embeddedReady && !embeddedConfirmInProgress,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (embeddedConfirmInProgress) {
                    LoadingButtonContent()
                } else {
                    Text("Pay")
                }
            }
        }
    }
}

@Composable
private fun LoadingButtonContent() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(16.dp)
                .clip(MaterialTheme.shapes.small),
            color = MaterialTheme.colors.onPrimary,
            strokeWidth = 2.dp,
        )
        Text("Processing…")
    }
}

@Composable
private fun PaymentFailedInlineBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colors.onSurface,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.error,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.body2,
            )
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun FlowControllerConfigureErrorBanner(
    message: String,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colors.onSurface,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Could not set up payment",
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.error,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.body2,
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun FlowControllerPaymentOptionSummary(
    paymentOption: PaymentOption?,
    flowControllerReady: Boolean,
) {
    if (flowControllerReady) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (paymentOption != null) {
                    Image(
                        painter = paymentOption.iconPainter,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Payment method",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                        )
                        Text(
                            text = paymentOption.label,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    Text(
                        text = "No payment method selected",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CartLineList(lines: List<CartLine>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        lines.forEach { line ->
            Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = line.product.name,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = line.product.description,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${line.quantity} × ${MockCatalog.formatUsd(line.product.unitPriceCents)}",
                            style = MaterialTheme.typography.body2,
                        )
                        Text(
                            text = MockCatalog.formatUsd(line.lineTotalCents),
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(SkeletonProductRowCount) {
            SkeletonLine(height = 96.dp)
        }
        SkeletonLine(height = 24.dp, fraction = SkeletonShortLineFraction)
    }
}

@Composable
private fun SkeletonLine(height: Dp, fraction: Float = 1f) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(height),
        color = Color.LightGray.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.medium,
    ) { }
}
