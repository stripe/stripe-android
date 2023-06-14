package com.stripe.android.customersheet

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.stripe.android.customersheet.ui.CustomerBottomSheet
import com.stripe.android.customersheet.ui.CustomerSheetScreen
import com.stripe.android.paymentsheet.utils.EdgeToEdge
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

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window?.statusBarColor = Color.TRANSPARENT

        setContent {
            StripeTheme {
                val navController = rememberNavController()
                CustomerBottomSheet(
                    navController = navController,
                    onClose = {
                        finishWithResult(InternalCustomerSheetResult.Canceled)
                    }
                ) {
                    EdgeToEdge { insets ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val viewState by viewModel.viewState.collectAsState()
                            val result by viewModel.result.collectAsState()

                            LaunchedEffect(result) {
                                result?.let { result ->
                                    setResult(result)
                                    navController.popBackStack()
                                }
                            }

                            CustomerSheetScreen(
                                viewState = viewState,
                                viewActionHandler = viewModel::handleViewAction,
                                paymentMethodNameProvider = viewModel::providePaymentMethodName,
                            )

                            Spacer(modifier = Modifier.requiredHeight(insets.navigationBar))
                        }
                    }
                }
            }
        }
    }

    private fun setResult(result: InternalCustomerSheetResult) {
        setResult(RESULT_OK, Intent().putExtras(result.toBundle()))
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
