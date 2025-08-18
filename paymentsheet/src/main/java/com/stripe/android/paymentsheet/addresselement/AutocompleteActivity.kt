package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import kotlinx.coroutines.flow.collectLatest

internal class AutocompleteActivity : AppCompatActivity() {
    private val starterArgs by lazy {
        AutocompleteContract.Args.fromIntent(intent)
    }

    private val viewModel: AutocompleteViewModel by viewModels<AutocompleteViewModel> {
        AutocompleteViewModel.Factory(requireNotNull(starterArgs))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val starterArgs = starterArgs
        if (starterArgs == null) {
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val appearanceContext = starterArgs.appearanceContext

        appearanceContext.applyAppearance()

        setContent {
            val bottomSheetState = rememberStripeBottomSheetState()

            LaunchedEffect(Unit) {
                viewModel.event.collectLatest { event ->
                    val result = when (event) {
                        is AutocompleteViewModel.Event.EnterManually -> AutocompleteContract.Result.EnterManually(
                            id = starterArgs.id,
                            addressDetails = event.addressDetails,
                        )
                        is AutocompleteViewModel.Event.GoBack -> AutocompleteContract.Result.Address(
                            id = starterArgs.id,
                            addressDetails = event.addressDetails,
                        )
                    }

                    setResult(result)

                    bottomSheetState.hide()
                    finish()
                }
            }

            BackHandler {
                viewModel.onBackPressed()
            }

            appearanceContext.Theme {
                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = viewModel::onBackPressed,
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AutocompleteScreenUI(
                            viewModel = viewModel,
                            isRootScreen = true,
                            appearanceContext = appearanceContext,
                            attributionDrawable =
                            PlacesClientProxy.getPlacesPoweredByGoogleDrawable(isSystemInDarkTheme()),
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()

        if (!isFinishing && !isChangingConfigurations) {
            setResult(
                AutocompleteContract.Result.Address(
                    id = starterArgs?.id ?: "",
                    addressDetails = null,
                )
            )
            finish()
        }
    }

    private fun setResult(result: AutocompleteContract.Result) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(result.toBundle())
        )
    }
}
