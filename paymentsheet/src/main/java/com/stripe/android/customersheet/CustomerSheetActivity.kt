package com.stripe.android.customersheet

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.customersheet.CustomerSheetViewAction.OnDismissed
import com.stripe.android.customersheet.ui.CustomerSheetScreen
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.fadeOut

internal class CustomerSheetActivity : AppCompatActivity() {

    private val args: CustomerSheetContract.Args? by lazy {
        CustomerSheetContract.Args.fromIntent(intent)
    }

    // TODO (jameswoo) Figure out how to create real view model in CustomerSheetActivityTest
    @VisibleForTesting
    internal var viewModelFactoryProducer: () -> ViewModelProvider.Factory = {
        CustomerSheetViewModel.Factory(args!!)
    }

    /**
     * TODO (jameswoo) verify that the [viewModels] delegate caches the right dependencies
     *
     * The ViewModel lifecycle is cached by this implementation, and the merchant might pass in
     * different dependencies, adapter, result callback, etc. This may require us to recreate our
     * [CustomerSessionScope], which would make it out of sync with what the [viewModels]
     * implementation caches.
     */
    private val viewModel: CustomerSheetViewModel by viewModels(
        factoryProducer = { viewModelFactoryProducer() },
    )

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (args == null) {
            finishWithResult(
                InternalCustomerSheetResult.Error(
                    exception = IllegalStateException("No CustomerSheetContract.Args provided"),
                )
            )
            return
        }

        viewModel.registerFromActivity(
            activityResultCaller = this,
            lifecycleOwner = this,
        )

        setContent {
            StripeTheme {
                val bottomSheetState = rememberStripeBottomSheetState(
                    confirmValueChange = {
                        if (it == ModalBottomSheetValue.Hidden) {
                            viewModel.bottomSheetConfirmStateChange()
                        } else {
                            true
                        }
                    }
                )

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

                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = { viewModel.handleViewAction(OnDismissed) },
                ) {
                    CustomerSheetScreen(viewModel)
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
        fadeOut()
    }
}
