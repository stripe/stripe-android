package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.utils.applicationIsTaskOwner
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.filterNotNull

/**
 * An `Activity` for selecting a payment option.
 */
internal class PaymentOptionsActivity : BaseSheetActivity<PaymentOptionsActivityResult>() {

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = PaymentOptionsViewModel.Factory {
        requireNotNull(starterArgs)
    }

    override val viewModel: PaymentOptionsViewModel by viewModels { viewModelFactory }

    private val starterArgs: PaymentOptionContract.Args? by lazy {
        PaymentOptionContract.Args.fromIntent(intent)
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val starterArgs = this.starterArgs
        if (starterArgs == null) {
            finish()
            return
        }

        starterArgs.configuration.appearance.parseAppearance()

        if (!applicationIsTaskOwner()) {
            viewModel.analyticsListener.cannotProperlyReturnFromLinkAndOtherLPMs()
        }

        viewModel.registerForActivityResult(
            activityResultCaller = this,
            lifecycleOwner = this,
        )

        setContent {
            StripeTheme {
                val isProcessing by viewModel.processing.collectAsState()

                val bottomSheetState = rememberStripeBottomSheetState(
                    confirmValueChange = { !isProcessing },
                )

                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                        viewModel.paymentOptionsActivityResult.filterNotNull().collect { sheetResult ->
                            setActivityResult(sheetResult)
                            bottomSheetState.hide()
                            viewModel.navigationHandler.closeScreens()
                            finish()
                        }
                    }
                }

                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = viewModel::onUserCancel,
                ) {
                    PaymentSheetScreen(viewModel)
                }
            }
        }
    }

    override fun setActivityResult(result: PaymentOptionsActivityResult) {
        setResult(
            result.resultCode,
            Intent().putExtras(result.toBundle()),
        )
    }
}
