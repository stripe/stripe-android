package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.di.subComponentBuilderProvider
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GoNext
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ConsentViewModel @Inject constructor(
    initialState: ConsentState,
    private val acceptConsent: AcceptConsent,
    private val goNext: GoNext
) : MavericksViewModel<ConsentState>(initialState) {

    fun onContinueClick() {
        viewModelScope.launch {
            val manifest = acceptConsent()
            goNext(manifest)
        }
    }

    companion object : MavericksViewModelFactory<ConsentViewModel, ConsentState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: ConsentState
        ): ConsentViewModel {
            return viewModelContext.subComponentBuilderProvider
                .consentSubComponentBuilder.get()
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class ConsentState(
    val test: Unit = Unit
) : MavericksState
