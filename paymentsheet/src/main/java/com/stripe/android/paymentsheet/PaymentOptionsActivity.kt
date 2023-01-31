package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.paymentsheet.databinding.ActivityPaymentOptionsBinding
import com.stripe.android.paymentsheet.databinding.FragmentPaymentOptionsPrimaryButtonBinding
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.utils.launchAndCollectIn
import com.stripe.android.uicore.StripeTheme

/**
 * An `Activity` for selecting a payment option.
 */
internal class PaymentOptionsActivity : BaseSheetActivity<PaymentOptionResult>() {
    @VisibleForTesting
    internal val viewBinding by lazy {
        ActivityPaymentOptionsBinding.inflate(layoutInflater)
    }

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = PaymentOptionsViewModel.Factory {
        requireNotNull(starterArgs)
    }

    override val viewModel: PaymentOptionsViewModel by viewModels { viewModelFactory }

    private val starterArgs: PaymentOptionContract.Args? by lazy {
        PaymentOptionContract.Args.fromIntent(intent)
    }

    override val rootView: ViewGroup by lazy { viewBinding.root }
    override val bottomSheet: ViewGroup by lazy { viewBinding.bottomSheet }
    override val linkAuthView: ComposeView by lazy { viewBinding.linkAuth }
    override val scrollView: ScrollView by lazy { viewBinding.scrollView }
    override val header: ComposeView by lazy { viewBinding.header }
    override val fragmentContainerParent: ViewGroup by lazy { viewBinding.fragmentContainerParent }
    override val notesView: ComposeView by lazy { viewBinding.notes }
    override val bottomSpacer: View by lazy { viewBinding.bottomSpacer }

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

        viewModel.paymentOptionResult.launchAndCollectIn(this) {
            closeSheet(it)
        }

        val elevation = resources.getDimension(R.dimen.stripe_paymentsheet_toolbar_elevation)
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            viewBinding.topBar.elevation = if (scrollView.scrollY > 0) {
                elevation
            } else {
                0f
            }
        }

        // This is temporary until we embed the top bar in a Scaffold
        viewBinding.topBar.clipToPadding = false

        viewBinding.topBar.setContent {
            StripeTheme {
                PaymentSheetTopBar(viewModel)
            }
        }

        viewBinding.contentContainer.setContent {
            StripeTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                currentScreen.Content(viewModel)
            }
        }

        viewBinding.message.setContent {
            StripeTheme {
                val errorMessage by viewModel.error.collectAsState(initial = null)

                errorMessage?.let { error ->
                    ErrorMessage(
                        error = error,
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 20.dp),
                    )
                }
            }
        }

        viewBinding.buttonContainer.setContent {
            AndroidViewBinding(
                factory = FragmentPaymentOptionsPrimaryButtonBinding::inflate,
            )
        }

        viewModel.selection.launchAndCollectIn(this) {
            viewModel.clearErrorMessages()
        }

        rootView.doOnNextLayout {
            // Expand sheet only after the first fragment is attached so that it
            // animates in. Further calls to expand() are no-op if the sheet is already
            // expanded.
            bottomSheetController.expand()
        }
    }

    private fun initializeStarterArgs(): PaymentOptionContract.Args? {
        starterArgs?.state?.config?.appearance?.parseAppearance()
        earlyExitDueToIllegalState = starterArgs == null
        return starterArgs
    }

    override fun setActivityResult(result: PaymentOptionResult) {
        setResult(
            result.resultCode,
            Intent()
                .putExtras(result.toBundle())
        )
    }

    internal companion object {
        internal const val EXTRA_STARTER_ARGS = BaseSheetActivity.EXTRA_STARTER_ARGS
    }
}
