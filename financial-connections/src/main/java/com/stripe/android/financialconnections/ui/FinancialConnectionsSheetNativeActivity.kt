package com.stripe.android.financialconnections.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerScreen
import com.stripe.android.financialconnections.features.attachpayment.AttachPaymentScreen
import com.stripe.android.financialconnections.features.consent.ConsentScreen
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerScreen
import com.stripe.android.financialconnections.features.manualentry.ManualEntryScreen
import com.stripe.android.financialconnections.features.manualentrysuccess.ManualEntrySuccessScreen
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthScreen
import com.stripe.android.financialconnections.features.reset.ResetScreen
import com.stripe.android.financialconnections.features.success.SuccessScreen
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.Finish
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import javax.inject.Inject

internal class FinancialConnectionsSheetNativeActivity : AppCompatActivity(), MavericksView {

    val viewModel: FinancialConnectionsSheetNativeViewModel by viewModel()

    @Inject
    lateinit var navigationManager: NavigationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.activityRetainedComponent.inject(this)
        viewModel.onEach { postInvalidate() }
        onBackPressedDispatcher.addCallback { viewModel.onBackPressed() }
        setContent {
            FinancialConnectionsTheme {
                Column {
                    Box(modifier = Modifier.weight(1f)) { NavHost() }
                }
            }
        }
    }

    /**
     * handle state changes here.
     */
    override fun invalidate() {
        withState(viewModel) { state ->
            state.viewEffect?.let { viewEffect ->
                when (viewEffect) {
                    is OpenUrl -> startActivity(
                        CreateBrowserIntentForUrl(
                            context = this,
                            uri = Uri.parse(viewEffect.url)
                        )
                    )
                    Finish -> {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
                viewModel.onViewEffectLaunched()
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun NavHost() {
        val navController = rememberNavController()
        val onCloseClick = { viewModel.onCloseClick() }
        NavigationEffect(navController)
        NavHost(navController, startDestination = NavigationDirections.consent.destination) {
            composable(NavigationDirections.consent.destination) {
                ConsentScreen()
            }
            composable(NavigationDirections.manualEntry.destination) {
                ManualEntryScreen()
            }
            composable(
                route = NavigationDirections.ManualEntrySuccess.route,
                arguments = NavigationDirections.ManualEntrySuccess.arguments
            ) {
                ManualEntrySuccessScreen(
                    microdepositVerificationMethod = NavigationDirections
                        .ManualEntrySuccess.microdeposits(it),
                    last4 = NavigationDirections
                        .ManualEntrySuccess.last4(it)
                )
            }
            composable(NavigationDirections.institutionPicker.destination) {
                InstitutionPickerScreen()
            }
            composable(NavigationDirections.partnerAuth.destination) {
                PartnerAuthScreen()
            }
            composable(NavigationDirections.accountPicker.destination) {
                AccountPickerScreen()
            }
            composable(NavigationDirections.success.destination) {
                SuccessScreen(onCloseClick)
            }
            composable(NavigationDirections.reset.destination) {
                ResetScreen()
            }
            composable(NavigationDirections.attachLinkedPaymentAccount.destination) {
                AttachPaymentScreen()
            }
        }
    }

    /**
     * Handles new intents in the form of the redirect from the custom tab hosted auth flow
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.handleOnNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    @Composable
    private fun NavigationEffect(navController: NavHostController) {
        LaunchedEffect(navigationManager.commands) {
            navigationManager.commands.collect { command ->
                if (command.destination.isNotEmpty()) {
                    navController.navigate(command.destination) {
                        popUpAfterAuth(navController)
                    }
                }
            }
        }
    }

    /**
     * Skips auth screens from back navigation.
     */
    private fun NavOptionsBuilder.popUpAfterAuth(navController: NavHostController) {
        val destination: String = navController.currentBackStackEntry?.destination?.route ?: return
        val destinationsToSkipOnBack = listOf(
            NavigationDirections.partnerAuth.destination,
            NavigationDirections.reset.destination
        )
        if (destinationsToSkipOnBack.contains(navController.currentDestination?.route)
        ) {
            popUpTo(destination) {
                inclusive = true
            }
        }
    }
}
