package com.stripe.android.paymentsheet.addresselement

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stripe.android.common.ui.BottomSheet
import com.stripe.android.common.ui.rememberBottomSheetState
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.utils.fadeOut
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        starterArgs.config?.appearance?.parseAppearance()

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            viewModel.navigator.navigationController = navController

            val bottomSheetState = rememberBottomSheetState()

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
                    onDismissed = viewModel.navigator::dismiss,
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = AddressElementScreen.InputAddress.route,
                        ) {
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
        fadeOut()
    }
}
