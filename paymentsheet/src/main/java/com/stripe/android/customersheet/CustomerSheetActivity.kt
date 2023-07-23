package com.stripe.android.customersheet

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.common.ui.BottomSheet
import com.stripe.android.common.ui.rememberBottomSheetState
import com.stripe.android.customersheet.ui.CustomerSheetScreen
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.utils.AnimationConstants

internal class CustomerSheetActivity : AppCompatActivity() {

    // TODO (jameswoo) Figure out how to create real view model in CustomerSheetActivityTest
    @VisibleForTesting
    internal var viewModelProvider: ViewModelProvider.Factory = CustomerSheetViewModel.Factory

    /**
     * TODO (jameswoo) verify that the [viewModels] delegate caches the right dependencies
     *
     * The ViewModel lifecycle is cached by this implementation, and the merchant might pass in
     * different dependencies, adapter, result callback, etc. This may require us to recreate our
     * [CustomerSessionScope], which would make it out of sync with what the [viewModels]
     * implementation caches.
     */
    private val viewModel: CustomerSheetViewModel by viewModels {
        viewModelProvider
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            StripeTheme {
                val bottomSheetState = rememberBottomSheetState()

                val viewState by viewModel.viewState.collectAsState()
                val result by viewModel.result.collectAsState()

                LaunchedEffect(result) {
                    result?.let { result ->
                        bottomSheetState.hide()
                        finishWithResult(result)
                    }
                }

                BackHandler {
                    viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)
                }

                BottomSheet(
                    state = bottomSheetState,
                    onDismissed = { viewModel.handleViewAction(CustomerSheetViewAction.OnDismissed) },
                ) {
                    CustomerSheetScreen(
                        viewState = viewState,
                        viewActionHandler = viewModel::handleViewAction,
                        paymentMethodNameProvider = viewModel::providePaymentMethodName,
                    )
                }
            }
        }
    }

    private fun finishWithResult(result: InternalCustomerSheetResult) {
        setResult(RESULT_OK, Intent().putExtras(result.toBundle()))
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(AnimationConstants.FADE_IN, AnimationConstants.FADE_OUT)
    }
}
