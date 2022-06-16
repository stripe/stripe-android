package com.stripe.android.financialconnections.di

import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject
import javax.inject.Provider

/**
 * Wrapper that exposes [FinancialConnectionsSheetNativeComponent]'s registered
 * subcomponent builders.
 *
 * This will be injected and exposed in
 * [com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel]
 *
 */
internal class FinancialConnectionsSubcomponentBuilderProvider @Inject constructor(
    val consentSubComponentBuilder: Provider<ConsentSubcomponent.Builder>,
    val institutionPickerSubcomponentBuilder: Provider<InstitutionPickerSubcomponent.Builder>
    // other subcomponent builder providers will be added here
)

/**
 * Mavericks uses [com.airbnb.mvrx.MavericksViewModelFactory] to create ViewModel instances.
 *
 * Since these factories are static objects, we can't inject the subcomponent builder as usual.
 *
 * Instead, these factories offer access to a [ViewModelContext] instance.
 *
 * This helps to retrieve [FinancialConnectionsSubcomponentBuilderProvider] when from the
 * [ViewModelContext] instance by transparently accessing the viewModel property.
 */
internal val ViewModelContext.financialConnectionsSubComponentBuilderProvider:
    FinancialConnectionsSubcomponentBuilderProvider
        get() = this
            .activity<FinancialConnectionsSheetNativeActivity>()
            .viewModel.subcomponentBuilderProvider
