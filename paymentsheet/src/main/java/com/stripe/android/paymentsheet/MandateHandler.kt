package com.stripe.android.paymentsheet

import androidx.lifecycle.viewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.PaymentSheet.PaymentMethodLayout
import com.stripe.android.paymentsheet.model.MandateText
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class MandateHandler(
    coroutineScope: CoroutineScope,
    private val selection: StateFlow<PaymentSelection?>,
    private val merchantDisplayName: String,
    private val isVerticalMode: Boolean,
    private val isSetupFlowProvider: () -> Boolean,
) {
    private val _mandateText = MutableStateFlow<MandateText?>(null)
    val mandateText: StateFlow<MandateText?> = _mandateText

    init {
        coroutineScope.launch {
            selection.collect { selection ->
                val mandateText = selection?.mandateText(
                    merchantName = merchantDisplayName,
                    isSetupFlow = isSetupFlowProvider(),
                )

                val showAbove = (selection as? PaymentSelection.Saved?)?.showMandateAbovePrimaryButton == true
                updateMandateText(mandateText = mandateText, showAbove = showAbove)
            }
        }
    }

    fun updateMandateText(mandateText: ResolvableString?, showAbove: Boolean) {
        _mandateText.value = if (mandateText != null) {
            MandateText(
                text = mandateText,
                showAbovePrimaryButton = showAbove || isVerticalMode,
            )
        } else {
            null
        }
    }

    companion object {
        fun create(viewModel: BaseSheetViewModel): MandateHandler {
            return MandateHandler(
                coroutineScope = viewModel.viewModelScope,
                selection = viewModel.selection,
                merchantDisplayName = viewModel.config.merchantDisplayName,
                isVerticalMode = viewModel.config.paymentMethodLayout != PaymentMethodLayout.Horizontal,
                isSetupFlowProvider = { viewModel.paymentMethodMetadata.value?.stripeIntent is SetupIntent },
            )
        }
    }
}
