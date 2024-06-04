package com.stripe.android.paymentsheet.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.databinding.StripeFragmentPrimaryButtonContainerBinding
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.utils.launchAndCollectIn
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBackgroundColor

internal abstract class BasePrimaryButtonContainerFragment : Fragment() {

    protected var viewBinding: StripeFragmentPrimaryButtonContainerBinding? = null

    abstract val viewModel: BaseSheetViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = StripeFragmentPrimaryButtonContainerBinding.inflate(inflater, container, false)
        return viewBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPrimaryButton()

        viewModel.primaryButtonUiState.launchAndCollectIn(viewLifecycleOwner) { uiState ->
            viewBinding?.primaryButton?.updateUiState(uiState)
        }
    }

    private fun setupPrimaryButton() {
        val viewBinding = viewBinding ?: return

        @Suppress("DEPRECATION")
        viewBinding.primaryButton.setAppearanceConfiguration(
            StripeTheme.primaryButtonStyle,
            tintList = viewModel.config.primaryButtonColor ?: ColorStateList.valueOf(
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
        super.onViewCreated(view, savedInstanceState)

        viewModel.buyButtonState.launchAndCollectIn(viewLifecycleOwner) { state ->
            viewBinding?.primaryButton?.updateState(state?.convert())
        }
    }
}

internal class PaymentOptionsPrimaryButtonContainerFragment : BasePrimaryButtonContainerFragment() {

    override val viewModel: PaymentOptionsViewModel by activityViewModels {
        PaymentOptionsViewModel.Factory { error("PaymentOptionsViewModel should already exist") }
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
