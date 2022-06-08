package com.stripe.android.financialconnections.di

import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.screens.ConsentSubcomponent
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject
import javax.inject.Provider

/**
 * Wrapper that exposes [FinancialConnectionsSheetComponent]'s registered
 * subcomponent builders.
 *
 * This will be injected and exposed in
 * [com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel]
 *
 */
internal class FinancialConnectionsSubcomponentBuilderProvider @Inject constructor(
    val consentSubComponentBuilder: Provider<ConsentSubcomponent.Builder>
    // other subcomponent builder providers will be added here
)

/**
 * helper to retrieve [FinancialConnectionsSubcomponentBuilderProvider] when building
 * a [com.airbnb.mvrx.MavericksViewModelFactory].
 */
internal val ViewModelContext.subComponentBuilderProvider: FinancialConnectionsSubcomponentBuilderProvider
    get() = this
        .activity<FinancialConnectionsSheetNativeActivity>()
        .viewModel.subcomponentBuilderProvider
