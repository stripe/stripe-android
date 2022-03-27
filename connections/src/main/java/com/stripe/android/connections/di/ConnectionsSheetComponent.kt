package com.stripe.android.connections.di

import com.stripe.android.connections.ConnectionsSheetContract
import com.stripe.android.connections.ConnectionsSheetViewModel
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.LoggingModule
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Singleton

@Singleton
@Subcomponent(
    modules = [
        ConnectionsSheetModule::class,
        CoroutineContextModule::class,
        LoggingModule::class
    ]
)
internal interface ConnectionsSheetComponent {
    val viewModel: ConnectionsSheetViewModel

    fun inject(factory: ConnectionsSheetViewModel.Factory)

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun configuration(configuration: ConnectionsSheetContract.Args): Builder

        fun build(): ConnectionsSheetComponent
    }
}
