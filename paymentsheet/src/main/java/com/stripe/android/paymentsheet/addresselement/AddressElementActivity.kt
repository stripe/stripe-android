package com.stripe.android.paymentsheet.addresselement

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.stripe.android.common.ui.BottomSheet
import com.stripe.android.common.ui.LoadingIndicator
import com.stripe.android.common.ui.rememberBottomSheetState
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.utils.AnimationConstants
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
internal class AddressElementActivity : ComponentActivity() {

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        AddressElementViewModel.Factory(
            applicationSupplier = { application },
            starterArgsSupplier = { starterArgs }
        )

    private val viewModel: AddressElementViewModel by viewModels { viewModelFactory }

    private val starterArgs by lazy {
        requireNotNull(AddressElementActivityContract.Args.fromIntent(intent))
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        starterArgs.config?.appearance?.parseAppearance()

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberAnimatedNavController()
            viewModel.navigator.navigationController = navController

            val bottomSheetState = rememberBottomSheetState(
                confirmValueChange = {
                    val route = navController.currentDestination?.route
                    route != AddressElementScreen.Autocomplete.route
                },
            )

            BackHandler {
                viewModel.navigator.onBack()
            }

            viewModel.navigator.onDismiss = { result ->
                coroutineScope.launch {
                    bottomSheetState.hide()
                    setResult(result)
                    finish()
                }
            }

            StripeTheme {
                BottomSheet(
                    state = bottomSheetState,
                    onShow = navController::navigateToContent,
                    onDismissed = viewModel.navigator::dismiss,
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AnimatedNavHost(
                            navController = navController,
                            startDestination = AddressElementScreen.Loading.route,
                        ) {
                            composable(AddressElementScreen.Loading.route) {
                                LoadingIndicator(modifier = Modifier.fillMaxSize())
                            }
                            composable(AddressElementScreen.InputAddress.route) {
                                InputAddressScreen(viewModel.inputAddressViewModelSubcomponentBuilderProvider)
                            }
                            composable(
                                AddressElementScreen.Autocomplete.route,
                                arguments = listOf(
                                    navArgument(AddressElementScreen.Autocomplete.countryArg) {
                                        type = NavType.StringType
                                    }
                                )
                            ) { backStackEntry ->
                                val country = backStackEntry
                                    .arguments
                                    ?.getString(
                                        AddressElementScreen.Autocomplete.countryArg
                                    )
                                AutocompleteScreen(
                                    viewModel.autoCompleteViewModelSubcomponentBuilderProvider,
                                    country
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setResult(result: AddressLauncherResult = AddressLauncherResult.Canceled) {
        setResult(
            result.resultCode,
            Intent().putExtras(
                AddressElementActivityContract.Result(result).toBundle()
            )
        )
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(AnimationConstants.FADE_IN, AnimationConstants.FADE_OUT)
    }
}

private fun NavHostController.navigateToContent() {
    return navigate(AddressElementScreen.InputAddress.route) {
        popUpTo(AddressElementScreen.Loading.route) {
            inclusive = true
        }
    }
}
