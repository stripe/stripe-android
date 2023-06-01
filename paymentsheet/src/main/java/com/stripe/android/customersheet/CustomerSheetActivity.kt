package com.stripe.android.customersheet

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelProvider

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
            when (val viewState = viewModel.viewState.collectAsState().value) {
                is CustomerSheetViewState.Data -> {
                    Data(viewState.data)
                }
                CustomerSheetViewState.Loading -> {
                    Loading()
                }
            }
        }

        onBackPressedDispatcher.addCallback {
            finishWithResult(InternalCustomerSheetResult.Canceled)
        }
    }

    private fun finishWithResult(result: InternalCustomerSheetResult) {
        setResult(RESULT_OK, Intent().putExtras(result.toBundle()))
        finish()
    }
}

@Composable
private fun Loading() {
    Column {
        Text("loading...")
    }
}

@Composable
private fun Data(data: String) {
    Column {
        Text(data)
    }
}
