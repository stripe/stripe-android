package com.stripe.android.paymentsheet

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.stripe.android.paymentsheet.databinding.ActivityPaymentOptionsBinding
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.Html
import com.stripe.android.ui.core.getBackgroundColor

/**
 * An `Activity` for selecting a payment option.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal class PaymentOptionsActivity : BaseSheetActivity<PaymentOptionResult>() {
    @VisibleForTesting
    internal val viewBinding by lazy {
        ActivityPaymentOptionsBinding.inflate(layoutInflater)
    }

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        PaymentOptionsViewModel.Factory(
            { application },
            { requireNotNull(starterArgs) },
            this,
            intent?.extras
        )

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
    override val toolbar: MaterialToolbar by lazy { viewBinding.toolbar }
    override val testModeIndicator: TextView by lazy { viewBinding.testmode }
    override val scrollView: ScrollView by lazy { viewBinding.scrollView }
    override val header: ComposeView by lazy { viewBinding.header }
    override val fragmentContainerParent: ViewGroup by lazy { viewBinding.fragmentContainerParent }
    override val messageView: TextView by lazy { viewBinding.message }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val starterArgs = this.starterArgs
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

        setupContinueButton()
        setupNotes()

        viewModel.transition.observe(this) { event ->
            event?.getContentIfNotHandled()?.let { transitionTarget ->
                onTransitionTarget(
                    transitionTarget,
                    bundleOf(
                        PaymentSheetActivity.EXTRA_STARTER_ARGS to starterArgs,
                        PaymentSheetActivity.EXTRA_FRAGMENT_CONFIG to
                            transitionTarget.fragmentConfig
                    )
                )
            }
        }

        viewModel.fragmentConfigEvent.observe(this) { event ->
            val config = event.getContentIfNotHandled()
            if (config != null) {
                viewModel.transitionTo(
                    // It would be nice to see this condition move into the PaymentOptionsListFragment
                    // where we also jump to a new unsaved card. However this move require
                    // the transition target to specify when to and when not to add things to the
                    // backstack.
                    if (starterArgs.paymentMethods.isEmpty() && !config.isGooglePayReady) {
                        PaymentOptionsViewModel.TransitionTarget.AddPaymentMethodSheet(config)
                    } else {
                        PaymentOptionsViewModel.TransitionTarget.SelectSavedPaymentMethod(config)
                    }
                )
            }
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentStarted(fm: FragmentManager, fragment: Fragment) {
                    viewBinding.continueButton.isVisible =
                        fragment is PaymentOptionsAddPaymentMethodFragment
                }
            },
            false
        )
    }

    private fun setupContinueButton() {
        viewBinding.continueButton.lockVisible = false
        viewBinding.continueButton.updateState(PrimaryButton.State.Ready)

        viewModel.config?.let {
            val buttonColor = it.primaryButtonColor ?: ColorStateList.valueOf(
                PaymentsTheme.primaryButtonStyle.getBackgroundColor(baseContext)
            )
            viewBinding.continueButton.setAppearanceConfiguration(
                PaymentsTheme.primaryButtonStyle,
                buttonColor
            )
        }

        viewModel.primaryButtonOnPress.observe(this) { action ->
            viewBinding.continueButton.setOnClickListener {
                action()
            }
        }

        viewModel.primaryButtonText.observe(this) { text ->
            viewBinding.continueButton.setLabel(text)
        }

        viewModel.ctaEnabled.observe(this) { isEnabled ->
            viewBinding.continueButton.isEnabled = isEnabled
        }

        viewModel.ctaEnabled.observe(this) { isEnabled ->
            viewBinding.continueButton.isEnabled = isEnabled
        }

        viewModel.updatePrimaryButtonText(
            initial = true,
            text = getString(R.string.stripe_paymentsheet_continue_button_label)
        )

        viewModel.updatePrimaryButtonOnPress(initial = true) {
            viewModel.onUserSelection()
        }
    }

    private fun setupNotes() {
        viewModel.notesText.observe(this) { stringRes ->
            val showNotes = stringRes != null
            stringRes?.let {
                viewBinding.notes.setContent {
                    Html(
                        html = getString(stringRes),
                        imageGetter = mapOf(),
                        color = PaymentsTheme.colors.subtitle,
                        style = PaymentsTheme.typography.body1.copy(textAlign = TextAlign.Center)
                    )
                }
            }
            viewBinding.notes.isVisible = showNotes
            viewBinding.continueButton.updateLayoutParams {
                (this as? FrameLayout.LayoutParams)?.updateMargins(
                    bottom = if (showNotes) {
                        0
                    } else {
                        resources.getDimensionPixelSize(
                            R.dimen.stripe_paymentsheet_button_container_spacing_bottom
                        )
                    }
                )
            }
        }
    }

    private fun onTransitionTarget(
        transitionTarget: PaymentOptionsViewModel.TransitionTarget,
        fragmentArgs: Bundle
    ) {
        supportFragmentManager.commit {
            when (transitionTarget) {
                is PaymentOptionsViewModel.TransitionTarget.AddPaymentMethodFull -> {
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
                        fragmentArgs
                    )
                }
                is PaymentOptionsViewModel.TransitionTarget.SelectSavedPaymentMethod -> {
                    replace(
                        fragmentContainerId,
                        PaymentOptionsListFragment::class.java,
                        fragmentArgs
                    )
                }
                is PaymentOptionsViewModel.TransitionTarget.AddPaymentMethodSheet -> {
                    replace(
                        fragmentContainerId,
                        PaymentOptionsAddPaymentMethodFragment::class.java,
                        fragmentArgs
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
    }
}
