package com.stripe.android.paymentsheet.example.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R

val EMOJI_FONT_SIZE = 36.sp
val MAIN_FONT_SIZE = 18.sp
val SUB_FONT_SIZE = 14.sp
val PADDING = 12.dp
const val ROW_START = .7f
val BACKGROUND_COLOR = Color(0xfff2f2f7)
val BUTTON_COLOR = Color(0xff635BFF)

const val SALAD_EMOJI = "\uD83E\uDD57"
const val HOT_DOG_EMOJI = "\uD83C\uDF2D"

internal class LaunchPaymentSheetCompleteActivity : BasePaymentSheetActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        setContent {
            MaterialTheme {
                val inProgress by viewModel.inProgress.observeAsState(false)
                val status by viewModel.status.observeAsState("")

                if (status.isNotBlank()) {
                    Toast.makeText(LocalContext.current, status, Toast.LENGTH_SHORT).show()
                    viewModel.statusDisplayed()
                }

                Receipt(
                    buyButtonEnabled = !inProgress,
                    buyButtonClickListener = {
                        prepareCheckout { customerConfig, clientSecret ->
                            if (isSetupIntent) {
                                paymentSheet.presentWithSetupIntent(
                                    clientSecret,
                                    PaymentSheet.Configuration(
                                        merchantDisplayName = merchantName,
                                        customer = customerConfig,
                                        googlePay = googlePayConfig,
                                    )
                                )
                            } else {
                                paymentSheet.presentWithPaymentIntent(
                                    clientSecret,
                                    PaymentSheet.Configuration(
                                        merchantDisplayName = merchantName,
                                        customer = customerConfig,
                                        googlePay = googlePayConfig,
                                    )
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun Receipt(
    buyButtonEnabled: Boolean,
    buyButtonClickListener: () -> Unit,
) {
    Surface(color = BACKGROUND_COLOR) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                stringResource(R.string.cart_title),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Card(Modifier.fillMaxWidth(1f)) {
                Column(
                    Modifier.fillMaxWidth(1f)
                ) {
                    ProductRow(HOT_DOG_EMOJI, R.string.hot_dog, "$0.99")
                    ProductRow(SALAD_EMOJI, R.string.salad, "$8.00")
                }
            }
            Column(
                Modifier.fillMaxWidth(1f).padding(vertical = PADDING)
            ) {
                ReceiptRow(stringResource(R.string.subtotal), "$8.99")
                ReceiptRow(stringResource(R.string.sales_tax), "$0.74")
                TotalLine(Modifier.align(Alignment.CenterHorizontally))
                ReceiptRow(stringResource(R.string.total), "$9.73")
                BuyButton(buyButtonEnabled, buyButtonClickListener)
            }
        }
    }
}

@Composable
fun ProductRow(
    productEmoji: String,
    productResId: Int,
    priceString: String,
) {
    Row(
        Modifier.padding(PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            productEmoji,
            modifier = Modifier.padding(end = 4.dp),
            fontSize = EMOJI_FONT_SIZE
        )
        Column(
            Modifier.fillMaxWidth(ROW_START)
        ) {
            Text(
                text = stringResource(productResId),
                fontSize = MAIN_FONT_SIZE
            )
            Text(
                text = stringResource(R.string.product_subtitle, stringResource(productResId)),
                fontSize = SUB_FONT_SIZE
            )
        }
        Text(
            text = priceString,
            Modifier.fillMaxWidth(),
            style = TextStyle.Default.copy(textAlign = TextAlign.End),
            fontSize = MAIN_FONT_SIZE
        )
    }
}

@Composable
fun ReceiptRow(
    description: String,
    priceString: String,
    color: Color = Color.Unspecified
) {
    Row(
        Modifier.padding(vertical = PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = description,
            modifier = Modifier.fillMaxWidth(ROW_START),
            fontSize = MAIN_FONT_SIZE,
            color = color
        )
        Text(
            text = priceString,
            Modifier.fillMaxWidth(),
            style = TextStyle.Default.copy(textAlign = TextAlign.End),
            fontSize = MAIN_FONT_SIZE,
            color = color
        )
    }
}

@Composable
fun BuyButton(
    buyButtonEnabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        enabled = buyButtonEnabled,
        modifier = Modifier.fillMaxWidth(1f),
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = BUTTON_COLOR,
            contentColor = Color.White
        )
    ) {
        Text(
            stringResource(R.string.buy),
            modifier = Modifier.padding(vertical = 2.dp),
            fontSize = MAIN_FONT_SIZE
        )
    }
}

@Composable
fun TotalLine(modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = PADDING)
            .height(1.dp)
            .background(Color.LightGray)
    )
}
