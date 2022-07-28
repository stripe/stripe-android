package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerSubcomponent
import com.stripe.android.financialconnections.features.consent.ConsentSubcomponent
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerSubcomponent
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthSubcomponent
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        FinancialConnectionsSheetNativeModule::class,
        FinancialConnectionsSheetSharedModule::class,
        LoggingModule::class,
        CoroutineContextModule::class
    ]
)
internal interface FinancialConnectionsSheetNativeComponent {
    fun inject(financialConnectionsSheetNativeActivity: FinancialConnectionsSheetNativeActivity)

    val viewModel: FinancialConnectionsSheetNativeViewModel

    // Exposed subcomponent builders.
    val consentBuilder: ConsentSubcomponent.Builder
    val institutionPickerBuilder: InstitutionPickerSubcomponent.Builder
    val accountPickerBuilder: AccountPickerSubcomponent.Builder
    val partnerAuthSubcomponent: PartnerAuthSubcomponent.Builder

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun initialState(initialState: FinancialConnectionsSheetNativeState): Builder

        @BindsInstance
        fun configuration(configuration: FinancialConnectionsSheet.Configuration): Builder

        fun build(): FinancialConnectionsSheetNativeComponent
    }
}
