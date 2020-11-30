package com.stripe.android.paymentsheet.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.SheetMode

/**
 * Base `ViewModel` for activities that use `BottomSheet`.
 */
internal abstract class SheetViewModel<TransitionTargetType, ViewStateType>(
    internal val isGuestMode: Boolean
) : ViewModel() {
    private val mutableError = MutableLiveData<Throwable>()
    internal val error: LiveData<Throwable> = mutableError

    protected val mutablePaymentMethods = MutableLiveData<List<PaymentMethod>>()
    internal val paymentMethods: LiveData<List<PaymentMethod>> = mutablePaymentMethods

    private val mutableTransition = MutableLiveData<TransitionTargetType>()
    internal val transition: LiveData<TransitionTargetType> = mutableTransition

    private val mutableSelection = MutableLiveData<PaymentSelection?>()
    internal val selection: LiveData<PaymentSelection?> = mutableSelection

    private val mutableSheetMode = MutableLiveData<SheetMode>()
    val sheetMode: LiveData<SheetMode> = mutableSheetMode.distinctUntilChanged()

    protected val mutableProcessing = MutableLiveData(false)
    val processing = mutableProcessing.distinctUntilChanged()

    protected val mutableViewState = MutableLiveData<ViewStateType>(null)
    internal val viewState: LiveData<ViewStateType> = mutableViewState.distinctUntilChanged()

    internal var shouldSavePaymentMethod: Boolean = false

    fun updateMode(mode: SheetMode) {
        mutableSheetMode.postValue(mode)
    }

    fun transitionTo(target: TransitionTargetType) {
        mutableTransition.postValue(target)
    }

    fun onError(throwable: Throwable) {
        mutableError.postValue(throwable)
    }

    fun updateSelection(selection: PaymentSelection?) {
        mutableSelection.postValue(selection)
    }
}
