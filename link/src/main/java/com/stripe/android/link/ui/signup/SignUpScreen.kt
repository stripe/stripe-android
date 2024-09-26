package com.stripe.android.link.ui.signup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.stripe.android.link.LinkScreen
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun SignUpScreen(
    viewModel: SignUpViewModel,
    navController: NavHostController
) {
    LaunchedEffect("SignUpEffects") {
        viewModel.effect.collect { effect ->
            when (effect) {
                SignUpEffect.NavigateToWallet -> navController.navigate(LinkScreen.Wallet.route)
            }
        }
    }
    val state by viewModel.state.collectAsState()
    AnimatedVisibility(visible = state.loading) {
        CircularProgressIndicator()
    }
}
