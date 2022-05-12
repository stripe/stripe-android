package com.stripe.android.financialconnections.di

import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.FinancialConnectionsSheetActivity
import com.stripe.android.financialconnections.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.screens.BankPickerSubcomponent
import javax.inject.Inject
import javax.inject.Provider

/**
 * Wrapper that exposes [FinancialConnectionsSheetComponent]'s registered
 * subcomponent builders.
 *
 * This will be injected and exposed in
 * [com.stripe.android.financialconnections.FinancialConnectionsSheetViewModel]
 *
 */
internal class FinancialConnectionsSubcomponentBuilderProvider @Inject constructor(
    val bankPickerSubComponentBuilder: Provider<BankPickerSubcomponent.Builder>
    // other subcomponent builder providers will be added here
)

/**
 * helper to retrieve [FinancialConnectionsSubcomponentBuilderProvider] when building
 * a [com.airbnb.mvrx.MavericksViewModelFactory].
 *
 *
 */
internal val ViewModelContext.subComponentBuilderProvider: FinancialConnectionsSubcomponentBuilderProvider
    get() = this
        .activity<FinancialConnectionsSheetNativeActivity>()
        .viewModel.subcomponentBuilderProvider