@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stripe.android.checkout.CheckoutSession
import com.stripe.android.checkout.PaymentOptionDisplayData
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme
import com.stripe.android.uicore.format.CurrencyFormatter
import kotlinx.coroutines.launch

internal class CheckoutControllerExampleCopyActivity : AppCompatActivity() {

    private val viewModel: CheckoutControllerExampleCopyViewModel by viewModels {
        CheckoutControllerExampleCopyViewModel.factory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val presenter = viewModel.controller.createPresenter(this)
        val paymentElement = presenter.paymentElement()

        lifecycleScope.launch {
            viewModel.sessionComplete.collect {
                Toast.makeText(this@CheckoutControllerExampleCopyActivity, "Payment complete!", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        setContent {
            val status by viewModel.status.collectAsState()
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route ?: CheckoutControllerExampleCopyScreen.Summary.route

            PlaygroundTheme(
                content = {
                    NavHost(
                        navController = navController,
                        startDestination = CheckoutControllerExampleCopyScreen.Summary.route,
                    ) {
                        composable(CheckoutControllerExampleCopyScreen.Summary.route) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                when (val currentStatus = status) {
                                    is CheckoutControllerExampleCopyViewModel.Status.Loading -> CopyLoadingContent()
                                    is CheckoutControllerExampleCopyViewModel.Status.Error -> {
                                        CopyErrorContent(currentStatus.message)
                                    }
                                    is CheckoutControllerExampleCopyViewModel.Status.Configured -> {
                                        currentStatus.checkoutSession?.let { session ->
                                            LineItemsSection(session)
                                            TotalSummarySection(session)
                                            ExpressCheckoutExamplePicker(
                                                selectedExample = currentStatus.expressCheckoutExample,
                                                onSelected = viewModel::selectExpressCheckoutExample,
                                            )
                                            CopyExpressCheckoutSection(
                                                session = session,
                                                content = { presenter.expressCheckoutElement().Content() },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        composable(CheckoutControllerExampleCopyScreen.Payment.route) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                when (val currentStatus = status) {
                                    is CheckoutControllerExampleCopyViewModel.Status.Loading -> CopyLoadingContent()
                                    is CheckoutControllerExampleCopyViewModel.Status.Error -> {
                                        CopyErrorContent(currentStatus.message)
                                    }
                                    is CheckoutControllerExampleCopyViewModel.Status.Configured -> {
                                        currentStatus.checkoutSession?.let { session ->
                                            CopyExpressCheckoutSection(
                                                session = session,
                                                content = { presenter.expressCheckoutElement().Content() },
                                            )
                                            paymentElement.PaymentOptionsContent()
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBarContent = {
                    val configured = status as? CheckoutControllerExampleCopyViewModel.Status.Configured
                    when (currentRoute) {
                        CheckoutControllerExampleCopyScreen.Summary.route -> Button(
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@CheckoutControllerExampleCopyActivity,
                                        CheckoutControllerExampleCopyPaymentActivity::class.java,
                                    )
                                )
                            },
                            enabled = configured != null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Continue to payment")
                        }
                        CheckoutControllerExampleCopyScreen.Payment.route -> {
                            CopyPaymentOptionRow(configured?.checkoutSession?.paymentOptionDisplayData)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { paymentElement.presentPaymentOptions() },
                                enabled = configured != null,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Select Payment Method")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { presenter.confirm() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Confirm")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Back to order summary")
                            }
                        }
                        else -> Unit
                    }
                },
            )
        }
    }
}

private enum class CheckoutControllerExampleCopyScreen(val route: String) {
    Summary("summary"),
    Payment("payment"),
}

@Composable
private fun ExpressCheckoutExamplePicker(
    selectedExample: CheckoutControllerExampleCopyViewModel.ExpressCheckoutExample,
    onSelected: (CheckoutControllerExampleCopyViewModel.ExpressCheckoutExample) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Express Checkout examples", style = MaterialTheme.typography.h6)
        CheckoutControllerExampleCopyViewModel.ExpressCheckoutExample.entries.forEach { example ->
            Button(
                onClick = { onSelected(example) },
                enabled = example != selectedExample,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(example.label)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
internal fun CopyExpressCheckoutSection(
    session: CheckoutSession,
    content: @Composable () -> Unit,
) {
    val availableMethods = session.availableExpressCheckoutPaymentMethods

    AnimatedVisibility(
        visible = availableMethods.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Express checkout: ${availableMethods.joinToString()}",
                style = MaterialTheme.typography.body2,
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun CopyLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun CopyErrorContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.h6,
            color = Color.Red,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message)
    }
}

@Composable
internal fun CopyPaymentOptionRow(paymentOption: PaymentOptionDisplayData?) {
    if (paymentOption != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = paymentOption.iconPainter,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = paymentOption.label,
                style = MaterialTheme.typography.body1,
            )
        }
    } else {
        Text(
            text = "No payment method selected",
            style = MaterialTheme.typography.body2,
            color = Color.Gray,
        )
    }
}

@Composable
private fun LineItemsSection(session: CheckoutSession) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Line Items", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        for (item in session.lineItems) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${item.name} x${item.quantity}",
                    style = MaterialTheme.typography.body2,
                )
                Text(
                    text = formatAmount(item.total, session.currency),
                    style = MaterialTheme.typography.body2,
                )
            }
        }
    }
}

@Composable
private fun TotalSummarySection(session: CheckoutSession) {
    val summary = session.totalSummary ?: return

    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(modifier = Modifier.padding(vertical = 12.dp))

        SummaryRow(label = "Subtotal", amount = formatAmount(summary.subtotal, session.currency))

        for (discount in summary.discountAmounts) {
            SummaryRow(
                label = discount.displayName,
                amount = "-${formatAmount(discount.amount, session.currency)}",
            )
        }

        summary.shippingRate?.let { shipping ->
            val amountText = if (shipping.amount == 0L) "Free" else formatAmount(shipping.amount, session.currency)
            SummaryRow(label = "Shipping", amount = amountText)
        }

        for (tax in summary.taxAmounts) {
            val label = if (tax.inclusive) "${tax.displayName} (included)" else tax.displayName
            SummaryRow(label = label, amount = formatAmount(tax.amount, session.currency))
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Total", style = MaterialTheme.typography.subtitle1)
            Text(
                text = formatAmount(summary.totalDueToday, session.currency),
                style = MaterialTheme.typography.subtitle1,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.body2)
        Text(text = amount, style = MaterialTheme.typography.body2)
    }
}

private fun formatAmount(amount: Long, currency: String): String {
    return CurrencyFormatter.format(amount, currency)
}
