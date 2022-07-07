package com.stripe.android.financialconnections.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [FinancialConnectionsApplicationModule::class]
)
internal interface FinancialConnectionsApplicationComponent {

    fun homeSubcomponentBuilder(): FinancialConnectionsSheetActivitySubcomponent.Builder
    fun nativeSubcomponentBuilder(): FinancialConnectionsSheetNativeActivitySubcomponent.Builder

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): FinancialConnectionsApplicationComponent
    }
}
