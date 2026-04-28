package com.stripe.tta.demo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.tta.demo.CheckoutViewModel
import com.stripe.tta.demo.ui.screens.CatalogScreen
import com.stripe.tta.demo.ui.screens.CheckoutScreen
import com.stripe.tta.demo.ui.screens.SummaryScreen

@Composable
internal fun TapToAddNavHost(
    navController: NavHostController,
    checkoutViewModel: CheckoutViewModel,
    paymentSheet: PaymentSheet,
    flowController: PaymentSheet.FlowController,
    embeddedPaymentElement: EmbeddedPaymentElement,
) {
    NavHost(
        navController = navController,
        startDestination = TapToAddNav.Catalog,
    ) {
        composable(TapToAddNav.Catalog) {
            CatalogScreen(
                viewModel = checkoutViewModel,
                onNavigateToCheckout = {
                    navController.navigate(TapToAddNav.Checkout)
                },
            )
        }
        composable(TapToAddNav.Checkout) {
            CheckoutScreen(
                viewModel = checkoutViewModel,
                paymentSheet = paymentSheet,
                flowController = flowController,
                embeddedPaymentElement = embeddedPaymentElement,
                onNavigateToCatalog = { navController.popBackStack() },
            )
        }
        composable(TapToAddNav.Summary) {
            val orderTotal by checkoutViewModel.completedOrderTotal.collectAsState()
            SummaryScreen(
                orderTotal = orderTotal.orEmpty(),
                onShopAgain = {
                    checkoutViewModel.afterSuccessfulCheckoutShopAgain()
                    navController.popBackStack(
                        route = TapToAddNav.Catalog,
                        inclusive = false,
                    )
                },
            )
        }
    }
}
