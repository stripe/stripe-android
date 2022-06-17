package com.stripe.android.paymentsheet.addresscollection

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.AddressOptionsAppBar
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.shouldUseDarkDynamicColor
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@OptIn(ExperimentalMaterialApi::class)
internal class AddressOptionsActivity : ComponentActivity() {

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        AddressOptionsViewModel.Factory(
            applicationSupplier = { application },
            starterArgsSupplier = { starterArgs }
        )

    private val viewModel: AddressOptionsViewModel by viewModels { viewModelFactory }

    private val starterArgs by lazy {
        requireNotNull(AddressOptionsActivityContract.Args.fromIntent(intent))
    }

    private lateinit var navController: NavHostController

    @OptIn(ExperimentalAnimationApi::class, ExperimentalSerializationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val modalBottomSheetState =
                rememberModalBottomSheetState(
                    ModalBottomSheetValue.Expanded,
                )
            val coroutineScope = rememberCoroutineScope()
            navController = rememberAnimatedNavController()
            viewModel.navigator.navigationController = navController

            LaunchedEffect(Unit) {
                snapshotFlow { modalBottomSheetState.currentValue }
                    .collect {
                       if (it == ModalBottomSheetValue.Hidden) {
                           dismiss()
                       }
                    }
            }



            ModalBottomSheetLayout(
                sheetState = modalBottomSheetState,
                sheetContent = {
                    Surface(
                        Modifier.fillMaxWidth()
                    ) {
                        PaymentsTheme {
                            Column {
                                AddressOptionsAppBar(
                                    isRootScreen = true,
                                    onButtonClick = {
                                        coroutineScope.launch {
                                            modalBottomSheetState.hide()
                                        }
                                    }
                                )
                                Column(Modifier.padding(horizontal = 20.dp)) {

                                    AnimatedNavHost(navController, AddressCollectionScreen.InputAddress.route) {
                                        composable(AddressCollectionScreen.InputAddress.route) {
                                            Column {
                                                Text("BaseAddress Screen ${address?.name}")
                                                Button(onClick = { viewModel.navigator.navigateTo(AddressCollectionScreen.Autocomplete) }) {
                                                    Text(text = "Change to AutoComplete screen")
                                                }
                                            }
                                        }
                                        composable(AddressCollectionScreen.Autocomplete.route) {
                                            Column {
                                                Text("AutoComplete Screen")
                                                Button(
                                                    onClick = {
                                                        val dummyAddress = CollectedAddress(name = "Skyler")
                                                        viewModel.navigator.setResult(CollectedAddress.KEY, dummyAddress)
                                                        viewModel.navigator.navigateTo(AddressCollectionScreen.InputAddress)
                                                    },
                                                    content = { Text(text = "Change to BaseAddress screen") }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
            ) {

            }
        }
    }

    private fun dismiss(result: AddressOptionsResult = AddressOptionsResult.Canceled) {
        setResult(
            result.resultCode,
            Intent()
                .putExtras(AddressOptionsActivityContract.Result(result).toBundle())
        )
        finish()
    }
}