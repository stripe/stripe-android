package com.stripe.android.paymentsheet.addresselement

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.ui.core.PaymentsTheme
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

    private lateinit var navController: NavHostController

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        starterArgs.config?.appearance?.parseAppearance()

        // set a default result in case the user closes the sheet manually
        setResult()

        setContent {
            val modalBottomSheetState =
                rememberModalBottomSheetState(
                    ModalBottomSheetValue.Expanded
                )

            navController = rememberAnimatedNavController()
            viewModel.navigator.navigationController = navController

            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                snapshotFlow { modalBottomSheetState.currentValue }.collect {
                    // finish the activity when the sheet closes.
                    if (it == ModalBottomSheetValue.Hidden) {
                        finish()
                    }
                }
            }

            viewModel.navigator.onDismiss = {
                setResult(it)
                coroutineScope.launch {
                    modalBottomSheetState.hide()
                }
            }

            ModalBottomSheetLayout(
                sheetState = modalBottomSheetState,
                sheetContent = {
                    PaymentsTheme {
                        Surface(
                            color = MaterialTheme.colors.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnimatedNavHost(
                                navController,
                                AddressElementScreen.InputAddress.route
                            ) {
                                composable(AddressElementScreen.InputAddress.route) {
                                    InputAddressScreen(viewModel.injector)
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
                                        viewModel.injector,
                                        country
                                    )
                                }
                            }
                        }
                    }
                },
                content = {},
                modifier = Modifier.navigationBarsPadding().systemBarsPadding()
            )
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
}
