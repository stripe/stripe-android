@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Button
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme
import kotlinx.serialization.Serializable

@Serializable
private data object CartRoute

@Serializable
private data object CheckoutRoute

internal class CheckoutTwoStepExampleActivity : AppCompatActivity() {
    private val viewModel: CheckoutTwoStepExampleViewModel by viewModels {
        CheckoutTwoStepExampleViewModel.factory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = CartRoute) {
                composable<CartRoute> {
                    CartScreen(
                        onCheckout = { navController.navigate(CheckoutRoute) },
                    )
                }
                composable<CheckoutRoute> {
                    CheckoutScreen(
                        viewModel = viewModel,
                        onBack = {
                            viewModel.cancelCheckout()
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CartScreen(onCheckout: () -> Unit) {
    PlaygroundTheme(
        topBarContent = { ExampleTopAppBar(title = "Cart", onBack = null) },
        content = {
            Text(text = "Your cart", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(8.dp))
            CheckoutTwoStepExampleViewModel.CART_ITEMS.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${item.name} x${item.quantity}")
                    Text(item.displayPrice)
                }
            }
        },
        bottomBarContent = {
            Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth()) {
                Text("Checkout")
            }
        },
    )
}

@Composable
private fun CheckoutScreen(
    viewModel: CheckoutTwoStepExampleViewModel,
    onBack: () -> Unit,
) {
    val activity = requireNotNull(LocalActivity.current as? ComponentActivity)
    val presenter = remember(activity) { viewModel.controller.createPresenter(activity) }
    val paymentElement = remember(presenter) { presenter.paymentElement() }
    val status by viewModel.status.collectAsState()
    val session by viewModel.controller.session.collectAsState()
    val isUpdating by viewModel.controller.isUpdating.collectAsState()
    val confirmationMessage by viewModel.confirmationMessage.collectAsState()

    LaunchedEffect(Unit) {
        // Configured state survives process death, so only start a new checkout when there isn't one.
        if (status == CheckoutTwoStepExampleViewModel.Status.Idle) {
            viewModel.startCheckout()
        }
    }
    BackHandler(onBack = onBack)

    PlaygroundTheme(
        topBarContent = { ExampleTopAppBar(title = "Checkout", onBack = onBack) },
        content = {
            when (val currentStatus = status) {
                CheckoutTwoStepExampleViewModel.Status.Idle,
                CheckoutTwoStepExampleViewModel.Status.Loading -> LoadingContent()
                is CheckoutTwoStepExampleViewModel.Status.Error -> CheckoutError(
                    message = currentStatus.message,
                    onRetry = viewModel::startCheckout,
                )
                CheckoutTwoStepExampleViewModel.Status.Configured,
                CheckoutTwoStepExampleViewModel.Status.Completed -> {
                    session?.let { currentSession ->
                        LineItemsSection(currentSession)
                        TotalSummarySection(currentSession)
                        if (currentStatus == CheckoutTwoStepExampleViewModel.Status.Configured) {
                            Spacer(Modifier.height(16.dp))
                            paymentElement.Content()
                        }
                    } ?: LoadingContent()
                }
            }
        },
        bottomBarContent = {
            when (status) {
                CheckoutTwoStepExampleViewModel.Status.Configured -> {
                    confirmationMessage?.let { Text(text = it, color = MaterialTheme.colors.error) }
                    Button(
                        onClick = {
                            viewModel.clearConfirmationMessage()
                            presenter.confirm()
                        },
                        enabled = !isUpdating && session?.paymentOption != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Pay") }
                }
                CheckoutTwoStepExampleViewModel.Status.Completed -> {
                    Text("Payment complete!", style = MaterialTheme.typography.h6)
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                }
                else -> Unit
            }
        },
    )
}

@Composable
private fun CheckoutError(message: String, onRetry: () -> Unit) {
    Text("Error", style = MaterialTheme.typography.h6, color = MaterialTheme.colors.error)
    Spacer(Modifier.height(8.dp))
    Text(message)
    Spacer(Modifier.height(16.dp))
    Button(onClick = onRetry) { Text("Retry") }
}

@Composable
private fun ExampleTopAppBar(title: String, onBack: (() -> Unit)?) {
    TopAppBar(
        windowInsets = AppBarDefaults.topAppBarWindowInsets,
        title = { Text(title) },
        navigationIcon = onBack?.let {
            {
                IconButton(onClick = it) {
                    Text("‹", style = MaterialTheme.typography.h4)
                }
            }
        },
    )
}
