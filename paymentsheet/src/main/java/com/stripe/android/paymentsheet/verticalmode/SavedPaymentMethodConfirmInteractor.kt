package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.common.spms.withLinkState
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface SavedPaymentMethodConfirmInteractor {
    val savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper

    val selection: StateFlow<PaymentSelection.Saved>

    val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod

    val state: StateFlow<State>

    val updateSelection: (PaymentSelection) -> Unit

    data class State(
        val linkFormEnabled: Boolean,
    )

    interface Factory {
        fun create(
            paymentSelection: PaymentSelection.Saved,
            updateSelection: (PaymentSelection) -> Unit,
        ): SavedPaymentMethodConfirmInteractor
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultSavedPaymentMethodConfirmInteractor(
    override val savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper,
    val initialSelection: PaymentSelection.Saved,
    val displayName: ResolvableString,
    override val updateSelection: (PaymentSelection) -> Unit,
    coroutineScope: CoroutineScope,
) : SavedPaymentMethodConfirmInteractor {

    override val state: StateFlow<SavedPaymentMethodConfirmInteractor.State> = savedPaymentMethodLinkFormHelper.state.mapAsStateFlow {
        SavedPaymentMethodConfirmInteractor.State(
            linkFormEnabled = it !is SavedPaymentMethodLinkFormHelper.State.Incomplete
        )
    }

    override val selection: StateFlow<PaymentSelection.Saved> = savedPaymentMethodLinkFormHelper.state.mapAsStateFlow {
        initialSelection.withLinkState(it)
    }

    override val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.Companion.create(
        displayName = displayName,
        paymentMethod = initialSelection.paymentMethod,
    )

    init {
        coroutineScope.launch {
            selection.collect {
                updateSelection(it)
            }
        }
    }

    // TODO: implement closing the coroutine scope.

    companion object {
        fun create(
            paymentMethodMetadata: PaymentMethodMetadata,
            savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper,
            initialSelection: PaymentSelection.Saved,
            updateSelection: (PaymentSelection) -> Unit,
            coroutineScope: CoroutineScope,
        ): DefaultSavedPaymentMethodConfirmInteractor {
            return DefaultSavedPaymentMethodConfirmInteractor(
                savedPaymentMethodLinkFormHelper = savedPaymentMethodLinkFormHelper,
                initialSelection = initialSelection,
                displayName = paymentMethodMetadata.supportedPaymentMethodForCode(PaymentMethod.Type.Card.code)?.displayName.orEmpty(),
                updateSelection = updateSelection,
                coroutineScope = coroutineScope,
            )
        }
    }

    class Factory @Inject constructor(
        private val paymentMethodMetadata: PaymentMethodMetadata,
        private val savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper,
        @ViewModelScope private val viewModelScope: CoroutineScope,
    ) : SavedPaymentMethodConfirmInteractor.Factory {
        override fun create(paymentSelection: PaymentSelection.Saved, updateSelection: (PaymentSelection) -> Unit): DefaultSavedPaymentMethodConfirmInteractor {
            return create(
                paymentMethodMetadata = paymentMethodMetadata,
                savedPaymentMethodLinkFormHelper = savedPaymentMethodLinkFormHelper,
                initialSelection = paymentSelection,
                updateSelection = updateSelection,
                coroutineScope = viewModelScope,
            )
        }

    }
}