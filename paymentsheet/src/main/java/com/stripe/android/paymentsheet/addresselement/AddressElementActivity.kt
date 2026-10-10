package com.stripe.android.paymentsheet.addresselement

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressElementResultStateHolder.State
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.flow.filterIsInstance

@OptIn(ExperimentalMaterialApi::class)
internal class AddressElementActivity : ComponentActivity() {

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        AddressElementViewModel.Factory(
            applicationSupplier = { application },
            starterArgsSupplier = { requireNotNull(starterArgs) }
        )

    private val viewModel: AddressElementViewModel by viewModels { viewModelFactory }

    private val starterArgs by lazy {
        AddressElementActivityContract.Args.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val starterArgs = starterArgs
        if (starterArgs == null) {
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        starterArgs.config?.appearance?.parseAppearance()

        setContent {
            val navController = rememberNavController()
            viewModel.navigator.navigationController = navController

            val bottomSheetState = rememberStripeBottomSheetState(
                confirmValueChange = { target ->
                    target != ModalBottomSheetValue.Hidden || viewModel.resultStateHolder.state.value !is State.Saving
                },
            )

            LaunchedEffect(bottomSheetState) {
                viewModel.resultStateHolder.state
                    .filterIsInstance<State.Finished>()
                    .collect { state ->
                        bottomSheetState.hide()
                        finishWithResult(state.result)
                    }
            }

            BackHandler {
                if (viewModel.resultStateHolder.state.value != State.Idle) return@BackHandler

                if (!viewModel.navigator.onBack()) {
                    viewModel.resultStateHolder.onUserCancel()
                }
            }

            AddressElementUi(
                appearance = starterArgs.config?.appearance ?: ConfigurationDefaults.appearance,
                bottomSheetState = bottomSheetState,
                navController = navController,
            )
        }
    }

    @Composable
    private fun AddressElementUi(
        appearance: PaymentSheet.Appearance,
        bottomSheetState: StripeBottomSheetState,
        navController: NavHostController,
    ) {
        PaymentElementTheme(appearance = appearance) {
            ElementsBottomSheetLayout(
                state = bottomSheetState,
                onDismissed = viewModel.resultStateHolder::onUserCancel,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = AddressElementScreen.InputAddress.route,
                    ) {
                        composable(AddressElementScreen.InputAddress.route) {
                            InputAddressScreen(viewModel.inputAddressViewModelSubcomponentFactoryProvider)
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
                                viewModel.autoCompleteViewModelSubcomponentFactoryProvider,
                                viewModel.navigator,
                                country
                            )
                        }
                    }
                }
            }
        }
    }

    private fun finishWithResult(result: AddressElementActivityContract.Result) {
        setResult(
            result.resultCode,
            Intent().putExtras(
                result.toBundle()
            )
        )
        finish()
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }
}
