package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.stripe.android.paymentsheet.ui.AddressOptionsAppBar
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.address.autocomplete.AddressAutocompleteResult
import com.stripe.android.ui.core.elements.AddressAutocompleteTextField
import com.stripe.android.ui.core.elements.AddressAutocompleteTextFieldController
import com.stripe.android.ui.core.shouldUseDarkDynamicColor
import kotlinx.coroutines.launch


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

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.addressResult.collect {
                setActivityResult(AddressOptionsResult.Succeeded(it.toString()))
            }
        }


        setContent {
            val modalBottomSheetState =
                rememberModalBottomSheetState(
                    ModalBottomSheetValue.Expanded,
                )
            val coroutineScope = rememberCoroutineScope()
            navController = rememberAnimatedNavController()

            LaunchedEffect(Unit) {
                snapshotFlow { modalBottomSheetState.currentValue }
                    .collect {
                       if (it == ModalBottomSheetValue.Hidden) {
                           viewModel.finish()
                           finish()
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
                                    AnimatedNavHost(navController, "BaseAddress") {
//                                        composable("Loading") {
//                                            Box(
//                                                modifier = Modifier
//                                                    .fillMaxWidth()
//                                                    .fillMaxHeight(),
//                                                contentAlignment = Alignment.Center
//                                            ) {
//                                                Loading()
//                                            }
//                                        }
                                        composable("BaseAddress") {
                                            Column {
                                                Text("BaseAddress Screen")
                                                FormUI(
                                                    viewModel.formController.hiddenIdentifiers,
                                                    viewModel.isEnabled,
                                                    viewModel.formController.elements,
                                                    viewModel.formController.lastTextFieldIdentifier
                                                ) {
                                                    Loading()
                                                }
                                                Button(onClick = { navController.navigate("AutoComplete") }) {
                                                    Text(text = "Change to AutoComplete screen")
                                                }
                                            }
                                        }
                                        composable("AutoComplete") {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().fillMaxHeight()
                                            ) {
                                                AddressAutocompleteTextField(
                                                    controller = AddressAutocompleteTextFieldController(
                                                        context = this@AddressOptionsActivity,
                                                        country = "US",
                                                        googlePlacesApiKey = "AIzaSyDnBZ_Wlh7BUW9WbpX-2__Vcz7qFKgnQTc",
                                                        workerScope = lifecycleScope
                                                    ),
                                                ) {
                                                    val autoCompleteResult =
                                                        AddressAutocompleteResult.Succeeded(it)
                                                    setResult(
                                                        autoCompleteResult.resultCode,
                                                        Intent().putExtras(autoCompleteResult.toBundle())
                                                    )
                                                    finish()
                                                }
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

        // Navigate to the initial screen once the view has been laid out.
//        window.decorView.rootView.viewTreeObserver.addOnGlobalLayoutListener(
//            object : ViewTreeObserver.OnGlobalLayoutListener {
//                override fun onGlobalLayout() {
//                    lifecycleScope.launch {
//                        navController.navigate("BaseAddress")
//                    }
//                    window.decorView.rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
//                }
//            })
    }

    fun setActivityResult(result: AddressOptionsResult) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(AddressOptionsActivityContract.Result(result).toBundle())
        )
    }

    @Composable
    private fun Loading() {
        Row(
            modifier = Modifier
                .height(
                    dimensionResource(R.dimen.stripe_paymentsheet_loading_container_height)
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val isDark = MaterialTheme.colors.surface.shouldUseDarkDynamicColor()
            CircularProgressIndicator(
                modifier = Modifier.size(
                    dimensionResource(R.dimen.stripe_paymentsheet_loading_indicator_size)
                ),
                color = if (isDark) Color.Black else Color.White,
                strokeWidth = dimensionResource(
                    R.dimen.stripe_paymentsheet_loading_indicator_stroke_width
                )
            )
        }
    }
}