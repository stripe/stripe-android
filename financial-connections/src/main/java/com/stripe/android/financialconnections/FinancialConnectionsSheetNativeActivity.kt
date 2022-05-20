package com.stripe.android.financialconnections

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.airbnb.mvrx.viewModel
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.screens.BankPickerScreen
import com.stripe.android.financialconnections.screens.BankPickerViewModel

internal class FinancialConnectionsSheetNativeActivity : AppCompatActivity() {

    val viewModel: FinancialConnectionsSheetViewModel by viewModel()

    private val starterArgs: FinancialConnectionsSheetActivityArgs? by lazy {
        FinancialConnectionsSheetActivityArgs.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            setContent {
                Column {
                    Box(modifier = Modifier.weight(1f)) {
                        NavHost()
                    }
                }
            }
        }
    }

    @Composable
    fun NavHost() {
        val navController = rememberNavController()
        NavHost(navController, startDestination = "bank-picker") {
            composable("bank-picker") {
                val viewModel: BankPickerViewModel = mavericksViewModel()
                BankPickerScreen(viewModel, navController)
            }
            composable("bank-confirmation") {
                BankConfirmationScreen(navController)
            }
        }
    }

    @Composable
    fun BankConfirmationScreen(navController: NavHostController) {
        val activityScopedViewModel: FinancialConnectionsSheetViewModel = mavericksActivityViewModel()

        val state by activityScopedViewModel.collectAsState()

        Column {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Navigation Scoped Count: ${state.manifest.toString()}")
            Button(onClick = { navController.navigate("first") }) {
                Text("Next")
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
}
