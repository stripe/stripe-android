package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.stripe.android.paymentsheet.databinding.ActivityPaymentOptionsBinding
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.utils.launchAndCollectIn
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.PaymentsTheme

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
    override val appbar: AppBarLayout by lazy { viewBinding.appbar }
    override val linkAuthView: ComposeView by lazy { viewBinding.linkAuth }
    override val toolbar: MaterialToolbar by lazy { viewBinding.toolbar }
    override val testModeIndicator: TextView by lazy { viewBinding.testmode }
    override val scrollView: ScrollView by lazy { viewBinding.scrollView }
    override val header: ComposeView by lazy { viewBinding.header }
    override val fragmentContainerParent: ViewGroup by lazy { viewBinding.fragmentContainerParent }
    override val messageView: TextView by lazy { viewBinding.message }
    override val notesView: ComposeView by lazy { viewBinding.notes }
    override val primaryButton: PrimaryButton by lazy { viewBinding.continueButton }
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

        viewModel.error.launchAndCollectIn(this) { error ->
            updateErrorMessage(
                messageView,
                error?.let { BaseSheetViewModel.UserErrorMessage(it) }
            )
        }

        viewBinding.contentContainer.setContent {
            PaymentsTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                currentScreen.PaymentOptionsContent(starterArgs)
            }
        }

        if (savedInstanceState == null) {
            viewModel.transitionToFirstScreenWhenReady()
        }

        viewModel.selection.launchAndCollectIn(this) {
            viewModel.clearErrorMessages()
            resetPrimaryButtonState()
        }

        viewModel.currentScreen.launchAndCollectIn(this) { currentScreen ->
            val visible = currentScreen is PaymentSheetScreen.AddFirstPaymentMethod ||
                currentScreen is PaymentSheetScreen.AddAnotherPaymentMethod ||
                viewModel.primaryButtonUIState.value?.visible == true

            viewBinding.continueButton.isVisible = visible
            viewBinding.bottomSpacer.isVisible = visible

            rootView.doOnNextLayout {
                // Expand sheet only after the first fragment is attached so that it
                // animates in. Further calls to expand() are no-op if the sheet is already
                // expanded.
                bottomSheetController.expand()
            }
        }
    }

    private fun initializeStarterArgs(): PaymentOptionContract.Args? {
        starterArgs?.state?.config?.appearance?.parseAppearance()
        earlyExitDueToIllegalState = starterArgs == null
        return starterArgs
    }

    override fun resetPrimaryButtonState() {
        viewBinding.continueButton.lockVisible = false
        viewBinding.continueButton.updateState(PrimaryButton.State.Ready)

        val customLabel = starterArgs?.state?.config?.primaryButtonLabel
        val label = customLabel ?: getString(R.string.stripe_continue_button_label)

        viewBinding.continueButton.setLabel(label)

        viewBinding.continueButton.setOnClickListener {
            viewModel.onUserSelection()
        }
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
