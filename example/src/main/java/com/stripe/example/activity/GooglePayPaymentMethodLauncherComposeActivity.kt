package com.stripe.example.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Checkbox
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.rememberGooglePayPaymentMethodLauncher
import com.stripe.android.model.CardBrand
import com.stripe.example.R
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class GooglePayPaymentMethodLauncherComposeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GooglePayPaymentMethodLauncherScreen()
        }
    }

    @Composable
    private fun GooglePayPaymentMethodLauncherScreen() {
        val scaffoldState = rememberScaffoldState()
        val scope = rememberCoroutineScope()
        var enabled by remember { mutableStateOf(false) }
        var checked by remember { mutableStateOf(false) }

        val googlePayConfig = GooglePayPaymentMethodLauncher.Config(
            environment = GooglePayEnvironment.Test,
            merchantCountryCode = "US",
            merchantName = "Widget Store",
            billingAddressConfig = GooglePayPaymentMethodLauncher.BillingAddressConfig(
                isRequired = true,
                format = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full,
                isPhoneNumberRequired = false
            ),
            existingPaymentMethodRequired = false,
            cardBrandFilter = if(checked) GooglePayPaymentMethodLauncherAcceptableBrandsFilter(CardBrand.Visa) else DefaultCardBrandFilter
        )

        val googlePayLauncher = rememberGooglePayPaymentMethodLauncher(
            config = googlePayConfig,
            readyCallback = { ready ->
                if (ready) {
                    enabled = true
                }
                scope.launch {
                    scaffoldState.snackbarHostState.showSnackbar("Google Pay ready? $ready")
                }
            },
            resultCallback = { result ->
                when (result) {
                    is GooglePayPaymentMethodLauncher.Result.Completed -> {
                        "Successfully created a PaymentMethod. ${result.paymentMethod}"
                    }
                    GooglePayPaymentMethodLauncher.Result.Canceled -> {
                        "Customer cancelled Google Pay."
                    }
                    is GooglePayPaymentMethodLauncher.Result.Failed -> {
                        "Google Pay failed: ${result.errorCode}: ${result.error.message}"
                    }
                }.let {
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(it)
                        enabled = false
                    }
                }
            }
        )

        GooglePayPaymentMethodLauncherScreen(
            scaffoldState = scaffoldState,
            enabled = enabled,
            onLaunchGooglePay = {
                googlePayLauncher.present(
                    currencyCode = "EUR",
                    amount = 2500L,
                )
            },
            checked,
            onChecked = { checked = it }
        )
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    private fun GooglePayPaymentMethodLauncherScreen(
        scaffoldState: ScaffoldState,
        enabled: Boolean,
        onLaunchGooglePay: () -> Unit,
        checked: Boolean,
        onChecked: (Boolean) -> Unit
    ) {
        Scaffold(scaffoldState = scaffoldState) {
            Column {
                AndroidView(
                    factory = { context ->
                        GooglePayButton(context)
                    },
                    modifier = Modifier
                        .wrapContentWidth()
                        .clickable(
                            enabled = enabled,
                            onClick = onLaunchGooglePay
                        )
                )
                Row {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onChecked
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp, top = 16.dp),
                        text = stringResource(R.string.allow_only_visa)
                    )
                }
            }
        }
    }
}

@Parcelize
class GooglePayPaymentMethodLauncherAcceptableBrandsFilter(
    private val cardBrands: Set<CardBrand>
) : CardBrandFilter {
    constructor(vararg cardBrands: CardBrand) : this(cardBrands.toSet())

    override fun isAccepted(cardBrand: CardBrand): Boolean {
        return cardBrand in cardBrands
    }
}
