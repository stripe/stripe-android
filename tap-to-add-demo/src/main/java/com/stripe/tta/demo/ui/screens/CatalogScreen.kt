package com.stripe.tta.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.tta.demo.CheckoutViewModel
import com.stripe.tta.demo.catalog.MockCatalog
import com.stripe.tta.demo.catalog.MockProduct

@Composable
internal fun CatalogScreen(
    viewModel: CheckoutViewModel,
    onNavigateToCheckout: () -> Unit,
) {
    val cartQuantities by viewModel.cartQuantities.collectAsState()
    val subtotalCents = MockCatalog.subtotalCents(cartQuantities)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Shop",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Select quantities, then continue to checkout.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            )
            MockCatalog.products.forEach { product ->
                CatalogProductRow(
                    product = product,
                    quantity = cartQuantities[product.id] ?: 0,
                    onIncrement = { viewModel.incrementQuantity(product.id) },
                    onDecrement = { viewModel.decrementQuantity(product.id) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Cart subtotal",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = MockCatalog.formatUsd(subtotalCents),
                    style = MaterialTheme.typography.h6,
                )
            }
            Button(
                onClick = onNavigateToCheckout,
                enabled = subtotalCents > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue to checkout")
            }
            OutlinedButton(
                onClick = { viewModel.clearCart() },
                enabled = cartQuantities.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear cart")
            }
        }
    }
}

@Composable
private fun CatalogProductRow(
    product: MockProduct,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = product.description,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${MockCatalog.formatUsd(product.unitPriceCents)} each",
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDecrement,
                        enabled = quantity > 0,
                    ) {
                        Text("−")
                    }
                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    OutlinedButton(onClick = onIncrement) {
                        Text("+")
                    }
                }
            }
        }
    }
}
