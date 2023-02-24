package com.stripe.android.paymentsheet.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.FragmentPrimaryButtonContainerBinding
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.utils.launchAndCollectIn
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBackgroundColor

internal abstract class BasePrimaryButtonContainerFragment : Fragment() {

    protected var viewBinding: FragmentPrimaryButtonContainerBinding? = null

    abstract val viewModel: BaseSheetViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentPrimaryButtonContainerBinding.inflate(inflater, container, false)
        return viewBinding?.root
    }

    abstract fun resetPrimaryButtonState()

    protected fun setupPrimaryButton() {
        val viewBinding = viewBinding ?: return

        viewModel.primaryButtonUIState.launchAndCollectIn(viewLifecycleOwner) { state ->
            state?.let {
                viewBinding.primaryButton.setOnClickListener {
                    state.onClick?.invoke()
                }
                viewBinding.primaryButton.setLabel(state.label)
                viewBinding.primaryButton.isVisible = state.visible
            } ?: run {
                resetPrimaryButtonState()
            }
        }

        viewModel.amount.launchAndCollectIn(viewLifecycleOwner) {
            resetPrimaryButtonState()
        }

        viewModel.selection.launchAndCollectIn(viewLifecycleOwner) {
            resetPrimaryButtonState()
        }

        viewModel.isPrimaryButtonEnabled.launchAndCollectIn(viewLifecycleOwner) { isEnabled ->
            viewBinding.primaryButton.isEnabled = isEnabled
        }

        viewBinding.primaryButton.setAppearanceConfiguration(
            StripeTheme.primaryButtonStyle,
            tintList = viewModel.config?.primaryButtonColor ?: ColorStateList.valueOf(
                StripeTheme.primaryButtonStyle.getBackgroundColor(requireActivity().baseContext)
            )
        )
    }

    override fun onDestroyView() {
        viewBinding = null
        super.onDestroyView()
    }
}

internal class PaymentSheetPrimaryButtonContainerFragment : BasePrimaryButtonContainerFragment() {

    override val viewModel: PaymentSheetViewModel by activityViewModels {
        PaymentSheetViewModel.Factory { error("PaymentSheetViewModel should already exist") }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupPrimaryButton()

        viewModel.currentScreen.launchAndCollectIn(viewLifecycleOwner) { currentScreen ->
            viewBinding?.primaryButton?.isVisible = currentScreen.showsBuyButton
        }

        viewModel.buyButtonState.launchAndCollectIn(viewLifecycleOwner) { state ->
            viewBinding?.primaryButton?.updateState(state?.convert())
        }
    }

    override fun resetPrimaryButtonState() {
        val viewBinding = viewBinding ?: return
        viewBinding.primaryButton.updateState(PrimaryButton.State.Ready)

        val customLabel = viewModel.config?.primaryButtonLabel

        val label = if (customLabel != null) {
            viewModel.config?.primaryButtonLabel
        } else if (viewModel.isProcessingPaymentIntent) {
            viewModel.amount.value?.buildPayButtonLabel(resources)
        } else {
            getString(R.string.stripe_setup_button_label)
        }

        viewBinding.primaryButton.setLabel(label)

        viewBinding.primaryButton.setOnClickListener {
            viewModel.checkout()
        }
    }
}

internal class PaymentOptionsPrimaryButtonContainerFragment : BasePrimaryButtonContainerFragment() {

    override val viewModel: PaymentOptionsViewModel by activityViewModels {
        PaymentOptionsViewModel.Factory { error("PaymentOptionsViewModel should already exist") }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupPrimaryButton()

        viewModel.currentScreen.launchAndCollectIn(viewLifecycleOwner) { currentScreen ->
            val visible = currentScreen is PaymentSheetScreen.AddFirstPaymentMethod ||
                currentScreen is PaymentSheetScreen.AddAnotherPaymentMethod ||
                viewModel.primaryButtonUIState.value?.visible == true

            viewBinding?.primaryButton?.isVisible = visible
        }
    }

    override fun resetPrimaryButtonState() {
        val viewBinding = viewBinding ?: return

        viewBinding.primaryButton.lockVisible = false
        viewBinding.primaryButton.updateState(PrimaryButton.State.Ready)

        val customLabel = viewModel.config?.primaryButtonLabel
        val label = customLabel ?: getString(R.string.stripe_continue_button_label)

        viewBinding.primaryButton.setLabel(label)

        viewBinding.primaryButton.setOnClickListener {
            viewModel.onUserSelection()
        }
    }
}

internal fun PaymentSheetViewState.convert(): PrimaryButton.State {
    return when (this) {
        is PaymentSheetViewState.Reset -> {
            PrimaryButton.State.Ready
        }
        is PaymentSheetViewState.StartProcessing -> {
            PrimaryButton.State.StartProcessing
        }
        is PaymentSheetViewState.FinishProcessing -> {
            PrimaryButton.State.FinishProcessing(this.onComplete)
        }
    }
}
