package com.stripe.android.connections.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.Module

/**
 * App-scoped component. On process kills, [Application.onCreate] will
 * get re-triggered, and graph will be reconstructed.
 */
@Component(modules = [SubcomponentsModule::class])
internal interface ConnectionsAppComponent {

    fun connectionsSheetComponent(): ConnectionsSheetComponent.Builder

    @Component.Builder
    interface Builder {
        // Now Application is provided by AppComponent.
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): ConnectionsAppComponent
    }
}

/**
 * App subcomponents declaration
 * (declares [ConnectionsSheetComponent] as [ConnectionsAppComponent] subcomponent.
 */
@Module(subcomponents = [ConnectionsSheetComponent::class])
class SubcomponentsModule
