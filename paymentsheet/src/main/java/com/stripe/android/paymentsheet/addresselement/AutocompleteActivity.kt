package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.flow.collectLatest

internal class AutocompleteActivity : AppCompatActivity() {
    private val starterArgs by lazy {
        AutocompleteContract.Args.fromIntent(intent)
    }

    private val viewModel: AutocompleteViewModel by viewModels<AutocompleteViewModel> {
        AutocompleteViewModel.Factory(args = requireNotNull(starterArgs))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val starterArgs = starterArgs
        if (starterArgs == null) {
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        starterArgs.appearance.parseAppearance()

        setContent {
            val bottomSheetState = rememberStripeBottomSheetState()

            LaunchedEffect(Unit) {
                viewModel.event.collectLatest { event ->
                    val forceExpandForm = when (event) {
                        is AutocompleteViewModel.Event.EnterManually -> true
                        is AutocompleteViewModel.Event.GoBack -> false
                    }

                    setResult(
                        AutocompleteContract.Result(
                            id = starterArgs.id,
                            addressDetails = event.addressDetails,
                            forceExpandForm = forceExpandForm,
                        )
                    )

                    bottomSheetState.hide()
                    finish()
                }
            }

            BackHandler {
                viewModel.onBackPressed()
            }

            StripeTheme {
                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = viewModel::onBackPressed,
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AutocompleteScreenUI(
                            viewModel = viewModel,
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
                AutocompleteContract.Result(
                    id = starterArgs?.id ?: "",
                    addressDetails = null,
                    forceExpandForm = false
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
