package com.stripe.android.connections.di

import android.app.Application
import com.stripe.android.connections.ConnectionsSheetContract
import com.stripe.android.connections.ConnectionsSheetViewModel
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.LoggingModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ConnectionsSheetModule::class,
        CoroutineContextModule::class,
        LoggingModule::class
    ]
)
internal interface ConnectionsSheetComponent {
    val viewModel: ConnectionsSheetViewModel

    fun inject(factory: ConnectionsSheetViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun configuration(configuration: ConnectionsSheetContract.Args): Builder

        fun build(): ConnectionsSheetComponent
    }
}
