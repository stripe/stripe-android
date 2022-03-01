package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.example.R
import com.stripe.example.Settings
import com.stripe.example.module.StripeIntentViewModel


/**
 * An Activity to demonstrate [PaymentLauncher] with Jetpack Compose.
 */
class ComposeExampleActivity : AppCompatActivity() {
    private val viewModel: StripeIntentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeScreen()
        }
    }

    @Composable
    fun ComposeScreen() {
        val inProgress by viewModel.inProgress.observeAsState(false)
        val status by viewModel.status.observeAsState("")
        var expanded by remember { mutableStateOf(false) }
        var selectedIndex by remember { mutableStateOf(0) }

        createPaymentLauncher().let { paymentLauncher ->
            Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                if (inProgress) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    stringResource(R.string.payment_auth_intro),
                    modifier = Modifier.padding(vertical = 5.dp),
                )

                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart)
                        .background(Color.Transparent)
                ) {
                    // Click handling happens on the box, so that it is a single accessible item
                    Box(
                        modifier = Modifier
                            .clickable(
                                onClickLabel = stringResource(com.stripe.android.paymentsheet.R.string.change),
                            ) {
                                expanded = true
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                start = 16.dp,
                                top = 4.dp,
                                bottom = 8.dp
                            )
                        ) {
//                            DropdownLabel(label)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    items[selectedIndex],
                                    modifier = Modifier.fillMaxWidth(.9f),
                                )
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.height(24.dp),
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        items.forEachIndexed { index, displayValue ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedIndex = index
                                    expanded = false
                                }
                            ) {
                                Text(text = displayValue)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val paramMap = Companion.params[items[selectedIndex]]?.toParamMap()
                        PaymentMethod.Type.fromCode(paramMap?.get("type") as String)?.let {
                            createAndConfirmPaymentIntent(
                                PaymentMethodCreateParams.createWithOverride(it, paramMap, setOf("test")),
                                paymentLauncher
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    enabled = !inProgress
                ) {
                    Text("Confirm")
                }

                Divider(modifier = Modifier.padding(vertical = 5.dp))
                Text(text = status)
            }
        }
    }

    /**
     * Create [PaymentLauncher] in a [Composable]
     */
    @Composable
    fun createPaymentLauncher(): PaymentLauncher {
        val settings = Settings(LocalContext.current)
        return PaymentLauncher.createForCompose(
            publishableKey = settings.publishableKey,
            stripeAccountId = settings.stripeAccountId
        ) {
            when (it) {
                is PaymentResult.Completed -> {
                    viewModel.status.value += "\n\nPaymentIntent confirmation succeeded\n\n"
                    viewModel.inProgress.value = false
                }
                is PaymentResult.Canceled -> {
                    viewModel.status.value += "\n\nPaymentIntent confirmation cancelled\n\n"
                    viewModel.inProgress.value = false
                }
                is PaymentResult.Failed -> {
                    viewModel.status.value += "\n\nPaymentIntent confirmation failed with " +
                        "throwable ${it.throwable} \n\n"
                    viewModel.inProgress.value = false
                }
            }
        }
    }

    @Composable
    fun ConfirmButton(
        params: PaymentMethodCreateParams,
        @StringRes buttonName: Int,
        paymentLauncher: PaymentLauncher,
        inProgress: Boolean
    ) {
        Button(
            onClick = { createAndConfirmPaymentIntent(params, paymentLauncher) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            enabled = !inProgress
        ) {
            Text(stringResource(buttonName))
        }
    }

    private fun createAndConfirmPaymentIntent(
        params: PaymentMethodCreateParams,
        paymentLauncher: PaymentLauncher,
    ) {
        viewModel.createPaymentIntent("us").observe(
            this
        ) {
            it.onSuccess { responseData ->
                val confirmPaymentIntentParams =
                    ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                        paymentMethodCreateParams = params,
                        clientSecret = responseData.getString("secret"),
                        shipping = SHIPPING
                    )
                paymentLauncher.confirm(confirmPaymentIntentParams)
            }
        }
    }

    private companion object {
        /**
         * See https://stripe.com/docs/payments/3d-secure#three-ds-cards for more options.
         */
        private val confirmParams3ds2 =
            PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.Builder()
                    .setNumber("4000000000003238")
                    .setExpiryMonth(1)
                    .setExpiryYear(2025)
                    .setCvc("123")
                    .build()
            )

        private val confirmParams3ds1 =
            PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.Builder()
                    .setNumber("4000000000003063")
                    .setExpiryMonth(1)
                    .setExpiryYear(2025)
                    .setCvc("123")
                    .build()
            )

        private val SHIPPING = ConfirmPaymentIntentParams.Shipping(
            address = Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build(),
            name = "Jenny Rosen",
            carrier = "Fedex",
            trackingNumber = "12345"
        )

        private val billingDetails = PaymentMethod.BillingDetails(
            address = Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build(),
            name = "Jenny Rosen",
            email = "jrosen@rosen.com",
            phone = "555-5555"
        )


        val params = mapOf(
            "3DS1" to confirmParams3ds1,
            "3DS2" to confirmParams3ds2,
            "sofort" to PaymentMethodCreateParams.Companion.create(
                PaymentMethodCreateParams.Sofort("AT"),
                billingDetails
            ),
            "bancontact" to PaymentMethodCreateParams.createBancontact(billingDetails),

            // TODO: Merchant country needs to be one of a set
            "bacsDebit" to PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.BacsDebit("00012345", "108800"),
                billingDetails
            ),
            // payment_method_data[type] not supported
            "blik" to PaymentMethodCreateParams.createBlik(
                billingDetails
            ),
            "sepa" to PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.SepaDebit("DE89370400440532013000"),
                billingDetails
            ),
            "eps" to PaymentMethodCreateParams.createEps(
                billingDetails,
            ),
            "giropay" to PaymentMethodCreateParams.createGiropay(
                billingDetails,
            ),
//            "fpx" to null,
            "iDeal" to PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Ideal("abn_amro"),
                billingDetails,
            ),
            "p24" to PaymentMethodCreateParams.createP24(
                billingDetails,
            ),
            // unkown parameter payment_method_data[netbanking]
//            "netbanking" to PaymentMethodCreateParams.create(
//                PaymentMethodCreateParams.Netbanking("NETBANKING_BANK"), // Field type?
//                billingDetails
//            ),
            // unkown parameter payment_method_data[netbanking]
//            "upi" to PaymentMethodCreateParams.create(
//                PaymentMethodCreateParams.Upi("VPA"), // Field type?
//                billingDetails
//            ),

            // Not a valid bsb number
//            "auBecsDebit" to PaymentMethodCreateParams.create(
//                PaymentMethodCreateParams.AuBecsDebit(
//                    "000-000",
//                    "000123456"
//                ),
//                billingDetails
//            ),
//            "afterPayClearPay" to null,
            "Alipay" to PaymentMethodCreateParams.createAlipay(),
            "grabPay" to PaymentMethodCreateParams.createGrabPay(
                billingDetails
            ),
            "oxxo" to PaymentMethodCreateParams.createOxxo(
                billingDetails
            ),
            "paypal" to PaymentMethodCreateParams.createPayPal(),
            "wechat" to PaymentMethodCreateParams.createWeChatPay(billingDetails),
        )

        val items = params.keys.toList()

    }
}
