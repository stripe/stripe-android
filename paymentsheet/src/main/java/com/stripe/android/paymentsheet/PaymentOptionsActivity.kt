package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.stripe.android.paymentsheet.databinding.ActivityPaymentOptionsBinding
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.utils.AnimationConstants

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

    private val fragmentContainerId: Int
        @IdRes
        get() = viewBinding.fragmentContainer.id

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

        viewModel.paymentOptionResult.observe(this) {
            closeSheet(it)
        }

        viewModel.error.observe(this) {
            updateErrorMessage(
                messageView,
                BaseSheetViewModel.UserErrorMessage(it)
            )
        }

        viewModel.transition.observe(this) { event ->
            clearErrorMessages()
            event?.getContentIfNotHandled()?.let { transitionTarget ->
                onTransitionTarget(transitionTarget)
            }
        }

        if (savedInstanceState == null) {
            viewModel.transitionToFirstScreenWhenReady()
        }

        viewModel.selection.observe(this) {
            clearErrorMessages()
            resetPrimaryButtonState()
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentStarted(fm: FragmentManager, fragment: Fragment) {
                    val visible =
                        fragment is PaymentOptionsAddPaymentMethodFragment ||
                            viewModel.primaryButtonUIState.value?.visible == true
                    viewBinding.continueButton.isVisible = visible
                    viewBinding.bottomSpacer.isVisible = visible
                }
            },
            false
        )
    }

    private fun initializeStarterArgs(): PaymentOptionContract.Args? {
        starterArgs?.state?.config?.appearance?.parseAppearance()
        earlyExitDueToIllegalState = starterArgs == null
        return starterArgs
    }

    private fun isSelectOrAddFragment() = supportFragmentManager.fragments.firstOrNull()?.let {
        it.tag == ADD_FULL_FRAGMENT_TAG ||
            it.tag == ADD_PAYMENT_METHOD_SHEET_TAG ||
            it.tag == SELECT_SAVED_PAYMENT_METHOD_TAG
    } ?: false

    override fun resetPrimaryButtonState() {
        viewBinding.continueButton.lockVisible = false
        viewBinding.continueButton.updateState(PrimaryButton.State.Ready)

        val customLabel = starterArgs?.state?.config?.primaryButtonLabel
        val label = customLabel ?: getString(R.string.stripe_continue_button_label)

        viewBinding.continueButton.setLabel(label)

        viewBinding.continueButton.setOnClickListener {
            clearErrorMessages()
            viewModel.onUserSelection()
        }
    }

    private fun onTransitionTarget(
        transitionTarget: BaseSheetViewModel.TransitionTarget,
    ) {
        val fragmentArgs = bundleOf(PaymentSheetActivity.EXTRA_STARTER_ARGS to starterArgs)

        supportFragmentManager.commit {
            when (transitionTarget) {
                is BaseSheetViewModel.TransitionTarget.AddPaymentMethodFull -> {
                    // Once the add fragment has been opened there is never a scenario that
                    // we should back to the add fragment from the select list view.
                    viewModel.hasTransitionToUnsavedLpm = true
                    setCustomAnimations(
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT,
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT
                    )
                    addToBackStack(null)

                    replace(
                        fragmentContainerId,
                        PaymentOptionsAddPaymentMethodFragment::class.java,
                        fragmentArgs,
                        ADD_FULL_FRAGMENT_TAG
                    )
                }
                is BaseSheetViewModel.TransitionTarget.SelectSavedPaymentMethod -> {
                    replace(
                        fragmentContainerId,
                        PaymentOptionsListFragment::class.java,
                        fragmentArgs,
                        SELECT_SAVED_PAYMENT_METHOD_TAG
                    )
                }
                is BaseSheetViewModel.TransitionTarget.AddPaymentMethodSheet -> {
                    // Once the add fragment has been opened there is never a scenario that
                    // we should back to the add fragment from the select list view.
                    viewModel.hasTransitionToUnsavedLpm = true
                    replace(
                        fragmentContainerId,
                        PaymentOptionsAddPaymentMethodFragment::class.java,
                        fragmentArgs,
                        ADD_PAYMENT_METHOD_SHEET_TAG
                    )
                }
            }
        }

        // Ensure the bottom sheet is expanded only after the fragment transaction is completed
        supportFragmentManager.executePendingTransactions()
        rootView.doOnNextLayout {
            // Expand sheet only after the first fragment is attached so that it animates in.
            // Further calls to expand() are no-op if the sheet is already expanded.
            bottomSheetController.expand()
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
        internal const val EXTRA_FRAGMENT_CONFIG = BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        internal const val EXTRA_STARTER_ARGS = BaseSheetActivity.EXTRA_STARTER_ARGS
        const val ADD_FULL_FRAGMENT_TAG = "AddFullFragment"
        const val ADD_PAYMENT_METHOD_SHEET_TAG = "AddPaymentMethodSheet"
        const val SELECT_SAVED_PAYMENT_METHOD_TAG = "SelectSavedPaymentMethod"
    }
}
