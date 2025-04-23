package com.stripe.android.link.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.link.injection.NativeLinkComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

internal class UpdateViewModel @Inject constructor(
    private val logger: Logger,
    initialState: UpdateScreenState,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<UpdateScreenState> = _state.asStateFlow()

    init {
        logger.info("paymentDetailsId: ${initialState.paymentDetailsId}")
    }
    fun onUpdateClicked() {
        logger.info("Update button clicked")
    }

    fun onCancelClicked() {
        logger.info("Cancel button clicked")
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            paymentDetailsId: String
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    UpdateViewModel(
                        initialState = UpdateScreenState.create(paymentDetailsId),
                        logger = parentComponent.logger
                    )
                }
            }
        }
    }
}
