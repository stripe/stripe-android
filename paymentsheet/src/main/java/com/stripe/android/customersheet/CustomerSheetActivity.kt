package com.stripe.android.customersheet

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.customersheet.ui.CustomerBottomSheet
import com.stripe.android.customersheet.ui.CustomerSheetScreen
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.utils.AnimationConstants
import kotlinx.coroutines.launch

internal class CustomerSheetActivity : AppCompatActivity() {

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = CustomerSheetViewModel.Factory

    /**
     * TODO verify that the [viewModels] delegate caches the right dependencies
     *
     * The ViewModel lifecycle is cached by this implementation, and the merchant might pass in
     * different dependencies, adapter, result callback, etc. This may require us to recreate our
     * [CustomerSessionScope], which would make it out of sync with what the [viewModels]
     * implementation caches.
     */
    private val viewModel by viewModels<CustomerSheetViewModel> {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StripeTheme {
                CustomerBottomSheet(
                    onClose = {
                        finishWithResult(InternalCustomerSheetResult.Canceled)
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val viewState by viewModel.viewState.collectAsState()
                        val errorMessage by viewModel.errorState.collectAsState()
                        CustomerSheetScreen(
                            viewState = viewState,
                            errorMessage = errorMessage,
                            viewActionHandler = viewModel::handleViewAction,
                        )
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.action.collect {
                handleAction(it)
            }
        }

        onBackPressedDispatcher.addCallback {
            finishWithResult(InternalCustomerSheetResult.Canceled)
        }
    }

    private fun handleAction(action: CustomerSheetAction?) {
        when (action) {
            is CustomerSheetAction.NavigateUp -> {
                finishWithResult(InternalCustomerSheetResult.Canceled)
            }
            null -> {
                // nothing to do
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
