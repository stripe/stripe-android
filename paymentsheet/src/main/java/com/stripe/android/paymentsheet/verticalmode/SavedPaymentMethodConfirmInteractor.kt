package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.lifecycle.viewModelScope
import com.stripe.android.common.spms.DefaultLinkFormElementFactory
import com.stripe.android.common.spms.DefaultSavedPaymentMethodLinkFormHelper
import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.common.spms.withLinkState
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal interface SavedPaymentMethodConfirmInteractor {
    val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod

    val formElement: FormElement?
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultSavedPaymentMethodConfirmInteractor(
    val initialSelection: PaymentSelection.Saved,
    val displayName: ResolvableString,
    val savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper,
    val updateSelection: (PaymentSelection.Saved) -> Unit,
    val coroutineScope: CoroutineScope,
) : SavedPaymentMethodConfirmInteractor {

    override val formElement = savedPaymentMethodLinkFormHelper.formElement

    override val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
        displayName = displayName,
        paymentMethod = initialSelection.paymentMethod,
    )

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
                displayName = paymentMethodMetadata.supportedPaymentMethodForCode(
                    PaymentMethod.Type.Card.code
                )?.displayName.orEmpty(),
                savedPaymentMethodLinkFormHelper = DefaultSavedPaymentMethodLinkFormHelper(
                    paymentMethodMetadata = paymentMethodMetadata,
                    linkConfigurationCoordinator = viewModel.linkHandler.linkConfigurationCoordinator,
                    savedStateHandle = viewModel.savedStateHandle,
                    linkFormElementFactory = DefaultLinkFormElementFactory,
                ),
                updateSelection = viewModel::updateSelection,
                coroutineScope = viewModel.viewModelScope,
            )
        }
    }
}
