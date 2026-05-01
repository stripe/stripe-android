package com.stripe.android.paymentsheet.verticalmode

import androidx.lifecycle.viewModelScope
import com.stripe.android.common.spms.DefaultLinkFormElementFactory
import com.stripe.android.common.spms.DefaultLinkInlineSignupAvailability
import com.stripe.android.common.spms.DefaultSavedPaymentMethodLinkFormHelper
import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.common.spms.withLinkState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface SavedPaymentMethodConfirmInteractor {
    val state: StateFlow<State>

    data class State(
        val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
        val form: Form,
        val linkBrand: LinkBrand? = null,
    ) {
        data class Form(
            val elements: List<FormElement>,
            val enabled: Boolean,
        )
    }

    interface Factory {
        fun create(
            initialSelection: PaymentSelection.Saved,
            updateSelection: (PaymentSelection.Saved) -> Unit,
        ): SavedPaymentMethodConfirmInteractor
    }
}

internal class DefaultSavedPaymentMethodConfirmInteractor(
    val initialSelection: PaymentSelection.Saved,
    val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    val savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper,
    val processing: StateFlow<Boolean>,
    val updateSelection: (PaymentSelection.Saved) -> Unit,
    val coroutineScope: CoroutineScope,
    val linkBrand: LinkBrand?,
) : SavedPaymentMethodConfirmInteractor {
    override val state = processing.mapAsStateFlow { isProcessing ->
        SavedPaymentMethodConfirmInteractor.State(
            displayableSavedPaymentMethod = displayableSavedPaymentMethod,
            form = SavedPaymentMethodConfirmInteractor.State.Form(
                elements = savedPaymentMethodLinkFormHelper.formElement?.let { listOf(it) } ?: emptyList(),
                enabled = !isProcessing,
            ),
            linkBrand = linkBrand,
        )
    }

    private val selection = savedPaymentMethodLinkFormHelper.state.mapAsStateFlow {
        initialSelection.withLinkState(it)
    }

    init {
        coroutineScope.launch {
            selection.collectLatest {
                updateSelection(it)
            }
        }
    }

    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
            initialSelection: PaymentSelection.Saved,
        ): DefaultSavedPaymentMethodConfirmInteractor {
            return DefaultSavedPaymentMethodConfirmInteractor(
                initialSelection = initialSelection,
                displayableSavedPaymentMethod = initialSelection.paymentMethod.toDisplayableSavedPaymentMethod(
                    paymentMethodMetadata = paymentMethodMetadata,
                    defaultPaymentMethodId = null,
                ),
                savedPaymentMethodLinkFormHelper = DefaultSavedPaymentMethodLinkFormHelper(
                    linkInlineSignupAvailability = DefaultLinkInlineSignupAvailability(paymentMethodMetadata),
                    linkConfigurationCoordinator = viewModel.linkHandler.linkConfigurationCoordinator,
                    savedStateHandle = viewModel.savedStateHandle,
                    linkFormElementFactory = DefaultLinkFormElementFactory,
                ),
                processing = viewModel.processing,
                updateSelection = viewModel::updateSelection,
                coroutineScope = viewModel.viewModelScope,
                linkBrand = paymentMethodMetadata.linkBrand,
            )
        }
    }

    class Factory @Inject constructor(
        private val paymentMethodMetadata: PaymentMethodMetadata,
        private val savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper,
        private val processing: StateFlow<Boolean>,
        private val coroutineScope: CoroutineScope,
    ) : SavedPaymentMethodConfirmInteractor.Factory {
        override fun create(
            initialSelection: PaymentSelection.Saved,
            updateSelection: (PaymentSelection.Saved) -> Unit
        ): SavedPaymentMethodConfirmInteractor {
            return DefaultSavedPaymentMethodConfirmInteractor(
                initialSelection = initialSelection,
                displayableSavedPaymentMethod = initialSelection.paymentMethod.toDisplayableSavedPaymentMethod(
                    paymentMethodMetadata = paymentMethodMetadata,
                    defaultPaymentMethodId = null,
                ),
                processing = processing,
                savedPaymentMethodLinkFormHelper = savedPaymentMethodLinkFormHelper,
                updateSelection = updateSelection,
                coroutineScope = coroutineScope,
                linkBrand = paymentMethodMetadata.linkBrand,
            )
        }
    }
}
