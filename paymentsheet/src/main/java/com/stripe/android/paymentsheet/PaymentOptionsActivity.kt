package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.utils.applicationIsTaskOwner
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull

/**
 * An `Activity` for selecting a payment option.
 */
internal class PaymentOptionsActivity : BaseSheetActivity<PaymentOptionResult>() {

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
        val starterArgs = initializeStarterArgs()
        super.onCreate(savedInstanceState)

        if (starterArgs == null) {
            finish()
            return
        }

        if (!applicationIsTaskOwner()) {
            viewModel.analyticsListener.cannotProperlyReturnFromLinkAndOtherLPMs()
        }

        setContent {
            StripeTheme {
                val isProcessing by viewModel.processing.collectAsState(Dispatchers.Main)

                val bottomSheetState = rememberStripeBottomSheetState(
                    confirmValueChange = { !isProcessing },
                )

                LaunchedEffect(Unit) {
                    viewModel.paymentOptionResult.filterNotNull().collect { sheetResult ->
                        setActivityResult(sheetResult)
                        bottomSheetState.hide()
                        viewModel.navigationHandler.closeScreens()
                        finish()
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

    private fun initializeStarterArgs(): PaymentOptionContract.Args? {
        starterArgs?.configuration?.appearance?.parseAppearance()
        earlyExitDueToIllegalState = starterArgs == null
        return starterArgs
    }

    override fun setActivityResult(result: PaymentOptionResult) {
        setResult(
            result.resultCode,
            Intent().putExtras(result.toBundle()),
        )
    }
}
