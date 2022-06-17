package com.stripe.android.paymentsheet.addresselement

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
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

        // set a default result in case the user closes the sheet manually
        setResult()

        setContent {
            val modalBottomSheetState =
                rememberModalBottomSheetState(
                    ModalBottomSheetValue.Expanded,
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
                            Modifier.fillMaxWidth()
                        ) {
                            AnimatedNavHost(
                                navController,
                                AddressElementScreen.InputAddress.route
                            ) {
                                composable(AddressElementScreen.InputAddress.route) {
                                    InputAddressScreen(viewModel.injector)
                                }
                                composable(AddressElementScreen.Autocomplete.route) {
                                    AutoCompleteScreen(viewModel.injector)
                                }
                            }
                        }
                    }
                },
                content = {}
            )
        }
    }

    private fun setResult(result: AddressElementResult = AddressElementResult.Canceled) {
        setResult(
            result.resultCode,
            Intent().putExtras(
                AddressElementActivityContract.Result(result).toBundle()
            )
        )
    }
}