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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.customersheet.ui.CustomerBottomSheet
import com.stripe.android.customersheet.ui.CustomerSheetScreen
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.utils.AnimationConstants

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
                        when (val viewState = viewModel.viewState.collectAsState().value) {
                            is CustomerSheetViewState.SelectPaymentMethod -> {
                                CustomerSheetScreen(
                                    header = viewState.title,
                                    isLiveMode = false,
                                    isProcessing = false,
                                    isEditing = false,
                                    onBackPressed = {
                                        onBackPressedDispatcher.onBackPressed()
                                    },
                                    onEdit = {
                                        TODO()
                                    }
                                )
                            }
                            is CustomerSheetViewState.Loading -> {
                                Loading()
                            }
                        }
                    }
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

    override fun finish() {
        super.finish()
        overridePendingTransition(AnimationConstants.FADE_IN, AnimationConstants.FADE_OUT)
    }
}

@Composable
private fun Loading() {
    val padding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}
