package com.stripe.android.paymentsheet.example.playground.expresscheckout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.StandaloneExpressCheckoutElement
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.ExpressCheckoutShipping
import com.stripe.android.paymentsheet.example.playground.settings.ExpressCheckoutShippingSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.ExpressCheckoutWalletOrder
import com.stripe.android.paymentsheet.example.playground.settings.ExpressCheckoutWalletOrderSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.GooglePaySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.InitializationType
import com.stripe.android.paymentsheet.example.playground.settings.LinkDisplaySetting
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.createBillingDetails

@Composable
internal fun ExpressCheckoutPlayground(
    standaloneElement: StandaloneExpressCheckoutElement,
    playgroundState: PlaygroundState.Payment,
    lastGooglePayUpdate: String?,
) {
    var configurationResult by remember(standaloneElement, playgroundState) { mutableStateOf<Result<Unit>?>(null) }
    LaunchedEffect(standaloneElement, playgroundState) {
        configurationResult = configure(standaloneElement, playgroundState)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Standalone Express Checkout Element")
        when (val result = configurationResult) {
            null -> CircularProgressIndicator()
            else -> {
                if (result.isSuccess) {
                    standaloneElement.expressCheckoutElement.Content()
                } else {
                    Text(
                        text = "Configuration failed: ${result.exceptionOrNull()?.message}",
                        color = MaterialTheme.colors.error,
                        modifier = Modifier.testTag(EXPRESS_CHECKOUT_ERROR_TAG),
                    )
                }
            }
        }
        lastGooglePayUpdate?.let {
            Text(it, modifier = Modifier.testTag(EXPRESS_CHECKOUT_UPDATE_TAG))
        }
    }
}

private suspend fun configure(
    standaloneElement: StandaloneExpressCheckoutElement,
    state: PlaygroundState.Payment,
): Result<Unit> {
    val configuration = state.expressCheckoutConfiguration()
    return if (state.initializationType == InitializationType.Normal) {
        if (state.checkoutMode == CheckoutMode.SETUP) {
            standaloneElement.configureWithSetupIntent(state.clientSecret, configuration)
        } else {
            standaloneElement.configureWithPaymentIntent(state.clientSecret, configuration)
        }
    } else {
        standaloneElement.configure(state.intentConfiguration(), configuration)
    }
}

private fun PlaygroundState.Payment.expressCheckoutConfiguration(): StandaloneExpressCheckoutElement.Configuration {
    val elementConfiguration = ExpressCheckoutElement.Configuration()
        .linkConfiguration(
            ExpressCheckoutElement.Configuration.LinkConfiguration().display(
                when (snapshot[LinkSettingsDefinition]) {
                    LinkDisplaySetting.Automatic ->
                        ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic
                    LinkDisplaySetting.Never,
                    LinkDisplaySetting.WalletButtonHidden ->
                        ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never
                }
            )
        )
        .paymentMethodOrder(
            when (snapshot[ExpressCheckoutWalletOrderSettingsDefinition]) {
                ExpressCheckoutWalletOrder.Session -> emptyList()
                ExpressCheckoutWalletOrder.GooglePayFirst -> listOf("google_pay", "link")
                ExpressCheckoutWalletOrder.LinkFirst -> listOf("link", "google_pay")
            }
        )
    return StandaloneExpressCheckoutElement.Configuration("Example, Inc.")
        .elementConfiguration(elementConfiguration)
        .shippingAddress(
            required = snapshot[ExpressCheckoutShippingSettingsDefinition] != ExpressCheckoutShipping.Off,
            allowedCountryCodes = emptySet(),
        )
        .apply {
            GooglePaySettingsDefinition.createGooglePayConfiguration(
                configOption = snapshot[GooglePaySettingsDefinition],
                merchant = merchantCode,
                currency = currencyCode,
            )?.let { googlePay(it) }
            createBillingDetails(
                snapshot[DefaultBillingAddressSettingsDefinition],
            )?.let { defaultBillingDetails(it) }
        }
}

internal fun expressCheckoutGooglePayResponse(
    shipping: ExpressCheckoutShipping,
    update: GooglePayPaymentDataUpdate,
): GooglePayPaymentDataUpdateResponse {
    return if (shipping == ExpressCheckoutShipping.Reject && update.shippingAddress != null) {
        GooglePayPaymentDataUpdateResponse(
            newTransactionInfo = null,
            error = GooglePayPaymentDataError(
                reason = GooglePayPaymentDataError.Reason.ShippingAddressInvalid,
                message = "This shipping address was rejected by the playground callback.",
                intent = GooglePayPaymentDataError.Intent.ShippingAddress,
            ),
        )
    } else {
        val update = update.shippingAddress
        GooglePayPaymentDataUpdateResponse(
            newTransactionInfo = if (update?.countryCode == "CA") {
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = "CAD",
                    countryCode = update.countryCode,
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                    totalPrice = 7000
                )
            } else {
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = "USD",
                    countryCode = "US",
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                    totalPrice = 4000
                )
            },
            error = null,
        )
    }
}

internal const val EXPRESS_CHECKOUT_ERROR_TAG = "express_checkout_error"
internal const val EXPRESS_CHECKOUT_UPDATE_TAG = "express_checkout_google_pay_update"
