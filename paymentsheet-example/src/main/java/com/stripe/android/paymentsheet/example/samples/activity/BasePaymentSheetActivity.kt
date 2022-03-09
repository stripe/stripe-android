package com.stripe.android.paymentsheet.example.samples.activity

import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.viewmodel.PaymentSheetViewModel

internal abstract class BasePaymentSheetActivity : AppCompatActivity() {
    protected val viewModel: PaymentSheetViewModel by lazy {
        PaymentSheetViewModel(application)
    }
    
    protected val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
        .setBackgroundTint(resources.getColor(R.color.black))
        .setTextColor(resources.getColor(R.color.white))
    }

    protected fun prepareCheckout(
        onSuccess: (PaymentSheet.CustomerConfiguration?, String) -> Unit
    ) {
        viewModel.prepareCheckout(backendUrl)

        viewModel.exampleCheckoutResponse.observe(this) { checkoutResponse ->
            // Init PaymentConfiguration with the publishable key returned from the backend,
            // which will be used on all Stripe API calls
            PaymentConfiguration.init(this, checkoutResponse.publishableKey)

            onSuccess(
                checkoutResponse.makeCustomerConfig(),
                checkoutResponse.paymentIntent
            )

            viewModel.exampleCheckoutResponse.removeObservers(this)
        }
    }

    protected open fun onPaymentSheetResult(
        paymentResult: PaymentSheetResult
    ) {
        viewModel.status.value = paymentResult.toString()
    }

    companion object {
        const val merchantName = "Example, Inc."
        const val backendUrl = "https://stripe-mobile-payment-sheet.glitch.me/checkout"
        val googlePayConfig = PaymentSheet.GooglePayConfiguration(
            environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
            countryCode = "US"
        )
    }
}

val EMOJI_FONT_SIZE = 36.sp
val MAIN_FONT_SIZE = 18.sp
val SUB_FONT_SIZE = 14.sp
val PADDING = 12.dp
const val ROW_START = .7f
val BACKGROUND_COLOR = Color(0xfff2f2f7)
val BUTTON_COLOR = Color(0xff635BFF)

const val SALAD_EMOJI = "\uD83E\uDD57"
const val HOT_DOG_EMOJI = "\uD83C\uDF2D"

@Composable
fun Receipt(
    isLoading: Boolean,
    bottomContent: @Composable () -> Unit
) {
    Surface(color = BACKGROUND_COLOR) {
        Column(
            Modifier.fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp, top = 2.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().height(4.dp))
            } else {
                Spacer(Modifier.fillMaxWidth().height(4.dp))
            }
            Text(
                stringResource(R.string.cart_title),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
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
                bottomContent()
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
fun PaymentMethodSelector(
    isEnabled: Boolean,
    paymentMethodLabel: String,
    @DrawableRes paymentMethodIcon: Int?,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.payment_method),
            modifier = Modifier.fillMaxWidth(0.5f)
                .padding(vertical = PADDING),
            fontSize = MAIN_FONT_SIZE
        )
        Row(
            Modifier.fillMaxWidth()
                .clickable(
                    enabled = isEnabled,
                    onClick = onClick
                ),
            horizontalArrangement = Arrangement.End
        ) {
            paymentMethodIcon?.let {
                Icon(
                    painter = painterResource(id = paymentMethodIcon),
                    contentDescription = null, // decorative element
                    modifier = Modifier.padding(horizontal = 4.dp),
                    tint = Color.Unspecified
                )
            }
            Text(
                text = paymentMethodLabel,
                fontSize = MAIN_FONT_SIZE,
                color = if (isEnabled) {
                    Color.Unspecified
                } else {
                    Color.Gray
                }
            )
        }
    }
}

@Composable
fun BuyButton(
    buyButtonEnabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        enabled = buyButtonEnabled,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
