package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.content.DefaultPaymentOptionDisplayDataHolder
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal typealias InternalRowSelectionCallback = () -> Unit

@Singleton
internal interface EmbeddedRowSelectionImmediateActionHandler {
    fun prepareRowSelectionCallbackFromForm(formResult: FormResult.Complete)
    fun prepareRowSelectionCallbackNonFormRows(selectionCode: String)
    fun prepareRowSelectionCallbackSavedPaymentRow(updatedSelection: PaymentSelection.Saved)
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Singleton
internal class DefaultEmbeddedRowSelectionImmediateActionHandler @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
    private val paymentOptionDisplayDataHolder: DefaultPaymentOptionDisplayDataHolder,
) : EmbeddedRowSelectionImmediateActionHandler {
    val selection = selectionHolder.selection

    private val shouldInvokeRowSelectionCallback: StateFlow<Boolean> = savedStateHandle.getStateFlow(
        key = EMBEDDED_ROW_SELECTION_SHOULD_INVOKE_KEY,
        initialValue = false
    )
    private val promisedSelectionId: StateFlow<String?> = savedStateHandle.getStateFlow(
        key = EMBEDDED_ROW_SELECTION_PROMISED_ID_KEY,
        initialValue = null
    )

    init {
        coroutineScope.launch {
            paymentOptionDisplayDataHolder.paymentOption.collect { paymentOption ->
                if (paymentOption != null) {
                    handleImmediateRowActionCallback(
                        selection = selectionHolder.selection.value
                    )
                }
            }
        }
    }

    override fun prepareRowSelectionCallbackFromForm(formResult: FormResult.Complete) {
        if (formResult.hasBeenConfirmed) {
            return
        }
        val selection = selectionHolder.selection
        val updatedSelection = formResult.selection

        // if you don't change something in the form, then prepareRowSelectionCallback gets called
        // if you do change something in the form, then updatedSelection != selection and paymentOptiondisplayDataholder
        // will trigger handleImmediateRowActionCallback
        savedStateHandle[EMBEDDED_ROW_SELECTION_PROMISED_ID_KEY] = updatedSelection.code()
        prepareRowSelectionCallback {
            updatedSelection == selection.value
        }
    }

    override fun prepareRowSelectionCallbackNonFormRows(selectionCode: String) {
        savedStateHandle[EMBEDDED_ROW_SELECTION_PROMISED_ID_KEY] = selectionCode
        prepareRowSelectionCallback {
            selectionCode == selection.value.code()
        }
    }

    override fun prepareRowSelectionCallbackSavedPaymentRow(updatedSelection: PaymentSelection.Saved) {
        savedStateHandle[EMBEDDED_ROW_SELECTION_PROMISED_ID_KEY] = updatedSelection.paymentMethod.id
        prepareRowSelectionCallback {
            updatedSelection == selection.value
        }
    }


    private fun prepareRowSelectionCallback(isReselectedPredicate: () -> Boolean) {
        savedStateHandle[EMBEDDED_ROW_SELECTION_SHOULD_INVOKE_KEY] = true
        if (isReselectedPredicate()) {
            handleImmediateRowActionCallback(selection.value)
        }
    }

    private fun handleImmediateRowActionCallback(
        selection: PaymentSelection?,
    ) {
        if (shouldInvokeRowSelectionCallback.value) {
            savedStateHandle[EMBEDDED_ROW_SELECTION_SHOULD_INVOKE_KEY] = false
            internalRowSelectionCallback.get()?.let { callback ->
                if (
                    selection is PaymentSelection.Saved &&
                    selection.paymentMethod.id == promisedSelectionId.value
                ) {
                    callback.invoke()
                } else if (selection.code() == promisedSelectionId.value) {
                    callback.invoke()
                }
            }
        }
    }

    companion object {
        const val EMBEDDED_ROW_SELECTION_SHOULD_INVOKE_KEY = "EMBEDDED_ROW_SELECTION_SHOULD_INVOKE_KEY"
        const val EMBEDDED_ROW_SELECTION_PROMISED_ID_KEY = "EMBEDDED_ROW_SELECTION_PROMISED_ID_KEY"
    }
}
