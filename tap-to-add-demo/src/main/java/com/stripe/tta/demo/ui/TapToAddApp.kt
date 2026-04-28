package com.stripe.tta.demo.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.rememberEmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.tta.demo.CheckoutViewModel

@Suppress("MagicNumber")
private val ScreenBackgroundColor = Color(0xFFF8F9FA)

@Composable
@OptIn(TapToAddPreview::class)
internal fun TapToAddApp(
    checkoutViewModel: CheckoutViewModel,
) {
    MaterialTheme {
        Surface(
            color = ScreenBackgroundColor
        ) {
            val navController = rememberNavController()

            val paymentSheetBuilder = remember(checkoutViewModel) {
                PaymentSheet.Builder(checkoutViewModel::onPaymentSheetResult)
                    .createIntentCallback(checkoutViewModel)
                    .createCardPresentSetupIntentCallback(checkoutViewModel)
            }

            val paymentSheet = paymentSheetBuilder.build()

            val flowControllerBuilder = remember(checkoutViewModel) {
                PaymentSheet.FlowController.Builder(
                    checkoutViewModel::onPaymentSheetResult,
                    checkoutViewModel::onPaymentOption,
                )
                    .createIntentCallback(checkoutViewModel)
                    .createCardPresentSetupIntentCallback(checkoutViewModel)
            }

            val flowController = flowControllerBuilder.build()

            val embeddedBuilder = remember(checkoutViewModel) {
                EmbeddedPaymentElement.Builder(
                    createIntentCallback = checkoutViewModel,
                    resultCallback = EmbeddedPaymentElement.ResultCallback { result ->
                        checkoutViewModel.onEmbeddedPaymentResult(result)
                    },
                )
                    .createCardPresentSetupIntentCallback(checkoutViewModel)
            }

            val embeddedPaymentElement = rememberEmbeddedPaymentElement(embeddedBuilder)

            LaunchedEffect(Unit) {
                checkoutViewModel.navigateToSummary.collect {
                    navController.navigate(TapToAddNav.Summary) {
                        launchSingleTop = true
                    }
                }
            }

            TapToAddNavHost(
                navController = navController,
                checkoutViewModel = checkoutViewModel,
                paymentSheet = paymentSheet,
                flowController = flowController,
                embeddedPaymentElement = embeddedPaymentElement,
            )
        }
    }
}
