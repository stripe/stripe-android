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
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stripe.android.common.ui.BottomSheetLoadingIndicator
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterialApi::class)
internal class AddressElementActivity : ComponentActivity() {

    private var activityArgs by mutableStateOf<AddressElementActivityContract.Args?>(null)

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        AddressElementViewModel.Factory(
            applicationSupplier = { application },
            starterArgsSupplier = { requireNotNull(activityArgs) }
        )

    private val viewModel: AddressElementViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialArgs = AddressElementActivityContract.Args.fromIntent(intent)
        if (initialArgs == null) {
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (initialArgs !is AddressElementActivityContract.Args.CheckoutShipping.Loading) {
            initialArgs.config?.appearance?.parseAppearance()
        }
        activityArgs = initialArgs

        setContent { AddressElementContent() }
    }

    @Composable
    private fun AddressElementContent() {
        val bottomSheetState = rememberStripeBottomSheetState()
        when (val args = requireNotNull(activityArgs)) {
            is AddressElementActivityContract.Args.CheckoutShipping.Loading -> {
                BackHandler {
                    finishWithResult(AddressElementActivityContract.Result.Canceled)
                }
                val loadingModifier = remember(bottomSheetState) { Modifier }
                StripeTheme {
                    ElementsBottomSheetLayout(
                        state = bottomSheetState,
                        onDismissed = {
                            finishWithResult(AddressElementActivityContract.Result.Canceled)
                        },
                    ) {
                        BottomSheetLoadingIndicator(modifier = loadingModifier)
                    }
                }
            }
            is AddressElementActivityContract.Args.CheckoutShipping.Ready,
            is AddressElementActivityContract.Args.Standalone -> {
                val navController = rememberNavController()
                viewModel.navigator.navigationController = navController

                LaunchedEffect(bottomSheetState) {
                    viewModel.resultStateHolder.result
                        .filterNotNull()
                        .collect { result ->
                            bottomSheetState.hide()
                            finishWithResult(result)
                        }
                }

                BackHandler {
                    if (!viewModel.navigator.onBack()) {
                        viewModel.resultStateHolder.setResult(
                            AddressElementActivityContract.Result.Canceled
                        )
                    }
                }

                AddressElementUi(bottomSheetState, navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNewIntent(intent)
    }

    private fun handleNewIntent(intent: Intent) {
        val updatedArgs = AddressElementActivityContract.Args.fromIntent(intent) ?: return
        val isValidTransition =
            !isFinishing &&
                activityArgs is AddressElementActivityContract.Args.CheckoutShipping.Loading &&
                updatedArgs is AddressElementActivityContract.Args.CheckoutShipping.Ready
        if (!isValidTransition) return

        updatedArgs.config?.appearance?.parseAppearance()
        this.intent = intent
        activityArgs = updatedArgs
    }

    @Composable
    private fun AddressElementUi(
        bottomSheetState: StripeBottomSheetState,
        navController: NavHostController,
    ) {
        StripeTheme {
            ElementsBottomSheetLayout(
                state = bottomSheetState,
                onDismissed = {
                    viewModel.resultStateHolder.setResult(AddressElementActivityContract.Result.Canceled)
                },
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
