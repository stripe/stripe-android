package com.stripe.android.paymentsheet.example.samples.ui.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.model.CartProduct
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.example.samples.ui.EMOJI_FONT_SIZE
import com.stripe.android.paymentsheet.example.samples.ui.MAIN_FONT_SIZE
import com.stripe.android.paymentsheet.example.samples.ui.PADDING
import com.stripe.android.paymentsheet.example.samples.ui.ROW_START
import com.stripe.android.paymentsheet.example.samples.ui.SUB_FONT_SIZE

@SuppressWarnings("LongMethod")
@Composable
fun Receipt(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    cartState: CartState,
    isEditable: Boolean = false,
    onQuantityChanged: (CartProduct.Id, Int) -> Unit = { _, _ -> },
    bottomContent: @Composable () -> Unit,
) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier,
        color = MaterialTheme.colors.background,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(state = scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp, top = 2.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            } else {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
            Text(
                stringResource(R.string.cart_title),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Card(Modifier.fillMaxWidth(1f)) {
                Column(Modifier.fillMaxWidth(1f)) {
                    for (product in cartState.products) {
                        ProductRow(
                            isProcessing = isLoading,
                            productEmoji = product.icon,
                            productResId = product.nameResId,
                            priceString = product.unitPriceString,
                            quantity = product.quantity,
                            isEditable = isEditable,
                            onQuantityChanged = { quantity ->
                                onQuantityChanged(product.id, quantity)
                            },
                        )
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth(1f)
                    .padding(vertical = PADDING)
            ) {
                ReceiptRow(stringResource(R.string.subtotal), cartState.formattedSubtotal)
                ReceiptRow(stringResource(R.string.sales_tax), cartState.formattedTax)
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = PADDING)
                )
                ReceiptRow(stringResource(R.string.total), cartState.formattedTotal)
                bottomContent()
            }
        }
    }
}

@Composable
fun ProductRow(
    isProcessing: Boolean,
    productEmoji: String,
    productResId: Int,
    priceString: String,
    quantity: Int,
    isEditable: Boolean,
    onQuantityChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(PADDING)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            productEmoji,
            modifier = Modifier.padding(end = 4.dp),
            fontSize = EMOJI_FONT_SIZE
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(productResId),
                fontSize = MAIN_FONT_SIZE
            )
            Text(
                text = priceString,
                fontSize = SUB_FONT_SIZE
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEditable) {
                IconButton(
                    onClick = { onQuantityChanged(quantity - 1) },
                    enabled = !isProcessing && quantity > 0,
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null)
                }
            }

            Text(
                text = if (isEditable) {
                    quantity.toString()
                } else {
                    "${quantity}x"
                },
                fontSize = MAIN_FONT_SIZE,
            )

            if (isEditable) {
                IconButton(
                    onClick = { onQuantityChanged(quantity + 1) },
                    enabled = !isProcessing,
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(
    description: String,
    priceString: String,
) {
    Row(
        Modifier.padding(vertical = PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = description,
            modifier = Modifier.fillMaxWidth(ROW_START),
            fontSize = MAIN_FONT_SIZE,
            color = MaterialTheme.colors.onSurface,
        )
        Text(
            text = priceString,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle.Default.copy(textAlign = TextAlign.End),
            fontSize = MAIN_FONT_SIZE,
            color = MaterialTheme.colors.onSurface,
        )
    }
}

@Preview
@Composable
fun Receipt_Editable() {
    MaterialTheme {
        Surface {
            Receipt(
                isLoading = false,
                cartState = CartState.default,
                isEditable = true,
                onQuantityChanged = { _, _ -> },
                bottomContent = {},
            )
        }
    }
}

@Preview
@Composable
fun Receipt_NotEditable() {
    MaterialTheme {
        Surface {
            Receipt(
                isLoading = false,
                cartState = CartState.default,
                isEditable = false,
                onQuantityChanged = { _, _ -> },
                bottomContent = {},
            )
        }
    }
}
