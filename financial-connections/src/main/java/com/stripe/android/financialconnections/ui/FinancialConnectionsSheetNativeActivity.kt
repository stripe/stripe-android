@file:OptIn(ExperimentalMaterialApi::class)

package com.stripe.android.financialconnections.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerScreen
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

internal class FinancialConnectionsSheetNativeActivity : AppCompatActivity(), MavericksView {

    val viewModel: FinancialConnectionsSheetNativeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onEach { postInvalidate() }
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
                    is OpenAuthFlowWithUrl -> startActivity(
                        CreateBrowserIntentForUrl(
                            context = this,
                            uri = Uri.parse(viewEffect.url)
                        )
                    )
                }
                viewModel.onViewEffectLaunched()
            }
        }
    }

    @Composable
    fun NavHost() {
        val navController = rememberNavController()
        NavigationEffect(navController)
        NavHost(navController, startDestination = NavigationDirections.consent.destination) {
            composable(NavigationDirections.consent.destination) {
                ConsentScreen()
            }
            composable(NavigationDirections.institutionPicker.destination) {
                InstitutionPickerScreen()
            }
            composable(NavigationDirections.partnerAuth.destination) {
                PartnerAuthScreen()
            }
        }
    }

    @Composable
    private fun NavigationEffect(navController: NavHostController) {
        LaunchedEffect(viewModel.navigationManager.commands) {
            viewModel.navigationManager.commands.collect { command ->
                if (command.destination.isNotEmpty()) {
                    navController.navigate(command.destination)
                }
            }
        }
    }
}
