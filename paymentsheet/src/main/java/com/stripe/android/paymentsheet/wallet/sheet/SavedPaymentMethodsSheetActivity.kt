package com.stripe.android.paymentsheet.wallet.sheet

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.paymentsheet.databinding.StripeActivitySavedPaymentMethodsSheetBinding
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.utils.launchAndCollectIn
import com.stripe.android.uicore.StripeTheme

internal class SavedPaymentMethodsSheetActivity : BaseSheetActivity<SavedPaymentMethodsSheetResult>() {
    internal val viewBinding by lazy {
        StripeActivitySavedPaymentMethodsSheetBinding.inflate(layoutInflater)
    }

    private var viewModelFactory: ViewModelProvider.Factory = SavedPaymentMethodsSheetViewModel.Factory {
        requireNotNull(starterArgs)
    }

    override val viewModel: SavedPaymentMethodsSheetViewModel by viewModels { viewModelFactory }

    private val starterArgs: SavedPaymentMethodsSheetContract.Args? by lazy {
        SavedPaymentMethodsSheetContract.Args.fromIntent(intent)
    }
    override val rootView: ViewGroup by lazy { viewBinding.root }
    override val bottomSheet: ViewGroup by lazy { viewBinding.bottomSheet }

    override fun onCreate(savedInstanceState: Bundle?) {
        val starterArgs = initializeStarterArgs()
        super.onCreate(savedInstanceState)

        if (starterArgs == null) {
            finish()
            return
        }

        starterArgs.statusBarColor?.let {
            window.statusBarColor = it
        }
        setContentView(viewBinding.root)

        viewModel.savedPaymentMethodsResults.launchAndCollectIn(this) {
            closeSheet(it)
        }

        viewBinding.content.setContent {
            StripeTheme {
                SavedPaymentMethodsSheetScreen(viewModel)
            }
        }
    }

    private fun initializeStarterArgs(): SavedPaymentMethodsSheetContract.Args? {
        starterArgs?.paymentSheetConfig?.appearance?.parseAppearance()
        earlyExitDueToIllegalState = starterArgs == null
        return starterArgs
    }
    override fun setActivityResult(result: SavedPaymentMethodsSheetResult) {
        setResult(
            result.resultCode,
            Intent().putExtras(result.toBundle()),
        )
    }
}